package com.example.myapplication;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView tvSummaryBattery, tvSummaryRam, tvSummaryStorage;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Views de Resumo
        tvSummaryBattery = findViewById(R.id.tvSummaryBattery);
        tvSummaryRam = findViewById(R.id.tvSummaryRam);
        tvSummaryStorage = findViewById(R.id.tvSummaryStorage);

        // Configurar Botões de Navegação
        MaterialButton btnBattery = findViewById(R.id.btnGoBattery);
        MaterialButton btnRam = findViewById(R.id.btnGoRam);
        MaterialButton btnStorage = findViewById(R.id.btnGoStorage);
        MaterialButton btnSensors = findViewById(R.id.btnGoSensors);

        btnBattery.setOnClickListener(v -> startActivity(new Intent(this, BatteryActivity.class)));
        btnRam.setOnClickListener(v -> startActivity(new Intent(this, RamActivity.class)));
        btnStorage.setOnClickListener(v -> startActivity(new Intent(this, StorageActivity.class)));
        btnSensors.setOnClickListener(v -> startActivity(new Intent(this, SensorsActivity.class)));

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateSummary();
                handler.postDelayed(this, 3000);
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

    private void updateSummary() {
        // Resumo Bateria
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct = (int) ((level / (float) scale) * 100);
            
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL;
            
            String chargeStr = isCharging ? "Carregando" : "Descarregando";
            tvSummaryBattery.setText("Bateria: " + pct + "% (" + chargeStr + ")");
        }

        // Resumo RAM
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(mi);
            long total = mi.totalMem / 1048576L;
            long avail = mi.availMem / 1048576L;
            int usage = (int) (((total - avail) * 100) / total);
            tvSummaryRam.setText("RAM: " + usage + "% em uso");
        }

        // Resumo Espaço
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long availableGigs = (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / (1024 * 1024 * 1024);
        tvSummaryStorage.setText("Espaço: " + availableGigs + " GB livres");
    }
}
