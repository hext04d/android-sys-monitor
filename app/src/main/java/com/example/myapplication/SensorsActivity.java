package com.example.myapplication;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorsActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private SensorAdapter adapter;
    private Map<Integer, String> sensorValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sensors);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvSensors), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvSensors = findViewById(R.id.rvSensors);
        rvSensors.setLayoutManager(new LinearLayoutManager(this));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        // Filtrar apenas sensores principais
        List<Sensor> sensorList = getMainSensors();

        adapter = new SensorAdapter(sensorList);
        rvSensors.setAdapter(adapter);

        // Registrar sensores filtrados
        for (Sensor sensor : sensorList) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private List<Sensor> getMainSensors() {
        List<Sensor> mainSensors = new ArrayList<>();
        int[] mainTypes = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_HEART_RATE
        };

        for (int type : mainTypes) {
            Sensor s = sensorManager.getDefaultSensor(type);
            if (s != null) {
                mainSensors.add(s);
            }
        }
        return mainSensors;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < event.values.length; i++) {
            values.append(String.format("%.2f", event.values[i]));
            if (i < event.values.length - 1) values.append(" | ");
        }
        sensorValues.put(event.sensor.getType(), values.toString());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    private class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {
        private final List<Sensor> sensors;

        SensorAdapter(List<Sensor> sensors) {
            this.sensors = sensors;
        }

        @NonNull
        @Override
        public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor, parent, false);
            return new SensorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
            Sensor sensor = sensors.get(position);
            holder.tvName.setText(getFriendlyName(sensor));
            holder.tvVendor.setText("Fabricante: " + sensor.getVendor());
            
            String val = sensorValues.get(sensor.getType());
            holder.tvValue.setText(val != null ? "Dados: " + val : "Aguardando dados...");
        }

        @Override
        public int getItemCount() {
            return sensors.size();
        }

        private String getFriendlyName(Sensor s) {
            switch (s.getType()) {
                case Sensor.TYPE_ACCELEROMETER: return "Acelerômetro";
                case Sensor.TYPE_GYROSCOPE: return "Giroscópio";
                case Sensor.TYPE_MAGNETIC_FIELD: return "Bússola (Campo Magnético)";
                case Sensor.TYPE_PROXIMITY: return "Sensor de Proximidade";
                case Sensor.TYPE_LIGHT: return "Sensor de Luz";
                case Sensor.TYPE_PRESSURE: return "Barômetro (Pressão)";
                case Sensor.TYPE_AMBIENT_TEMPERATURE: return "Temperatura Ambiente";
                case Sensor.TYPE_HEART_RATE: return "Frequência Cardíaca";
                default: return s.getName();
            }
        }

        class SensorViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvValue, tvVendor;

            SensorViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvSensorName);
                tvValue = itemView.findViewById(R.id.tvSensorValue);
                tvVendor = itemView.findViewById(R.id.tvSensorVendor);
            }
        }
    }
}
