package com.osmino.sova;

import static com.osmino.sova.DataController.ASSISTANT_CONTROLLER_CHANGED;
import static com.stfalcon.chatkit.messages.MessageHolders.DOUBLE_TAP_EVENT;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.osmino.sova.model.AssistantController;
import com.osmino.sova.preference.SettingsActivity;
import com.osmino.sova.ui.AvatarFragment;
import com.osmino.sova.ui.textChat.TextChatFragment;
import com.osmino.sova.ui.textChat.TextChatViewModel;
import com.osmino.sova.ui.textChat.TextChatViewModel.ChatMode;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class MainActivity  extends AppCompatActivity implements TextView.OnEditorActionListener, TextWatcher, View.OnClickListener {
    private final String TAG = "MainActivity";
    private EditText msgInput;

    private TextChatViewModel textChatViewModel;
    private AppCompatImageButton msgSendBtn;

    private DataController dc;
    private MediaRecorder recorder;
    private long startHTime = 0L;
    private String audioFileName;
    private boolean isNetworkConnected;

    private ViewPager2 mPager;
    private FragmentStateAdapter mPagerAdapter;
    private static final int pageIndexAvatar = 0;
    private static final int pageIndexChat = 1;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 42;
    private static final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean audioRecordingPermissionGranted = false;
    private MaterialButton btnVoiceInput;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (android.net.ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                checkConnection();
            } else if (ASSISTANT_CONTROLLER_CHANGED.equals(action)) {
                updatePagerStatus();
            } else if (DOUBLE_TAP_EVENT.equals(action)) {
                textChatViewModel.setChatMode(TextChatViewModel.ChatMode.VOICE_CHAT);
                UpdateLayout();
            }
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            isNetworkConnected = true;
        }
        @Override
        public void onLost(@NonNull Network network) {
            isNetworkConnected = false;
        }
    };

    private ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            boolean chatModeChanged = false;
            if (position == pageIndexAvatar) {
                if (textChatViewModel.getChatMode() != TextChatViewModel.ChatMode.AVATAR_CHAT) {
                    textChatViewModel.setChatMode(TextChatViewModel.ChatMode.AVATAR_CHAT);
                    chatModeChanged = true;
                }
            } else if (position == pageIndexChat) {
                if (textChatViewModel.getChatMode() != TextChatViewModel.ChatMode.VOICE_CHAT &&
                        textChatViewModel.getChatMode() != TextChatViewModel.ChatMode.TEXT_CHAT) {
                    textChatViewModel.setChatMode(TextChatViewModel.ChatMode.VOICE_CHAT);
                    chatModeChanged = true;
                }
            } else {
                Log.e(TAG, "Unknown page index " + position);
            }

            if (chatModeChanged) {
                UpdateLayout();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dc = DataController.getInstance(getApplicationContext());
        textChatViewModel = new ViewModelProvider(this).get(TextChatViewModel.class);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ColorStateList micIconColor = ColorStateList.valueOf(getResources().getColor(R.color.mic_primary, getTheme()));
        ColorStateList micIconBackColor = ColorStateList.valueOf(getResources().getColor(R.color.btn_voice_input, getTheme()));

        mPager = findViewById(R.id.pager);
        mPager.setUserInputEnabled(false);
        updatePagerStatus();
        mPagerAdapter = new ScreenSlidePagerAdapter(this);
        mPager.setAdapter(mPagerAdapter);
        mPager.registerOnPageChangeCallback(pageChangeCallback);

        findViewById(R.id.btn_switch_to_voice).setOnClickListener(this);
        findViewById(R.id.btn_attach).setOnClickListener(this);
        findViewById(R.id.btn_settings).setOnClickListener(this);
        findViewById(R.id.btn_keyboard).setOnClickListener(this);

        msgSendBtn = findViewById(R.id.btn_send_msg);
        msgSendBtn.setOnClickListener(this);
        msgSendBtn.setEnabled(false);

        msgInput = findViewById(R.id.msgInput);
        msgInput.addTextChangedListener(this);
        msgInput.setOnEditorActionListener(this);

        btnVoiceInput = findViewById(R.id.btn_voice_input);
        btnVoiceInput.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        vibrator.vibrate(30);
                    }

                    if (!isNetworkConnected) {
                        showNetworkNotConnectedMsg();
                        return false;
                    }

                    AssistantController controller = dc.getAssistantController();
                    if (controller != null) {
                        controller.stopPlay();
                    }

                    btnVoiceInput.setBackgroundTintList(micIconColor);
                    btnVoiceInput.setIconTint(micIconBackColor);

                    startRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    btnVoiceInput.setBackgroundTintList(micIconBackColor);
                    btnVoiceInput.setIconTint(micIconColor);

                    stopRecording();
                    v.performClick();
                    break;
            }
            return false;
        });


        IntentFilter intentFilter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallback();
        } else {
            intentFilter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        }
        intentFilter.addAction(ASSISTANT_CONTROLLER_CHANGED);
        intentFilter.addAction(DOUBLE_TAP_EVENT);
        registerReceiver(broadcastReceiver, intentFilter);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    private void showNetworkNotConnectedMsg() {
        showToast(R.string.error_mainactivity_not_connected);
    }

    private void checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        isNetworkConnected = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (grantResults.length > 0) {
                    audioRecordingPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                } break;
        }

