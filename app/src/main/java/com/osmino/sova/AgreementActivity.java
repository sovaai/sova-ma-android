package com.osmino.sova;

import android.content.Intent;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.osmino.sova.preference.Preference_SovaChat;

public class AgreementActivity extends AppCompatActivity {
    private Preference_SovaChat mSovaChatPreference;

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSovaChatPreference = Preference_SovaChat.getInstance(getApplicationContext());
        if (mSovaChatPreference.containsAgreementAccepted() && mSovaChatPreference.getAgreementAccepted()) {
            startMainActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_agreement);

        Button btnOk = findViewById(R.id.btnOk);
        btnOk.setEnabled(false);
        btnOk.setOnClickListener(v -> {
            mSovaChatPreference.putAgreementAccepted(true);
            startMainActivity();
            finish();
        });

        CheckBox checkBox = findViewById(R.id.chbAgree);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnOk.setEnabled(isChecked);
        });

    }
}