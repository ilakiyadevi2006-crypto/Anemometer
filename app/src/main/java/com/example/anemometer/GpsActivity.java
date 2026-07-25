package com.example.anemometer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GpsActivity extends AppCompatActivity implements ServerConnectionProvider {

    private TextView tvGpsStatus, tvLoadPinStatus;
    private TextView txtLatitude, txtLongitude, txtAccuracy, txtGpsSpeed, txtTime, txtLoadPinValue;
    private ImageView gaugeNeedle;
    private final Handler handler = new Handler();

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isServerConnected = false;

    private static final String SERVER_URL = "http://127.0.0.1:5000/gps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

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
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvLoadPinStatus = findViewById(R.id.tvLoadPinStatus);
        txtLatitude = findViewById(R.id.txtLatitude);
        txtLongitude = findViewById(R.id.txtLongitude);
        txtAccuracy = findViewById(R.id.txtAccuracy);
        txtGpsSpeed = findViewById(R.id.txtGpsSpeed);
        txtTime = findViewById(R.id.txtTime);
        txtLoadPinValue = findViewById(R.id.txtLoadPinValue);
        gaugeNeedle = findViewById(R.id.gaugeNeedle);
    }

    private void setupSideMenu() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        findViewById(R.id.btnMenu).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        SideMenuHelper.setup(this, drawerLayout, navigationView, -1);
    }

    private void checkNotificationState() {
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.areNotificationsEnabled()) {
            NotificationHelper.showStatusNotification(this);
        }
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
                            isServerConnected = true;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();

                            JSONObject json = new JSONObject(response.toString());
                            String latitude = json.getString("latitude");
                            String longitude = json.getString("longitude");
                            String accuracy = json.getString("accuracy");
                            String speed = json.getString("speed");
                            float loadPinValue = (float) json.getDouble("load_pin_value");

                            runOnUiThread(() -> {
                                tvGpsStatus.setText(R.string.status_connected);
                                tvGpsStatus.setTextColor(Color.parseColor("#10B981"));
                                tvLoadPinStatus.setText(R.string.status_connected);
                                tvLoadPinStatus.setTextColor(Color.parseColor("#10B981"));
                                
                                txtLatitude.setText(latitude);
                                txtLongitude.setText(longitude);
                                txtAccuracy.setText(accuracy);
                                txtGpsSpeed.setText(speed);

                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                                txtTime.setText(sdf.format(new Date()));
                                
                                txtLoadPinValue.setText(String.format(Locale.getDefault(), "%.2f mA", loadPinValue));
                                updateGaugeNeedle(loadPinValue);
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

    private void updateGaugeNeedle(float valueInMa) {
        if (gaugeNeedle == null) return;
        float clampedMa = Math.max(4, Math.min(valueInMa, 20));
        float angle = -90f + ((clampedMa - 4f) / (20f - 4f)) * 180f;
        gaugeNeedle.post(() -> {
            gaugeNeedle.setPivotX(gaugeNeedle.getWidth() / 2f);
            gaugeNeedle.setPivotY(gaugeNeedle.getHeight());
            gaugeNeedle.animate().rotation(angle).setDuration(500).setInterpolator(new LinearInterpolator()).start();
        });
    }

    private void handleDisconnection() {
        isServerConnected = false;
        runOnUiThread(() -> {
            tvGpsStatus.setText(R.string.status_disconnected);
            tvGpsStatus.setTextColor(Color.RED);
            tvLoadPinStatus.setText(R.string.status_disconnected);
            tvLoadPinStatus.setTextColor(Color.RED);

            if (gaugeNeedle != null) {
                gaugeNeedle.animate().rotation(0).setDuration(500).setInterpolator(new LinearInterpolator()).start();
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.navAnemometer).setOnClickListener(v -> {
            startActivity(new Intent(this, AnemometerActivity.class));
            finish();
        });
        findViewById(R.id.navAdcp).setOnClickListener(v -> {
            startActivity(new Intent(this, AdcpActivity.class));
            finish();
        });
        findViewById(R.id.navStatus).setOnClickListener(v -> {
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
