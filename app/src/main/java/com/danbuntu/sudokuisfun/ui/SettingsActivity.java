package com.danbuntu.sudokuisfun.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.danbuntu.sudokuisfun.R;
import com.danbuntu.sudokuisfun.ocr.DataManager;
import com.danbuntu.sudokuisfun.utils.SudokuUtils;
import com.danbuntu.sudokuisfun.utils.ThisBackupAgent;

import java.io.File;

/**
 * Created by Dan on 5/16/2016. Have a great day!
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        getFragmentManager().beginTransaction().replace(R.id.preferences_frame, new MyPreferenceFragment()).commit();

        android.support.v7.app.ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            getPreferenceManager().findPreference(getString(R.string.pref_key_trainOcr)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), OCRActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

            getPreferenceManager().findPreference(getString(R.string.pref_key_privacy_policy)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Uri webUri = Uri.parse(getString(R.string.privacy_policy_url));
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                    startActivity(webIntent);
                    return true;
                }
            });

            if (!SudokuUtils.checkWritePermissions(getActivity()))
                SudokuUtils.requestWritePermission(getActivity());
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode == SudokuUtils.WRITE_STORAGE_REQUEST) {
                Preference trainOcr = getPreferenceScreen().findPreference(getString(R.string.pref_key_trainOcr));
                Preference deleteOcr = getPreferenceManager().findPreference(getString(R.string.pref_key_deleteOcr));
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // write permission denied by user
                    if (deleteOcr != null) {
                        setPreferenceEnabled(false, deleteOcr, "Can't access storage, permissions denied");
                    }
                    if (trainOcr != null) trainOcr.setEnabled(false);

                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {

            final File ocrFile = DataManager.getExternalDataFile(getActivity());
            final PreferenceManager prefManager = getPreferenceManager();
            final Preference deleteOcr = prefManager.findPreference(getString(R.string.pref_key_deleteOcr));
            final Preference restorePuzzles = prefManager.findPreference(getString(R.string.pref_key_restorePuzzles));
            final Preference wipeStatsPreference = prefManager.findPreference(getString(R.string.pref_key_wipeStats));
            final ListPreference unitPreference = (ListPreference) prefManager.findPreference(getString(R.string.pref_key_characterUnit));
            final ListPreference keyBoardSizePreference = (ListPreference) prefManager.findPreference(getString(R.string.pref_key_keyboardSize));
            CheckBoxPreference hideAssistance = (CheckBoxPreference) prefManager.findPreference(getString(R.string.pref_key_hideAssistanceOptions));

            hideAssistance.setSummary(String.format(getString(R.string.pref_hideAssistance_Summary),
                    getString(R.string.menu_solvePuzzle), getString(R.string.menu_findPossibilities)));

            // let us know when a list preference changes so we can update the summary
            prefManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            updateListPreferenceSummary(unitPreference, getString(R.string.pref_characterUnit_Summary));
            updateListPreferenceSummary(keyBoardSizePreference, getString(R.string.pref_keyboardSize_summary));

            if (deleteOcr != null) {

                String summary = "Tap to delete the digit recognition training file";
                if (ocrFile.exists()) {
                    int count = prefManager.getSharedPreferences().getInt(getString(R.string.pref_key_externalSigCount), 0);
                    if (count > 0)
                        if (count == 1) {
                            summary = "Tap to clear " + count + " saved digit";
                        } else {
                            summary = "Tap to clear " + count + " saved digits";
                        }
                    setPreferenceEnabled(true, deleteOcr, summary);
                } else {
                    summary = "There is no data";
                    setPreferenceEnabled(false, deleteOcr, summary);
                }

                deleteOcr.setEnabled(ocrFile.exists());
                deleteOcr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        // delete the training file
                        if (ocrFile.exists()) {

                            new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.message_areYouSure)).setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    boolean deleted;
                                    synchronized(ThisBackupAgent.sSyncLock) {
                                         deleted = ocrFile.delete();
                                    }
                                    if (deleted) {
                                        Log.i("SettingsActivity", "Successfully deleted ocr training data");
                                        Toast.makeText(getActivity(), "Deleted ocr training data", Toast.LENGTH_LONG).show();
                                        setPreferenceEnabled(false, deleteOcr, "There is no saved data");
                                        prefManager.getSharedPreferences().edit().putInt(getString(R.string.pref_key_externalSigCount), 0).apply();
                                    } else {
                                        Log.e("SettingsActivity", "Failed to delete ocr training data");
                                        Toast.makeText(getActivity(), getString(R.string.pref_message_delete_ocr_failure), Toast.LENGTH_LONG).show();
                                    }
                                    dialog.dismiss();
                                }
                            }).setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).setMessage(getString(R.string.pref_message_delete_ocr_areYouSure)).show();

                        } else {
                            setPreferenceEnabled(false, deleteOcr, "There is no saved data");
                        }

                        return true;

                    }
                });
            }

            restorePuzzles.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    PuzzleManager pm = PuzzleManager.getInstance(getActivity());
                    if(pm.restoreDefaultPuzzles()) {
                        Toast.makeText(getActivity(), "Default puzzles restored", Toast.LENGTH_LONG).show();
                        restorePuzzles.setEnabled(false);
                    } else {
                        Toast.makeText(getActivity(), "Failed to restore the default puzzles", Toast.LENGTH_LONG).show();
                    }
                    pm.close();
                    return true;
                }
            });

            wipeStatsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.message_areYouSure))
                            .setMessage("Are you sure you want to delete all statistics?")
                            .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PuzzleManager pm = PuzzleManager.getInstance(getActivity());
                                    pm.wipeStatistics();
                                    Toast.makeText(getActivity(), "Cleared statistics", Toast.LENGTH_LONG).show();
                                    wipeStatsPreference.setEnabled(false);
                                    pm.close();
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .create().show();

                    return true;
                }
            });

            super.onResume();
        }

        private void setPreferenceEnabled(boolean enabled, Preference preference, String message) {
            preference.setEnabled(enabled);
            preference.setSummary(message);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

            Preference preference = findPreference(s);

            if(preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;

                if(s.equals(getString(R.string.pref_key_characterUnit))) {
                    listPreference.setSummary(String.format(getString(R.string.pref_characterUnit_Summary),
                            listPreference.getEntry()));
                } else if(s.equals(getString(R.string.pref_key_keyboardSize))) {
                    listPreference.setSummary(String.format(getString(R.string.pref_keyboardSize_summary),
                            listPreference.getEntry()));
                }
            }
        }
    }

    private static void updateListPreferenceSummary(ListPreference listPreference, String summary) {
        // set formatted ListPreference summary
        String currentValue = String.valueOf(listPreference.getEntry());
        listPreference.setSummary(String.format(summary, currentValue));
    }
}