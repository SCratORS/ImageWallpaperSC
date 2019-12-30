package com.scrat.imagewallpapersc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.MediaStore;

@SuppressLint("ExportedPreferenceActivity")
public class ImageWallpaperSCSettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if  (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        addPreferencesFromResource(R.xml.settings);
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);



        findPreference("directory").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if  (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("*/*");
                    startActivityForResult(photoPickerIntent, 1);

                    return true;
                } else return false;
            }
        });
    }
    public String getRealPathFromURI (Uri contentUri) {
        String path = null;
        String[] proj = { MediaStore.MediaColumns.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        assert cursor != null;
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            path = cursor.getString(column_index);
        }
        cursor.close();
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String[] separated = getRealPathFromURI(imageReturnedIntent.getData()).split("/");
            StringBuilder folder = new StringBuilder();
            if (separated.length > 0) {
                for (int id = 0; id < separated.length - 1; id++) folder.append(separated[id]).append("/");
            }
//            ImageWallpaperSCRender.Path = folder.toString();
            EditTextPreference directory = (EditTextPreference) findPreference("directory");
            directory.getEditText().setText(folder.toString());
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
        EditTextPreference dir_sel = (EditTextPreference) findPreference("directory");
        dir_sel.setSummary(dir_sel.getText());
//        ImageWallpaperSCRender.Path = dir_sel.getText();

//        SwitchPreference chk_blur = (SwitchPreference) findPreference("blur");
//        ImageWallpaperSCRender.blur = chk_blur.isChecked();

        SwitchPreference chk_snd = (SwitchPreference) findPreference("sound");
 //       ImageWallpaperSCRender.VolumeEnable = chk_snd.isChecked();

        EditTextPreference duration = (EditTextPreference) findPreference("duration");
        duration.setSummary(duration.getText() +" "+ getResources().getString(R.string.min));
//        ImageWallpaperSCRender.Timer = Integer.parseInt(duration.getText());


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

        ListPreference BlurSetting = (ListPreference) findPreference("blur_level");
        String[] Blur_value = getResources().getStringArray(R.array.blur_values);
        if (BlurSetting.getValue().equals(Blur_value[0])) {
            BlurSetting.setSummary(getResources().getString(R.string.blur_low));
        }
        if (BlurSetting.getValue().equals(Blur_value[1])) {
            BlurSetting.setSummary(getResources().getString(R.string.blur_normal));
        }
        if (BlurSetting.getValue().equals(Blur_value[2])) {
            BlurSetting.setSummary(getResources().getString(R.string.blur_high));
        }
        if (BlurSetting.getValue().equals(Blur_value[3])) {
            BlurSetting.setSummary(getResources().getString(R.string.blur_ultra));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref instanceof EditTextPreference) {
            EditTextPreference etp = (EditTextPreference) pref;
            if (key.equals("directory")) {
                etp.setSummary(etp.getText());
//                ImageWallpaperSCRender.Path = etp.getText();
            }
            if (key.equals("duration")) {
                etp.setSummary(etp.getText() + " " + getResources().getString(R.string.min));
//                ImageWallpaperSCRender.Timer = Integer.parseInt(etp.getText());
//                ImageWallpaperSCRender.TimeSet = ImageWallpaperSCRender.Timer * 60000;
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
  //              ImageWallpaperSCRender.SetSpeed = Float.parseFloat(ltp.getValue());
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
//                ImageWallpaperSCRender.touch = Integer.parseInt(ltp.getValue());

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
//                ImageWallpaperSCRender.Quality = Integer.parseInt(ltp.getValue());
            }

            if (key.equals("blur_level")) {
                String[] Blur_value = getResources().getStringArray(R.array.blur_values);
                if (ltp.getValue().equals(Blur_value[0])) {
                    ltp.setSummary(getResources().getString(R.string.blur_low));
                }
                if (ltp.getValue().equals(Blur_value[1])) {
                    ltp.setSummary(getResources().getString(R.string.blur_normal));
                }
                if (ltp.getValue().equals(Blur_value[2])) {
                    ltp.setSummary(getResources().getString(R.string.blur_high));
                }
                if (ltp.getValue().equals(Blur_value[3])) {
                    ltp.setSummary(getResources().getString(R.string.blur_ultra));
                }
//                ImageWallpaperSCRender.LevelGausse = Integer.parseInt(ltp.getValue());
            }
        }
/*
        if (pref instanceof SwitchPreference) {
            if (key.equals("blur")) {
                ImageWallpaperSCRender.blur = (sharedPreferences.getBoolean("blur", false));
            }
            if (key.equals("sound")) {
                ImageWallpaperSCRender.VolumeEnable = (sharedPreferences.getBoolean("sound", false));
            }
        }

 */
    }
}