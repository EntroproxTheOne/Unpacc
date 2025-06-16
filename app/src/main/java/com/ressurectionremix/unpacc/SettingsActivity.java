package com.ressurectionremix.unpacc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private int clickCount = 0;
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);



        // References
        MaterialSwitch monetSwitch = findViewById(R.id.monetSwitch);
        RadioGroup themeGroup = findViewById(R.id.themeRadioGroup);
        MaterialSwitch wifiOnlySwitch = findViewById(R.id.wifiOnlySwitch);
        MaterialButton changeDirBtn = findViewById(R.id.changeDirBtn);
        TextView versionInfo = findViewById(R.id.versionInfo);

        // Load saved preferences
        SharedPreferences sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean useMonet = sharedPref.getBoolean("use_monet", true);
        monetSwitch.setChecked(useMonet);

        int theme = sharedPref.getInt("theme_mode", 0); // 0: system, 1: light, 2: dark
        switch (theme) {
            case 1:
                ((android.widget.RadioButton) findViewById(R.id.radioLight)).setChecked(true);
                break;
            case 2:
                ((android.widget.RadioButton) findViewById(R.id.radioDark)).setChecked(true);
                break;
            default:
                ((android.widget.RadioButton) findViewById(R.id.radioSystem)).setChecked(true);
                break;
        }

        // Monet toggle
        monetSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("use_monet", isChecked);
            editor.apply();
            Toast.makeText(this, "Monet theme " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        // Theme radio group
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = 0;
            if (checkedId == R.id.radioLight) mode = 1;
            else if (checkedId == R.id.radioDark) mode = 2;

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("theme_mode", mode);
            editor.apply();

            // Apply theme
            switch (mode) {
                case 1:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
                case 2:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
            }

            recreate();
        });

        // WiFi Only Toggle
        wifiOnlySwitch.setChecked(sharedPref.getBoolean("wifi_only", false));
        wifiOnlySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("wifi_only", isChecked);
            editor.apply();
            Toast.makeText(this, "WiFi-only downloads: " + (isChecked ? "On" : "Off"), Toast.LENGTH_SHORT).show();
        });

        // Change directory
        changeDirBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, 42);
        });

        // Version info easter egg
        versionInfo.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 500) {
                clickCount++;
                if (clickCount >= 5) {
                    Toast.makeText(this, "Developer Mode Activated!", Toast.LENGTH_SHORT).show();
                    clickCount = 0;
                }
            } else {
                clickCount = 1;
            }
            lastClickTime = currentTime;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 42 && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            SharedPreferences sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("download_dir", uri.toString());
            editor.apply();

            Toast.makeText(this, "Download directory set", Toast.LENGTH_SHORT).show();
        }
    }
}