//        if (!audioRecordingPermissionGranted) {
//            finish();
//        }
    }

    private void updatePagerStatus() {
        boolean hasAvatar = false;
        if (dc.getAssistantController() != null) {
            hasAvatar = dc.getAssistantController().getAssistant().getHasAvatar();
        }

        mPager.setUserInputEnabled(hasAvatar);
        if (!hasAvatar) {
            if (textChatViewModel.getChatMode() == ChatMode.AVATAR_CHAT) {
                textChatViewModel.setChatMode(ChatMode.VOICE_CHAT);
                UpdateLayout();
            }
        }
    }

    private static class ScreenSlidePagerAdapter  extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new AvatarFragment();
            } else if (position == 1) {
                return new TextChatFragment();
            } else {
                return null;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        UpdateLayout();
    }

    private void UpdateLayout() {
        Log.d(TAG, "UpdateLayout(): " + textChatViewModel.getChatMode());
        switch (textChatViewModel.getChatMode()) {
            case VOICE_CHAT:
                if (mPager.getCurrentItem() != pageIndexChat) {
                    mPager.setCurrentItem(pageIndexChat);
                }
                findViewById(R.id.voice_input_bar).setVisibility(View.VISIBLE);
                findViewById(R.id.text_input_bar).setVisibility(View.GONE);
                hideSoftKeyboard();
                break;
            case TEXT_CHAT:
                if (mPager.getCurrentItem() != pageIndexChat) {
                    mPager.setCurrentItem(pageIndexChat);
                }
                findViewById(R.id.voice_input_bar).setVisibility(View.GONE);
                findViewById(R.id.text_input_bar).setVisibility(View.VISIBLE);

                msgInput.requestFocus();
                showSoftKeyboard();
                break;
            case AVATAR_CHAT:
                if (mPager.getCurrentItem() != pageIndexAvatar) {
                    mPager.setCurrentItem(pageIndexAvatar);
                }
                findViewById(R.id.voice_input_bar).setVisibility(View.VISIBLE);
                findViewById(R.id.text_input_bar).setVisibility(View.GONE);
                hideSoftKeyboard();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } else {
            unregisterReceiver(broadcastReceiver);
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_keyboard: {
                textChatViewModel.setChatMode(TextChatViewModel.ChatMode.TEXT_CHAT);
                UpdateLayout();
            } break;

            case R.id.btn_switch_to_voice: {
                textChatViewModel.setChatMode(TextChatViewModel.ChatMode.VOICE_CHAT);
                UpdateLayout();
            } break;

            case R.id.btn_send_msg: {
                if (isNetworkConnected) {
                    sendMessage();
                } else {
                    showNetworkNotConnectedMsg();
                }
            } break;

            case R.id.btn_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
            } break;
        }
    }

    private void sendMessage() {
        String msg = msgInput.getText().toString();
        if (!msg.isEmpty() && dc.getAssistantController() != null) {
            dc.getAssistantController().sendRequest(msg);

            msgInput.setText("");
        }
    }

    private void showSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
        }
    }

    private void hideSoftKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        msgSendBtn.setEnabled(s.length() > 0);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null) {
            // if shift key is down, then we want to insert the '\n' char in the TextView;
            // otherwise, the default action is to send the message.
            if (!event.isShiftPressed()) {
                sendMessage();
                return true;
            }
            return false;
        }

        sendMessage();
        return true;
    }

    void startRecording() {
        String uuid = UUID.randomUUID().toString();
        audioFileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/" + uuid + ".3gp";
        Log.d(TAG, audioFileName);

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(audioFileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "recorder.prepare() failed");
        }
        recorder.start();
        startHTime = SystemClock.uptimeMillis();
    }

    void stopRecording() {
        if (recorder != null) {
            recorder.release();
            recorder = null;

            long timeInMilliseconds = SystemClock.uptimeMillis() - startHTime;

            if (timeInMilliseconds > 500) {
                if (dc.getAssistantController() != null) {
                    dc.getAssistantController().RecognizeVoice(audioFileName);
                }
            } else {
                showToast(R.string.voice_record_toast);
            }
        }
    }

    private void showToast(int textId) {
        Toast toast = Toast.makeText(this, textId, Toast.LENGTH_SHORT);
        View view = toast.getView();
        view.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent, getTheme())));
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void registerNetworkCallback()
    {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            connectivityManager.registerDefaultNetworkCallback(networkCallback);
            isNetworkConnected = false;
        } catch (Exception e){
            isNetworkConnected = false;
        }
    }
}
