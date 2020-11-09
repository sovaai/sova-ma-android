package com.osmino.sova.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.osmino.sova.api.capi.ChatEvent;
import com.osmino.sova.api.capi.ChatInit;
import com.osmino.sova.api.capi.ChatRequest;
import com.osmino.sova.api.capi.ChatRequest.Result;
import com.osmino.sova.api.capi.RequestResult;
import com.osmino.sova.model.chat.Message;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkAPI {
    private final String TAG = "NetworkAPI";
    private final String UUID;
    private String CUID;
    private CAPI capi;
    private NetworkAPICallback networkAPICallback;

    public interface NetworkAPICallback {
        void onChatInited(String cuid);

        void onCUIDChanged(String cuid);

        void onNewResponse(String response, int emotion);

        void onNetworkError(String errorText);
    }

    public NetworkAPI(String uuid, String cuid, NetworkAPICallback networkAPICallback) {
        UUID = uuid;
        CUID = cuid;

        this.networkAPICallback = networkAPICallback;

        capi = CAPI.create();

        chatInit();
    }

    public void setNetworkAPICallback(NetworkAPICallback networkAPICallback) {
        this.networkAPICallback = networkAPICallback;
    }

    public String getUUID(){
        return UUID;
    }

    private void chatInit() {
        ChatInit chatInit = new ChatInit();
        chatInit.uuid = UUID;
        chatInit.cuid = CUID;
        capi.ChatInit(chatInit).enqueue(new Callback<CAPI.ResultChatInit>() {
            @Override
            public void onResponse(@NonNull Call<CAPI.ResultChatInit> call, @NonNull Response<CAPI.ResultChatInit> response) {
                if (response.body() != null && response.body().result != null) {
                    Log.d(TAG, "Chat init response. cuid=" + response.body().result.cuid);
                    CUID = response.body().result.cuid;
                    if (networkAPICallback != null) {
                        networkAPICallback.onChatInited(CUID);
                    }
                } else {
                    Log.e(TAG, "Wrong response from server");
                }

            }

            @Override
            public void onFailure(@NonNull Call<CAPI.ResultChatInit> call, @NonNull Throwable t) {
                Log.e(TAG, "Fail call chatInit: " + t.toString());
            }
        });
    }

    Callback<ChatRequest.Result> processMessageResult = new Callback<ChatRequest.Result>() {
        @Override
        public void onResponse(@NonNull Call<ChatRequest.Result> call, @NonNull Response<ChatRequest.Result> response) {
            if (response.body() == null || response.body().result == null) {
                Log.e(TAG, "Empty body() in response");
                return;
            }

            RequestResult result = response.body().result;
            if (result.cuid != null  && !result.cuid.equals(CUID)) {
                CUID = result.cuid;
                if (networkAPICallback != null) {
                    networkAPICallback.onCUIDChanged(CUID);
                }
            }

            int emotion = 0;
            if (result.animation != null) {
                emotion = result.animation.type;
            }

            Log.v(TAG, "Request response. Text: '" + result.text.value + "', emotion: " + emotion);
            String fixedURLText = result.text.value.replaceAll("href=\"event:", "href=\"");
            Log.v(TAG, "Fixed text: " + fixedURLText);
            if (networkAPICallback != null) {
                networkAPICallback.onNewResponse(fixedURLText, emotion);
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChatRequest.Result> call, @NonNull Throwable t) {
            Log.e(TAG, "Fail call Chat API: " + t.toString());
            if (networkAPICallback != null) {
                networkAPICallback.onNetworkError(t.getMessage());
            }
        }
    };

    public void sendRequest(Message message) {
        if (CUID == null || CUID.isEmpty()) {
            Log.e(TAG, "CUID must be set");
            chatInit();
            return;
        }
        ChatRequest request = new ChatRequest();
        request.cuid = CUID;
        request.text = message.getText();

        capi.ChatRequest(request).enqueue(processMessageResult);
    }

    public void requestReadyChatEvent() {
        if (CUID == null || CUID.isEmpty()) {
            Log.e(TAG, "CUID must be set");
            chatInit();
            return;
        }
        ChatEvent chatEventRequest = new ChatEvent();
        chatEventRequest.cuid = CUID;
        chatEventRequest.euid = "00b2fcbe-f27f-437b-a0d5-91072d840ed3"; // READY

        capi.ChatEvent(chatEventRequest).enqueue(processMessageResult);
    }
}
