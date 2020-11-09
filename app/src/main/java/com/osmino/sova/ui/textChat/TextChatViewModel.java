package com.osmino.sova.ui.textChat;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;

import com.osmino.sova.preference.Preference_SovaChat;


public class TextChatViewModel extends AndroidViewModel {

    public enum ChatMode {
        VOICE_CHAT,
        TEXT_CHAT,
        AVATAR_CHAT
    }

    private ChatMode mChatMode;

    Preference_SovaChat mSovaChatPreference;

    public TextChatViewModel(final Application application) {
        super(application);

        mSovaChatPreference = Preference_SovaChat.getInstance(getApplication().getApplicationContext());
        mChatMode = mSovaChatPreference.getChatMode();
        if (mChatMode == null) {
            mChatMode = TextChatViewModel.ChatMode.VOICE_CHAT;
        }

    }

    public ChatMode getChatMode() {
        return mChatMode;
    }

    public void setChatMode(ChatMode chatMode) {
        mChatMode = chatMode;
        mSovaChatPreference.putChatMode(chatMode);
    }

    public int getLastKey() {
        return mSovaChatPreference.getTextChatLastKey();
    }

    public void setLastKey(int lastKey) {
        mSovaChatPreference.putTextChatLastKey(lastKey);
    }
}