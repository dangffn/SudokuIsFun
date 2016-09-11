package com.danbuntu.androidsudoku;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Dan on 5/16/2016.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        getFragmentManager().beginTransaction().replace(R.id.preferences_frame, new MyPreferenceFragment()).commit();

        android.support.v7.app.ActionBar actionbar = getSupportActionBar();
        if(actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            getPreferenceManager().findPreference(getString(R.string.pref_train_ocr)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), OCRActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }

        @Override
        public void onResume() {

            final File ocrFile = new File("/sdcard/" + getString(R.string.ocr_train_file));
            final Preference deleteOcr = getPreferenceManager().findPreference("pref_clear_ocr");

            if(deleteOcr != null) {

                if(ocrFile.exists()) {
                    int count = getPreferenceManager().getSharedPreferences().getInt(getString(R.string.pref_external_sig_count), 0);
                    if(count > 0)
                        if(count == 1) {
                            deleteOcr.setSummary("Tap to clear " + count + " saved digit");
                        } else {
                            deleteOcr.setSummary("Tap to clear " + count + " saved digits");
                        }
                }

                deleteOcr.setEnabled(ocrFile.exists());
                deleteOcr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        // delete the training file
                        if (ocrFile.exists()) {
                            if (ocrFile.delete()) {
                                deleteOcr.setEnabled(false);
                                Log.i("SettingsActivity", "Successfully deleted ocr training data");
                                Toast.makeText(getActivity(), "Deleted OCR training data", Toast.LENGTH_LONG).show();
                                deleteOcr.setSummary("There is no data");
                                getPreferenceManager().getSharedPreferences().edit().putInt(getString(R.string.pref_external_sig_count), 0).apply();
                            } else {
                                Log.e("SettingsActivity", "Failed to delete ocr training data");
                                Toast.makeText(getActivity(), "Failed to delete OCR training data", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            deleteOcr.setEnabled(false);
                        }

                        return true;

                    }
                });
            }

            super.onResume();
        }
    }
}