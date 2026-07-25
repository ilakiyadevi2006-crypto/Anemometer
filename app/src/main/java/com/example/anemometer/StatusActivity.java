package com.example.anemometer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class StatusActivity extends AppCompatActivity implements ServerConnectionProvider {

    private TextView tvBeaconStatus, tvFogStatus, tvLanternStatus;
    private LinearLayout btnBeaconOn, btnBeaconOff, btnFogOn, btnFogOff, btnLanternOn, btnLanternOff;
    private ImageView ivBeaconOn, ivBeaconOff, ivFogOn, ivFogOff, ivLanternOn, ivLanternOff;
    private TextView tvBeaconOn, tvBeaconOff, tvFogOn, tvFogOff, tvLanternOn, tvLanternOff;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private boolean isServerConnected = false;

    private final Handler handler = new Handler();
    private static final String SERVER_URL = "http://127.0.0.1:5000/status";
    private static final String UPDATE_URL = "http://127.0.0.1:5000/update_status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        initViews();
        setupNavigation();
        setupSideMenu();
        startFetchingData();
        checkNotificationState();

        updateBeaconUI(false);
        updateFogUI(false);
        updateLanternUI(false);
    }

    @Override
    public boolean isServerConnected() {
        return isServerConnected;
    }

    private void initViews() {
        tvBeaconStatus = findViewById(R.id.tvBeaconStatus);
        tvFogStatus = findViewById(R.id.tvFogStatus);
        tvLanternStatus = findViewById(R.id.tvLanternStatus);
        btnBeaconOn = findViewById(R.id.btnBeaconOn);
        btnBeaconOff = findViewById(R.id.btnBeaconOff);
        btnFogOn = findViewById(R.id.btnFogOn);
        btnFogOff = findViewById(R.id.btnFogOff);
        btnLanternOn = findViewById(R.id.btnLanternOn);
        btnLanternOff = findViewById(R.id.btnLanternOff);
        ivBeaconOn = findViewById(R.id.ivBeaconOn);
        ivBeaconOff = findViewById(R.id.ivBeaconOff);
        ivFogOn = findViewById(R.id.ivFogOn);
        ivFogOff = findViewById(R.id.ivFogOff);
        ivLanternOn = findViewById(R.id.ivLanternOn);
        ivLanternOff = findViewById(R.id.ivLanternOff);
        tvBeaconOn = findViewById(R.id.tvBeaconOn);
        tvBeaconOff = findViewById(R.id.tvBeaconOff);
        tvFogOn = findViewById(R.id.tvFogOn);
        tvFogOff = findViewById(R.id.tvFogOff);
        tvLanternOn = findViewById(R.id.tvLanternOn);
        tvLanternOff = findViewById(R.id.tvLanternOff);

        btnBeaconOn.setOnClickListener(v -> sendUpdateToServer("beacon", "on"));
        btnBeaconOff.setOnClickListener(v -> sendUpdateToServer("beacon", "off"));
        btnFogOn.setOnClickListener(v -> sendUpdateToServer("fog", "on"));
        btnFogOff.setOnClickListener(v -> sendUpdateToServer("fog", "off"));
        btnLanternOn.setOnClickListener(v -> sendUpdateToServer("lantern", "on"));
        btnLanternOff.setOnClickListener(v -> sendUpdateToServer("lantern", "off"));
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

                        if (connection.getResponseCode() == 200) {
                            isServerConnected = true;
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();

                            JSONObject json = new JSONObject(response.toString());
                            boolean beacon = json.getString("beacon").equalsIgnoreCase("on");
                            boolean fog = json.getString("fog").equalsIgnoreCase("on");
                            boolean lantern = json.getString("lantern").equalsIgnoreCase("on");

                            runOnUiThread(() -> {
                                updateBeaconUI(beacon);
                                updateFogUI(fog);
                                updateLanternUI(lantern);
                            });
                        } else {
                            isServerConnected = false;
                        }
                    } catch (Exception e) {
                        isServerConnected = false;
                    }
                }).start();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void sendUpdateToServer(String device, String state) {
        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_URL + "?device=" + device + "&state=" + state);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.getResponseCode();
            } catch (Exception e) {
                Log.e("StatusActivity", "Error updating status", e);
            }
        }).start();
        
        if (device.equals("beacon")) updateBeaconUI(state.equals("on"));
        else if (device.equals("fog")) updateFogUI(state.equals("on"));
        else if (device.equals("lantern")) updateLanternUI(state.equals("on"));
    }

    private void updateBeaconUI(boolean isOn) {
        if (isOn) {
            tvBeaconStatus.setText(R.string.status_on);
            tvBeaconStatus.setTextColor(Color.parseColor("#10B981"));
            btnBeaconOn.setBackgroundResource(R.drawable.bg_indicator_btn_on);
            ivBeaconOn.setColorFilter(Color.WHITE);
            tvBeaconOn.setTextColor(Color.WHITE);
            btnBeaconOff.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivBeaconOff.setColorFilter(Color.parseColor("#94A3B8"));
            tvBeaconOff.setTextColor(Color.parseColor("#64748B"));
        } else {
            tvBeaconStatus.setText(R.string.status_off);
            tvBeaconStatus.setTextColor(Color.parseColor("#EF4444"));
            btnBeaconOn.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivBeaconOn.setColorFilter(Color.parseColor("#94A3B8"));
            tvBeaconOn.setTextColor(Color.parseColor("#64748B"));
            btnBeaconOff.setBackgroundResource(R.drawable.bg_indicator_btn_off);
            ivBeaconOff.setColorFilter(Color.WHITE);
            tvBeaconOff.setTextColor(Color.WHITE);
        }
    }

    private void updateFogUI(boolean isOn) {
        if (isOn) {
            tvFogStatus.setText(R.string.status_on);
            tvFogStatus.setTextColor(Color.parseColor("#10B981"));
            btnFogOn.setBackgroundResource(R.drawable.bg_indicator_btn_on);
            ivFogOn.setColorFilter(Color.WHITE);
            tvFogOn.setTextColor(Color.WHITE);
            btnFogOff.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivFogOff.setColorFilter(Color.parseColor("#94A3B8"));
            tvFogOff.setTextColor(Color.parseColor("#64748B"));
        } else {
            tvFogStatus.setText(R.string.status_off);
            tvFogStatus.setTextColor(Color.parseColor("#EF4444"));
            btnFogOn.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivFogOn.setColorFilter(Color.parseColor("#94A3B8"));
            tvFogOn.setTextColor(Color.parseColor("#64748B"));
            btnFogOff.setBackgroundResource(R.drawable.bg_indicator_btn_off);
            ivFogOff.setColorFilter(Color.WHITE);
            tvFogOff.setTextColor(Color.WHITE);
        }
    }

    private void updateLanternUI(boolean isOn) {
        if (isOn) {
            tvLanternStatus.setText(R.string.status_on);
            tvLanternStatus.setTextColor(Color.parseColor("#10B981"));
            btnLanternOn.setBackgroundResource(R.drawable.bg_indicator_btn_on);
            ivLanternOn.setColorFilter(Color.WHITE);
            tvLanternOn.setTextColor(Color.WHITE);
            btnLanternOff.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivLanternOff.setColorFilter(Color.parseColor("#94A3B8"));
            tvLanternOff.setTextColor(Color.parseColor("#64748B"));
        } else {
            tvLanternStatus.setText(R.string.status_off);
            tvLanternStatus.setTextColor(Color.parseColor("#EF4444"));
            btnLanternOn.setBackgroundResource(R.drawable.bg_indicator_btn_inactive);
            ivLanternOn.setColorFilter(Color.parseColor("#94A3B8"));
            tvLanternOn.setTextColor(Color.parseColor("#64748B"));
            btnLanternOff.setBackgroundResource(R.drawable.bg_indicator_btn_off);
            ivLanternOff.setColorFilter(Color.WHITE);
            tvLanternOff.setTextColor(Color.WHITE);
        }
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
        findViewById(R.id.navGps).setOnClickListener(v -> {
            startActivity(new Intent(this, GpsActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
