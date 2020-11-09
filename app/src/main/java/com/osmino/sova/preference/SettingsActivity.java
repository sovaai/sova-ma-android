package com.osmino.sova.preference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osmino.sova.DataController;
import com.osmino.sova.R;
import com.osmino.sova.SovaApp;
import com.osmino.sova.model.Assistant;

import com.osmino.sova.model.AssistantController;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    public static final String ASSISTANT_CHANGED = "com.osmino.sova.assistant.changed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, EditTextPreference.OnBindEditTextListener {
        private static final String TAG = "SettingsFragment";
        private DataController dc;
        private Preference_SovaChat mSovaChatPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            dc = DataController.getInstance(requireActivity().getApplicationContext());
            mSovaChatPreference = Preference_SovaChat.getInstance(requireContext());

            ListPreference darkThemePreference = findPreference("preference_dark_mode");
            if (darkThemePreference != null) {
                darkThemePreference.setOnPreferenceChangeListener((preference1, newValue) -> SovaApp.setDarkThemeMode((String)newValue));
            }

            ListPreference assistantPreference = findPreference("preference_assistant");
            if (assistantPreference != null) {
                assistantPreference.setOnPreferenceChangeListener((preference1, newValue) -> {
                    Log.d(TAG, "Assistant changed to id=" + newValue);
                    mSovaChatPreference.putAssistantId(Integer.parseInt(newValue.toString()));
                    requireContext().sendBroadcast(new Intent(ASSISTANT_CHANGED));
                    return true;
                });
                dc.getAssistantsList().observe(requireActivity(), assistants -> {
                    List<String> entries = new ArrayList<>();
                    List<String> values = new ArrayList<>();
                    for(Assistant assistant: assistants) {
                        entries.add(assistant.getName());
                        values.add(Integer.toString(assistant.getId()));
                    }
                    assistantPreference.setEntries(entries.toArray(new CharSequence[0]));
                    assistantPreference.setEntryValues(values.toArray(new CharSequence[0]));
                    if (mSovaChatPreference.containsAssistantId()) {
                        Log.d(TAG, "Set assistant to " + mSovaChatPreference.getAssistantId());
                        assistantPreference.setEnabled(true);
                        assistantPreference.setValue("0");
                        assistantPreference.setValue(Long.toString(mSovaChatPreference.getAssistantId()));
                    } else {
                        assistantPreference.setEnabled(entries.size() > 0);
                    }
                });
            }

        }

        @Override
        public void onDisplayPreferenceDialog(final Preference preference) {
            if (preference.getKey().equals("preference_assistant")) {
                AssistantListPreferenceDialogFragment f = AssistantListPreferenceDialogFragment.newInstance(preference.getKey());
                f.setOnButtonClickListener((position, v) -> {
                    CharSequence[] values = ((ListPreference)preference).getEntryValues();
                    if (values.length >= position) {
                        Log.e(TAG, "");
                    }
                    int assistantId = Integer.parseInt(values[position].toString());
                    if (v.getId() == R.id.btnDeleteAssistant) {
                        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.assistant_dialog_delete_contact_title)
                                .setMessage(R.string.assistant_dialog_delete_contact_message)
                                .setNegativeButton(R.string.assistant_dialog_delete_contact_btn_cancel, (dialog, which) -> {})
                                .setPositiveButton(R.string.assistant_dialog_delete_contact_btn_delete, (dialog, which) -> {
                                    dc.deleteAssistant(assistantId);
                                    f.dismiss();
                                })
                                .show();
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red, null));
                    } else if (v.getId() == R.id.btnEditAssistant) {
                        f.dismiss();
                        AddEditAssistantFragment assistantFragment = new AddEditAssistantFragment(true, assistantId);
                        assistantFragment.show(requireActivity().getSupportFragmentManager(), "assistantFragment");
                    }
                });
                f.setTargetFragment(this, 0);
                f.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            Activity settingsActivity = getActivity();
            if (settingsActivity == null) {
                Log.e(TAG, "Settings activity is null");
                return true;
            }

            String key = preference.getKey();
            if (key == null) {
                return super.onPreferenceTreeClick(preference);
            }

            if (key.equals("preference_clear_all")) {
                clearHistoryDialog(settingsActivity);
                return true;
            } else if (key.equals("preference_download_log")) {
                AssistantController assistantController = dc.getAssistantController();
                if (assistantController != null) {
                    assistantController.getAllMessages(messages -> {
                        Gson gson = new GsonBuilder().create();
                        String json = gson.toJson(messages);

                        try {
                            File outputFile = File.createTempFile("sova", ".json", requireContext().getCacheDir());
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            fos.write(json.getBytes());
                            fos.close();

                            Uri fileUri = FileProvider.getUriForFile(requireContext().getApplicationContext(),
                                    requireContext().getPackageName() + ".fileprovider", outputFile);

                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "Share via"));
                        } catch (IOException e) {
                            Log.e(TAG, "Can`t write json to temp file", e);
                            FirebaseCrashlytics.getInstance().recordException(e);
                        } catch (Exception e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    });
                }
                return true;
            } else if (key.equals("preference_send_email_to_support")) {
                sendMailToSupport(settingsActivity);
                return true;
            } else if (key.equals("preference_add_assistant")) {
                AddEditAssistantFragment assistantFragment = new AddEditAssistantFragment(false, 0);
                assistantFragment.show(requireActivity().getSupportFragmentManager(), "assistantFragment");
            }
            else if (key.equals("preference_about_application")) {

            }

            return super.onPreferenceTreeClick(preference);
        }

        void clearHistoryDialog(Context context) {
            new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.preference_clear_all))
                .setMessage(context.getString(R.string.preference_clear_all_confirm_message))
                .setPositiveButton(android.R.string.yes, (arg0, arg1) -> {
                    Log.d(TAG, "Clear all");
                    AssistantController assistantController = dc.getAssistantController();
                    if (assistantController != null) {
                        assistantController.deleteAllMessages();
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    Log.d(TAG, "Cancel");
                    dialog.cancel();
                }).create().show();
        }

        void sendMailToSupport(Context context) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.preference_support_email)});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.tech_support_email_subject));
            context.startActivity(Intent.createChooser(emailIntent, "Email"));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.d(TAG, "onPreferenceChange: " + newValue);
            return false;
        }

        @Override
        public void onBindEditText(@NonNull EditText editText) {
            Log.d(TAG, "onBindEditText");
            editText.setInputType(EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
            editText.setOnEditorActionListener((view, actionId, event) -> false);
        }
    }
}