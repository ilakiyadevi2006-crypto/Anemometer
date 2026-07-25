package com.example.anemometer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdcpActivity extends AppCompatActivity implements ServerConnectionProvider {

    private TextView tvAdcpStatus;
    private TextView txtWaveSpeed, txtWaveDirection, txtPeriod, txtSignificantHeight, txtTideHeight;
    private final Handler handler = new Handler();

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isServerConnected = false;

    private static final String SERVER_URL = "http://127.0.0.1:5000/adcp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adcp);

        initViews();
        setupNavigation();
        setupSideMenu();
        startFetchingData();
        checkNotificationState();
    }

    @Override
    public boolean isServerConnected() {
        return isServerConnected;
    }

    private void initViews() {
        tvAdcpStatus = findViewById(R.id.tvAdcpStatus);
        txtWaveSpeed = findViewById(R.id.txtWaveSpeed);
        txtWaveDirection = findViewById(R.id.txtWaveDirection);
        txtPeriod = findViewById(R.id.txtPeriod);
        txtSignificantHeight = findViewById(R.id.txtSignificantHeight);
        txtTideHeight = findViewById(R.id.txtTideHeight);
    }

    private void setupSideMenu() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        SideMenuHelper.setup(this, drawerLayout, navigationView, -1);
    }

    private void checkNotificationState() {
        NotificationHelper.updateStatusNotification(this, isServerConnected);
    }

    private void startFetchingData() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        URL url = new URL(SERVER_URL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(1000);
                        connection.setReadTimeout(1000);

                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();

                            JSONObject json = new JSONObject(response.toString());
                            String waveSpeed = json.getString("wave_speed");
                            String waveDirection = json.getString("wave_direction");
                            String period = json.getString("period");
                            String significantHeight = json.getString("significant_height");
                            String tideHeight = json.getString("tide_height");

                            runOnUiThread(() -> {
                                if (!isServerConnected) {
                                    isServerConnected = true;
                                    NotificationHelper.updateStatusNotification(AdcpActivity.this, true);
                                }
                                tvAdcpStatus.setText(R.string.status_connected);
                                tvAdcpStatus.setTextColor(Color.parseColor("#10B981"));
                                
                                txtWaveSpeed.setText(waveSpeed);
                                txtWaveDirection.setText(waveDirection);
                                txtPeriod.setText(period);
                                txtSignificantHeight.setText(significantHeight);
                                txtTideHeight.setText(tideHeight);
                            });
                        } else {
                            handleDisconnection();
                        }
                    } catch (Exception e) {
                        handleDisconnection();
                    }
                }).start();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void handleDisconnection() {
        if (isServerConnected) {
            isServerConnected = false;
            NotificationHelper.updateStatusNotification(this, false);
        }
        runOnUiThread(() -> {
            tvAdcpStatus.setText(R.string.status_disconnected);
            tvAdcpStatus.setTextColor(Color.RED);
        });
    }

    private void setupNavigation() {
        View navAnemometer = findViewById(R.id.navAnemometer);
        View navGps = findViewById(R.id.navGps);
        View navStatus = findViewById(R.id.navStatus);
        View navAdcp = findViewById(R.id.navAdcp);

        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navAnemometer);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navGps);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navStatus);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navAdcp);

        navAnemometer.setOnClickListener(v -> {
            startActivity(new Intent(this, AnemometerActivity.class));
            finish();
        });
        navGps.setOnClickListener(v -> {
            startActivity(new Intent(this, GpsActivity.class));
            finish();
        });
        navStatus.setOnClickListener(v -> {
            startActivity(new Intent(this, StatusActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
