package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageActivity extends AppCompatActivity {

    private TextView tvStorage, tvStorageImages, tvStorageVideos, tvStorageMusic, tvStorageDocs, tvStorageApks, tvStorageApps;
    private CircularProgressIndicator progressStorage;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private long sizeImages = 0, sizeVideos = 0, sizeMusic = 0, sizeDocs = 0, sizeApks = 0;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                checkFullStoragePermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_storage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvStorage = findViewById(R.id.tvStorage);
        tvStorageImages = findViewById(R.id.tvStorageImages);
        tvStorageVideos = findViewById(R.id.tvStorageVideos);
        tvStorageMusic = findViewById(R.id.tvStorageMusic);
        tvStorageDocs = findViewById(R.id.tvStorageDocs);
        tvStorageApks = findViewById(R.id.tvStorageApks);
        tvStorageApps = findViewById(R.id.tvStorageApps);
        progressStorage = findViewById(R.id.progressStorage);
        
        MaterialButton btnClearCache = findViewById(R.id.btnClearCache);
        btnClearCache.setOnClickListener(v -> clearAppCache());

        updateStorageInfo();
        checkAndRequestPermissions();
    }

    private void clearAppCache() {
        try {
            File dir = getCacheDir();
            if (deleteDir(dir)) {
                Toast.makeText(this, R.string.cache_cleaned, Toast.LENGTH_SHORT).show();
                updateStorageInfo();
                startCalculation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        boolean needRequest = false;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            requestPermissionLauncher.launch(permissions.toArray(new String[0]));
        } else {
            checkFullStoragePermission();
        }
    }

    private void checkFullStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                    startActivity(intent);
                    Toast.makeText(this, "Por favor, autorize o acesso a todos os arquivos para uma varredura completa.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            } else {
                startCalculation();
            }
        } else {
            startCalculation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startCalculation();
            }
        }
    }

    private void updateStorageInfo() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long totalBlocks = stat.getBlockCountLong();

        long availableGigs = (availableBlocks * blockSize) / (1024 * 1024 * 1024);
        long totalGigs = (totalBlocks * blockSize) / (1024 * 1024 * 1024);
        long usedGigs = totalGigs - availableGigs;
        int storagePercent = (int) ((usedGigs * 100) / totalGigs);

        tvStorage.setText(getString(R.string.storage_status, availableGigs, totalGigs));
        progressStorage.setProgress(storagePercent);
    }

    private void startCalculation() {
        sizeImages = 0; sizeVideos = 0; sizeMusic = 0; sizeDocs = 0; sizeApks = 0;
        executor.execute(() -> {
            File root = Environment.getExternalStorageDirectory();
            scanDirectory(root);

            File dataPath = Environment.getDataDirectory();
            StatFs stat = new StatFs(dataPath.getPath());
            long totalUsedBytes = (stat.getBlockCountLong() - stat.getAvailableBlocksLong()) * stat.getBlockSizeLong();
            long otherSize = totalUsedBytes - (sizeImages + sizeVideos + sizeMusic + sizeDocs + sizeApks);

            handler.post(() -> {
                tvStorageImages.setText(formatSize(sizeImages));
                tvStorageVideos.setText(formatSize(sizeVideos));
                tvStorageMusic.setText(formatSize(sizeMusic));
                tvStorageDocs.setText(formatSize(sizeDocs));
                tvStorageApks.setText(formatSize(sizeApks));
                tvStorageApps.setText(formatSize(otherSize));
            });
        });
    }

    private void scanDirectory(File directory) {
        if (directory == null || !directory.exists()) return;
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().equalsIgnoreCase("Android")) {
                    scanDirectory(file);
                }
            } else {
                categorizeFile(file);
            }
        }
    }

    private void categorizeFile(File file) {
        String name = file.getName().toLowerCase();
        long length = file.length();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".webp")) sizeImages += length;
        else if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov")) sizeVideos += length;
        else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".m4a")) sizeMusic += length;
        else if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".txt")) sizeDocs += length;
        else if (name.endsWith(".apk")) sizeApks += length;
    }

    private String formatSize(long bytes) {
        if (bytes < 0) bytes = 0;
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb > 1) return String.format("%.2f GB", gb);
        if (mb > 1) return String.format("%.2f MB", mb);
        return String.format("%.2f KB", kb);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
