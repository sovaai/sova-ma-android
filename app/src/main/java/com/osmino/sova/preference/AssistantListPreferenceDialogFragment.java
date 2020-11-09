package com.osmino.sova.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import com.osmino.sova.R;

public class AssistantListPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    public interface OnButtonClickListener {
        void onButtonClick(int position, View v);
    }

    class AssistantListAdapter extends ArrayAdapter<CharSequence>{
        public AssistantListAdapter(@NonNull final Context context,
                                    final int resource,
                                    final int textViewResourceId,
                                    @NonNull final CharSequence[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable final View convertView, @NonNull final ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            CheckedTextView checkedTextView = view.findViewById(android.R.id.text1);
            if (checkedTextView != null) {
                checkedTextView.setChecked(position == mClickedDialogEntryIndex);
            }

            ImageButton editButton = view.findViewById(R.id.btnEditAssistant);
            if (editButton != null) {
                editButton.setOnClickListener(v -> {
                    if (mButtonClickListener != null) {
                        mButtonClickListener.onButtonClick(position, v);
                    }
                });
            }

            ImageButton deleteButton = view.findViewById(R.id.btnDeleteAssistant);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(v -> {
                    if (mButtonClickListener != null) {
                        mButtonClickListener.onButtonClick(position, v);
                    }
                });
            }
            return view;
        }
    }

    private static final String SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index";
    private static final String SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "ListPreferenceDialogFragment.entryValues";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
            int mClickedDialogEntryIndex;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    private OnButtonClickListener mButtonClickListener;

    public static AssistantListPreferenceDialogFragment newInstance(String key) {
        final AssistantListPreferenceDialogFragment fragment = new AssistantListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final ListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException(
                        "ListPreference requires an entries array and an entryValues array.");
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mButtonClickListener = listener;
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(final Builder builder) {
        super.onPrepareDialogBuilder(builder);

        ListView listview = new ListView(requireContext());
        listview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        listview.setCacheColorHint(0);
        listview.setBackgroundColor(Color.WHITE);

        AssistantListAdapter adapter = new AssistantListAdapter(requireActivity(),
                R.layout.assistant_list_preference_item,
                android.R.id.text1,
                mEntries);

        listview.setAdapter(adapter);
        builder.setView(listview);

        listview.setOnItemClickListener((parent, view, position, id) -> {
            Log.d("Dialog", "View click " + view.toString() + ", position: " + position + ", id: " + id);
            mClickedDialogEntryIndex = position;

            AssistantListPreferenceDialogFragment.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            this.dismiss();
        });

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            final ListPreference preference = getListPreference();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }
}
