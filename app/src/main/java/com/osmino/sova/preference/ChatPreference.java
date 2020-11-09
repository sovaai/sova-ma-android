package com.osmino.sova.preference;

import com.google.gson.Gson;
import com.osmino.sova.ui.textChat.TextChatViewModel;
import com.skydoves.preferenceroom.KeyName;
import com.skydoves.preferenceroom.PreferenceEntity;
import com.skydoves.preferenceroom.PreferenceTypeConverter;
import com.skydoves.preferenceroom.TypeConverter;

@PreferenceEntity("SovaChat")
class SovaChatPreference {
    public static class ChatModeConverter extends PreferenceTypeConverter<TextChatViewModel.ChatMode> {
        private final Gson gson;

        // default constructor will be called by PreferenceRoom
        public ChatModeConverter(Class<TextChatViewModel.ChatMode> clazz) {
            super(clazz);
            this.gson = new Gson();
        }

        @Override
        public String convertObject(TextChatViewModel.ChatMode pet) {
            return gson.toJson(pet);
        }

        @Override
        public TextChatViewModel.ChatMode convertType(String string) {
            return gson.fromJson(string, TextChatViewModel.ChatMode.class);
        }
    }
    @TypeConverter(ChatModeConverter.class)
    @KeyName("chatMode") protected final TextChatViewModel.ChatMode chatMode = TextChatViewModel.ChatMode.VOICE_CHAT;

    @KeyName("textChatLastKey") protected final int lastKey = 0;

    @KeyName("assistantId") protected final long assistantId = 0;

    @KeyName("agreementAccepted") protected final boolean agreementAccepted = false;

    @KeyName("initialCompleted") protected final boolean initialCompleted = false;
}
