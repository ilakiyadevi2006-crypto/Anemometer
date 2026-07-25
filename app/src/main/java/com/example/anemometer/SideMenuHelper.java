package com.example.anemometer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.net.URL;

public class SideMenuHelper {

    public static void setup(Activity activity, DrawerLayout drawerLayout, NavigationView navigationView, int currentMenuItemId) {
        navigationView.setCheckedItem(currentMenuItemId);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menuNotifications) {
                showNotificationDialog(activity);
            } else if (id == R.id.menuServerConfig) {
                boolean connected = false;
                if (activity instanceof ServerConnectionProvider) {
                    connected = ((ServerConnectionProvider) activity).isServerConnected();
                }
                showServerConfigDialog(activity, connected);
            } else if (id == R.id.menuHelp) {
                showHelpDialog(activity);
            } else if (id == R.id.menuTerms) {
                showTermsDialog(activity);
            } else if (id == R.id.menuAppInfo) {
                showAppInfoDialog(activity);
            } else if (id == R.id.menuAbout) {
                showAboutDialog(activity);
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private static void showNotificationDialog(Activity activity) {
        AppPreferences prefs = new AppPreferences(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_notifications, null);
        AlertDialog dialog = createRoundedDialog(activity, view);

        RadioButton rbOn = view.findViewById(R.id.rbNotifyOn);
        RadioButton rbOff = view.findViewById(R.id.rbNotifyOff);

        if (prefs.areNotificationsEnabled()) rbOn.setChecked(true);
        else rbOff.setChecked(true);

        view.findViewById(R.id.btnCancelNotify).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnSaveNotify).setOnClickListener(v -> {
            boolean enabled = rbOn.isChecked();
            prefs.setNotificationsEnabled(enabled);
            
            boolean isServerConnected = false;
            if (activity instanceof ServerConnectionProvider) {
                isServerConnected = ((ServerConnectionProvider) activity).isServerConnected();
            }

            if (enabled) {
                NotificationHelper.updateStatusNotification(activity, isServerConnected);
                Toast.makeText(activity, "Notifications Enabled", Toast.LENGTH_SHORT).show();
            } else {
                NotificationHelper.cancelNotification(activity);
                Toast.makeText(activity, "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private static void showServerConfigDialog(Activity activity, boolean isConnected) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_server_config, null);
        AlertDialog dialog = createRoundedDialog(activity, view);

        TextView tvIp = view.findViewById(R.id.tvDialogIp);
        TextView tvPort = view.findViewById(R.id.tvDialogPort);
        View vDot = view.findViewById(R.id.vDialogStatusDot);
        TextView tvStatus = view.findViewById(R.id.tvDialogStatusText);

        try {
            URL url = new URL("http://127.0.0.1:5000");
            tvIp.setText(url.getHost());
            tvPort.setText(String.valueOf(url.getPort() == -1 ? 80 : url.getPort()));
        } catch (Exception ignored) {}

        if (isConnected) {
            vDot.setBackgroundResource(R.drawable.bg_green_dot);
            tvStatus.setText("Connected");
            tvStatus.setTextColor(Color.parseColor("#10B981"));
        } else {
            vDot.setBackgroundResource(R.drawable.bg_indicator_btn_off);
            tvStatus.setText("Disconnected");
            tvStatus.setTextColor(Color.RED);
        }

        view.findViewById(R.id.btnDialogClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void showAboutDialog(Activity activity) {
        showInfoDialog(activity, activity.getString(R.string.menu_about), 
                activity.getString(R.string.about_content));
    }

    private static void showHelpDialog(Activity activity) {
        showInfoDialog(activity, activity.getString(R.string.menu_help), 
                activity.getString(R.string.help_content));
    }

    private static void showTermsDialog(Activity activity) {
        showInfoDialog(activity, activity.getString(R.string.menu_terms), 
                activity.getString(R.string.terms_content));
    }

    private static void showAppInfoDialog(Activity activity) {
        showInfoDialog(activity, activity.getString(R.string.menu_app_info),
                activity.getText(R.string.app_info_content));
    }

    private static void showInfoDialog(Activity activity, CharSequence title, CharSequence content) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_info, null);
        AlertDialog dialog = createRoundedDialog(activity, view);

        ((TextView) view.findViewById(R.id.tvInfoTitle)).setText(title);
        ((TextView) view.findViewById(R.id.tvInfoContent)).setText(content);
        view.findViewById(R.id.btnInfoClose).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private static AlertDialog createRoundedDialog(Activity activity, View view) {
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            view.setBackgroundResource(R.drawable.bg_dialog);
        }
        return dialog;
    }
}
