package com.scrat.imagewallpapersc;

import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ImageWallpaperSCSettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{
    private final int OPEN_DIRECTORY_REQUEST_CODE = 0xf11e;
    private final int OPEN_MULTIPLE_REQUEST_CODE = 0xf11f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        android.app.ActionBar actionbar;
        actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        findPreference("directory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openDirectory();
                return true;
            }
        });
        findPreference("multi").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                selectMulti();
                return true;
            }
        });
        findPreference("apply").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                applyWalls();
                return true;
            }
        });


    }
    private void applyWalls() {
        try {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, ImageWallpaperSCService.class));
            startActivity(intent);
            finish();
        }
        catch (android.content.ActivityNotFoundException e3){
            try {
                Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                startActivity(intent);
                finish();
            } catch (android.content.ActivityNotFoundException e2){
               Toast.makeText(this, getResources().getString(R.string.activity_error), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |  Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
    }

    private void selectMulti () {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |  Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, OPEN_MULTIPLE_REQUEST_CODE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (resultCode == RESULT_OK) {
            SharedPreferences.Editor settings = getSharedPreferences("Settings", MODE_PRIVATE).edit();
            if (requestCode == OPEN_DIRECTORY_REQUEST_CODE) {
                Uri directoryUri = imageReturnedIntent.getData();
                if (directoryUri != null) {
                    getContentResolver().takePersistableUriPermission(
                            directoryUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                    settings.putString("directory", directoryUri.toString());
                    settings.putInt("mode", 2);
                } else settings.putInt("mode", 0);
                settings.apply();
            }
            if (requestCode == OPEN_MULTIPLE_REQUEST_CODE) {
                ArrayList<String> listUri = new ArrayList<>();
                ClipData clipData = imageReturnedIntent.getClipData();
                if (clipData!=null)
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri fileUri = clipData.getItemAt(i).getUri();
                    getContentResolver().takePersistableUriPermission(fileUri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    listUri.add(fileUri.toString());
                }
                else {
                    Uri fileUri = imageReturnedIntent.getData();
                    getContentResolver().takePersistableUriPermission(Objects.requireNonNull(fileUri),Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    listUri.add(Objects.requireNonNull(fileUri).toString());
                }
                Set<String> arraySet = new HashSet<>(listUri);
                settings.putStringSet("multi", arraySet);
                settings.putInt("mode", 1);
                settings.apply();
            }
            sendBroadcast(new Intent("com.scrat.imagewallpapersc.UpdateDBForSaveFolder"));
        }
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        EditTextPreference duration = (EditTextPreference) findPreference("duration");
        if (duration.getText().equals("") || duration.getText().equals("0")) duration.setSummary(getResources().getString(R.string.Infinitely)); else
            duration.setSummary(duration.getText() + " " + getResources().getString(R.string.min));
        String[] Speed_value = getResources().getStringArray(R.array.speed_values);
        ListPreference SpeedSetting = (ListPreference) findPreference("speed");
        if (SpeedSetting.getValue().equals(Speed_value[0])) {
            SpeedSetting.setSummary(getResources().getString(R.string.speed_fast));
        }
        if (SpeedSetting.getValue().equals(Speed_value[1])) {
            SpeedSetting.setSummary(getResources().getString(R.string.speed_normal));
        }
        if (SpeedSetting.getValue().equals(Speed_value[2])) {
            SpeedSetting.setSummary(getResources().getString(R.string.speed_low));
        }
        if (SpeedSetting.getValue().equals(Speed_value[3])) {
            SpeedSetting.setSummary(getResources().getString(R.string.speed_off));
        }

        String[] touch_value = getResources().getStringArray(R.array.touch_values);
        ListPreference TouchSetting = (ListPreference) findPreference("touch");
        if (TouchSetting.getValue().equals(touch_value[2])) {
            TouchSetting.setSummary(getResources().getString(R.string.touch_off));
        }
        if (TouchSetting.getValue().equals(touch_value[1])) {
            TouchSetting.setSummary(getResources().getString(R.string.touch_offset));
        }
        if (TouchSetting.getValue().equals(touch_value[0])) {
            TouchSetting.setSummary(getResources().getString(R.string.touch_parallax));
        }
        ListPreference QualitySetting = (ListPreference) findPreference("quality");
        String[] Quality_value = getResources().getStringArray(R.array.quality_values);
        if (QualitySetting.getValue().equals(Quality_value[0])) {
            QualitySetting.setSummary(getResources().getString(R.string.quality_ultra));
        }
        if (QualitySetting.getValue().equals(Quality_value[1])) {
            QualitySetting.setSummary(getResources().getString(R.string.quality_high));
        }
        if (QualitySetting.getValue().equals(Quality_value[2])) {
            QualitySetting.setSummary(getResources().getString(R.string.quality_normal));
        }
        if (QualitySetting.getValue().equals(Quality_value[3])) {
            QualitySetting.setSummary(getResources().getString(R.string.quality_low));
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            if (key.equals("duration")) {
                if (etp.getText().equals("") || etp.getText().equals("0")) etp.setSummary(getResources().getString(R.string.Infinitely)); else
                etp.setSummary(etp.getText() + " " + getResources().getString(R.string.min));
            }
        }
        if (pref instanceof ListPreference) {
            ListPreference ltp = (ListPreference) pref;
            if (key.equals("speed")) {
                String[] Speed_value = getResources().getStringArray(R.array.speed_values);
                if (ltp.getValue().equals(Speed_value[0])) {
                    ltp.setSummary(getResources().getString(R.string.speed_fast));
                }
                if (ltp.getValue().equals(Speed_value[1])) {
                    ltp.setSummary(getResources().getString(R.string.speed_normal));
                }
                if (ltp.getValue().equals(Speed_value[2])) {
                    ltp.setSummary(getResources().getString(R.string.speed_low));
                }
                if (ltp.getValue().equals(Speed_value[3])) {
                    ltp.setSummary(getResources().getString(R.string.speed_off));
                }
            }
            if (key.equals("touch")) {
                String[] touch_value = getResources().getStringArray(R.array.touch_values);
                if (ltp.getValue().equals(touch_value[2])) {
                    ltp.setSummary(getResources().getString(R.string.touch_off));
                }
                if (ltp.getValue().equals(touch_value[1])) {
                    ltp.setSummary(getResources().getString(R.string.touch_offset));
                }
                if (ltp.getValue().equals(touch_value[0])) {
                    ltp.setSummary(getResources().getString(R.string.touch_parallax));
                }
            }
            if (key.equals("quality")) {
                String[] Quality_value = getResources().getStringArray(R.array.quality_values);
                if (ltp.getValue().equals(Quality_value[0])) {
                    ltp.setSummary(getResources().getString(R.string.quality_ultra));
                }
                if (ltp.getValue().equals(Quality_value[1])) {
                    ltp.setSummary(getResources().getString(R.string.quality_high));
                }
                if (ltp.getValue().equals(Quality_value[2])) {
                    ltp.setSummary(getResources().getString(R.string.quality_normal));
                }
                if (ltp.getValue().equals(Quality_value[3])) {
                    ltp.setSummary(getResources().getString(R.string.quality_low));
                }
            }
        }
    }
}