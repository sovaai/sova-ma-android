package com.osmino.sova.model;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.PositionalDataSource;

import com.osmino.sova.model.chat.Message;

import java.util.ArrayList;
import java.util.List;

public class MessagesDataSourceFactory extends DataSource.Factory<Integer, Message> {
    static class MessagesDataSource extends PositionalDataSource<Message> {
        String TAG = "MessagesDataSource";

        private List<Message> msgList;
        private boolean observerInited = false;

        public MessagesDataSource(LiveData<List<Message>> messagesList) {
            Log.d(TAG, "MessagesDataSource constructor");

            msgList = messagesList.getValue();

            new Handler(Looper.getMainLooper()).post(() ->
                    messagesList.observeForever(messages -> {
                    Log.d(TAG, "observeForever changes. messages: " + messages.toString());
                    if (!observerInited) {
                        Log.d(TAG, "init list");
                        observerInited = true;
                    } else {
                        Log.d(TAG, "invalidate");
                        msgList = messages;
                        invalidate();
                    }
            }));
        }

        @Override
        public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Message> callback) {
            Log.d(TAG, "loadInitial, requestedStartPosition = " + params.requestedStartPosition +
                            ", requestedLoadSize = " + params.requestedLoadSize +
                            ", msgList = " + msgList);
            List<Message> result = new ArrayList<>();
            if (msgList != null && params.requestedStartPosition < msgList.size()) {
                int endPos = params.requestedStartPosition + params.requestedLoadSize;
                if (endPos > msgList.size()) {
                    endPos = msgList.size();
                }
                result = msgList.subList(params.requestedStartPosition, endPos);
                Log.d(TAG, "loadInitial, Return result" + result);
            }
            callback.onResult(result, 0);
        }

        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Message> callback) {
            Log.d(TAG, "loadRange, startPosition = " + params.startPosition + ", loadSize = " + params.loadSize);
            List<Message> result = new ArrayList<>();
            if (msgList != null &&
                    params.startPosition < msgList.size() &&
                    params.startPosition + params.loadSize <= msgList.size()) {
                result = msgList.subList(params.startPosition, params.startPosition + params.loadSize);
            }
            callback.onResult(result);
        }
    }

    private final LiveData<List<Message>> messageList;

    MessagesDataSourceFactory(LiveData<List<Message>> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public DataSource<Integer, Message> create() {
        return new MessagesDataSource(this.messageList);
    }
}
