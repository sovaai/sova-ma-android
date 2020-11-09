package com.osmino.sova.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import com.osmino.sova.DataController;
import com.osmino.sova.R;
import com.osmino.sova.model.Assistant;

public class AddEditAssistantFragment extends DialogFragment implements DialogInterface.OnClickListener, View.OnClickListener {
    private static final String TAG = "AddAssistant";
    private final boolean mIsEditMode;
    private final int mAssistantId;
    private DataController dc;
    private Assistant mEditedAssistant;
    private EditText assistant_name, assistant_api_url, assistant_token;
    private SwitchCompat assistant_listen_word;


    AddEditAssistantFragment(boolean isEditMode, int assistantId) {
        super();
        mIsEditMode = isEditMode;
        mAssistantId = assistantId;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        dc = DataController.getInstance(context.getApplicationContext());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        builder.setView(inflater.inflate(R.layout.dialog_add_assistant, null));

        if (mIsEditMode) {
            builder.setTitle(R.string.preference_assistant_edit_dialog_title)
                    .setNeutralButton(R.string.preference_assistant_edit_dialog_ok, this)
                    .setNegativeButton(R.string.preference_assistant_dialog_cancel, this);
        } else {
            builder.setTitle(R.string.preference_assistant_create_dialog_title)
                    .setNeutralButton(R.string.preference_assistant_create_dialog_ok, this)
                    .setNegativeButton(R.string.preference_assistant_dialog_cancel, this);
        }
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog) {
            Button theButton = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_NEUTRAL);
            theButton.setOnClickListener(this);
        }
        if (dialog != null) {
            assistant_name = dialog.findViewById(R.id.assistant_name);
            assistant_api_url = dialog.findViewById(R.id.assistant_api_url);
            assistant_token = dialog.findViewById(R.id.assistant_token);
            assistant_listen_word = dialog.findViewById(R.id.assistant_listen_word);
            if (assistant_listen_word != null) {
                assistant_listen_word.setVisibility(View.GONE);
            }

            if (mIsEditMode) {
                dc.getAssistant(mAssistantId, assistant -> {
                    mEditedAssistant = assistant;
                    if (assistant != null) {
                        assistant_name.setText(assistant.getName());
                        assistant_api_url.setText(assistant.getApiURI());
                        assistant_token.setText(assistant.getToken());
                        assistant_listen_word.setChecked(assistant.getListen());
                    }
                });
            }
        } else {
            Log.e(TAG, "Dialog in null");
            throw new IllegalStateException("Dialog in null");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }

    @Override
    public void onClick(View v) {
        if (assistant_name.getText().length() == 0) {
            assistant_name.setError(getString(R.string.preference_assistant_edit_dialog_must_be_not_empty));
        } else if (assistant_token.getText().length() == 0) {
            assistant_token.setError(getString(R.string.preference_assistant_edit_dialog_must_be_not_empty));
        } else if (dc != null) {
            if (mIsEditMode) {
                if (mEditedAssistant != null){
                    mEditedAssistant.setName(assistant_name.getText().toString());
                    mEditedAssistant.setApiURI(assistant_api_url.getText().toString());
                    mEditedAssistant.setToken(assistant_token.getText().toString());
                    mEditedAssistant.setListen(assistant_listen_word.isChecked());
                    dc.updateAssistant(mEditedAssistant);
                }
            } else {
                dc.addAssistant(assistant_name.getText().toString(),
                        assistant_api_url.getText().toString(),
                        assistant_token.getText().toString(),
                        assistant_listen_word.isChecked());
            }
            dismiss();
        }
    }
}
