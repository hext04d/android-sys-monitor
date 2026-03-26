package com.example.myapplication;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class BatteryActivity extends AppCompatActivity {

    private TextView tvBattery, tvBatteryStatus, tvBatteryTemp;
    private CircularProgressIndicator progressBattery;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_battery);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvBattery = findViewById(R.id.tvBattery);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvBatteryTemp = findViewById(R.id.tvBatteryTemp);
        progressBattery = findViewById(R.id.progressBattery);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateBatteryInfo();
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

    private void updateBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            
            tvBattery.setText(getString(R.string.battery_status, batteryPct));
            progressBattery.setProgress(batteryPct);

            float temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
            tvBatteryTemp.setText(getString(R.string.battery_temp, temp));

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            String statusString = "Desconhecido";
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: statusString = getString(R.string.status_charging); break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING: statusString = getString(R.string.status_discharging); break;
                case BatteryManager.BATTERY_STATUS_FULL: statusString = getString(R.string.status_full); break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING: statusString = getString(R.string.status_not_charging); break;
            }
            tvBatteryStatus.setText(getString(R.string.battery_charging, statusString));
        }
    }
}
