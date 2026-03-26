package com.example.myapplication;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class RamActivity extends AppCompatActivity {

    private TextView tvRam;
    private CircularProgressIndicator progressRam;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ram);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvRam = findViewById(R.id.tvRam);
        progressRam = findViewById(R.id.progressRam);
        MaterialButton btnCleanRam = findViewById(R.id.btnCleanRam);

        btnCleanRam.setOnClickListener(v -> cleanRam());

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateRamInfo();
                handler.postDelayed(this, 2000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateRamInfo() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(mi);
            long availableMegs = mi.availMem / 1048576L;
            long totalMegs = mi.totalMem / 1048576L;
            long usedMegs = totalMegs - availableMegs;
            int ramPercent = (int) ((usedMegs * 100) / totalMegs);

            tvRam.setText(getString(R.string.ram_status, availableMegs, totalMegs));
            progressRam.setProgress(ramPercent);
        }
    }

    private void cleanRam() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            // Solicita ao sistema que tente limpar processos em segundo plano
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
            if (runningProcesses != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                    // Não mata o próprio processo do app
                    if (!processInfo.processName.equals(getPackageName())) {
                        activityManager.killBackgroundProcesses(processInfo.processName);
                    }
                }
            }
            
            // Força a coleta de lixo da JVM
            System.gc();
            
            Toast.makeText(this, R.string.ram_cleaned, Toast.LENGTH_SHORT).show();
            updateRamInfo();
        }
    }
}
