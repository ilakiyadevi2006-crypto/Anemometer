package com.example.anemometer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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

public class AnemometerActivity extends AppCompatActivity implements ServerConnectionProvider {

    private ImageView imgNeedle, imgCompassNeedle;
    private ImageView imgSpeedPin, imgSpeedPin1;

    private TextView txtSpeed;
    private TextView txtDirection;
    private TextView txtDirectionDegree;
    private TextView txtMaxSpeed;
    private TextView txtMaxSpeedTime;
    private TextView txtAvgSpeed;
    private TextView txtTemperature;
    private TextView txtHumidity;
    private TextView tvConnectionStatus;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isServerConnected = false;

    private final Handler handler = new Handler();

    private static final String SERVER_URL = "http://127.0.0.1:5000/anemometer";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anemometer);

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
        imgNeedle = findViewById(R.id.imgNeedle);
        imgCompassNeedle = findViewById(R.id.imgCompassNeedle);
        imgSpeedPin = findViewById(R.id.imgSpeedPin);
        imgSpeedPin1 = findViewById(R.id.imgSpeedPin1);

        txtSpeed = findViewById(R.id.txtSpeed);
        txtDirection = findViewById(R.id.txtDirection);
        txtDirectionDegree = findViewById(R.id.txtDirectionDegree);
        txtMaxSpeed = findViewById(R.id.txtMaxSpeed);
        txtMaxSpeedTime = findViewById(R.id.txtMaxSpeedTime);
        txtAvgSpeed = findViewById(R.id.txtAvgSpeed);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtHumidity = findViewById(R.id.txtHumidity);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        imgSpeedPin.post(() -> {
            imgSpeedPin.setPivotX(imgSpeedPin.getWidth() / 2f);
            imgSpeedPin.setPivotY(imgSpeedPin.getHeight() * 0.82f);
        });

        imgSpeedPin1.post(() -> {
            imgSpeedPin1.setPivotX(imgSpeedPin1.getWidth() / 2f);
            imgSpeedPin1.setPivotY(imgSpeedPin1.getHeight() * 0.82f);
        });
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
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                response.append(currentLine);
                            }
                            reader.close();

                            JSONObject json = new JSONObject(response.toString());
                            float speed = (float) json.getDouble("speed");
                            float serverMax = (float) json.getDouble("max_speed");
                            float serverAvg = (float) json.getDouble("avg_speed");
                            int degree = json.getInt("direction_degree");
                            String direction = json.getString("direction");
                            float temperature = (float) json.getDouble("temperature");
                            int humidity = json.getInt("humidity");

                            runOnUiThread(() -> {
                                if (!isServerConnected) {
                                    isServerConnected = true;
                                    NotificationHelper.updateStatusNotification(AnemometerActivity.this, true);
                                }
                                tvConnectionStatus.setText(R.string.status_connected);
                                tvConnectionStatus.setTextColor(Color.parseColor("#10B981"));

                                txtSpeed.setText(String.format(Locale.getDefault(), "%.1f", speed));
                                txtDirection.setText(direction);
                                txtDirectionDegree.setText(degree + "°");
                                txtTemperature.setText(String.format(Locale.getDefault(), "%.1f°C", temperature));
                                txtHumidity.setText(String.format(Locale.getDefault(), "%d%%", humidity));

                                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                                txtMaxSpeedTime.setText(sdf.format(new Date()));

                                updateNeedle(speed);
                                updateMaxSpeed(serverMax);
                                updateAvgSpeed(serverAvg);
                                updateCompassNeedle(degree);
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
            tvConnectionStatus.setText(R.string.status_disconnected);
            tvConnectionStatus.setTextColor(Color.RED);
        });
    }

    private void updateNeedle(float currentSpeed) {
        if (imgNeedle == null) return;
        float speed = Math.max(0, Math.min(currentSpeed, 30));
        float angle = -90 + (speed / 30f) * 180f;
        imgNeedle.setPivotX(imgNeedle.getWidth() / 2f);
        imgNeedle.setPivotY(imgNeedle.getHeight());
        imgNeedle.animate().rotation(angle).setDuration(500).setInterpolator(new LinearInterpolator()).start();
    }

    private void updateCompassNeedle(int degree) {
        if (imgCompassNeedle == null) return;
        imgCompassNeedle.setPivotX(imgCompassNeedle.getWidth() / 2f);
        imgCompassNeedle.setPivotY(imgCompassNeedle.getHeight() / 2f);
        imgCompassNeedle.animate().rotation(degree).setDuration(500).setInterpolator(new LinearInterpolator()).start();
    }

    private void updateMaxSpeed(float targetSpeed) {
        if (imgSpeedPin == null) return;
        float speed = Math.max(0, Math.min(targetSpeed, 30));
        float angle = -90f + ((speed / 30f) * 180f);
        imgSpeedPin.animate().rotation(angle).setDuration(800).setInterpolator(new LinearInterpolator()).start();
        txtMaxSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", speed));
    }

    private void updateAvgSpeed(float targetSpeed) {
        if (imgSpeedPin1 == null) return;
        float speed = Math.max(0, Math.min(targetSpeed, 30));
        float angle = -90f + ((speed / 30f) * 180f);
        imgSpeedPin1.animate().rotation(angle).setDuration(800).setInterpolator(new LinearInterpolator()).start();
        txtAvgSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", speed));
    }

    private void setupNavigation() {
        View navAdcp = findViewById(R.id.navAdcp);
        View navGps = findViewById(R.id.navGps);
        View navStatus = findViewById(R.id.navStatus);
        View navAnemometer = findViewById(R.id.navAnemometer);

        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navAdcp);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navGps);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navStatus);
        com.example.anemometer.BottomNavHelper.applyHoverAnimation(navAnemometer);

        navAdcp.setOnClickListener(v -> {
            startActivity(new Intent(this, AdcpActivity.class));
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
