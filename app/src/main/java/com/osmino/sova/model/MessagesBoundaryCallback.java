package com.osmino.sova.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.paging.PagedList;
import androidx.paging.PagingRequestHelper;

import com.osmino.sova.api.CAPI;
import com.osmino.sova.api.NetworkAPI;
import com.osmino.sova.model.chat.Message;

import java.util.concurrent.Executor;

class MessagesBoundaryCallback extends PagedList.BoundaryCallback<Message> {
    private final String TAG = "BoundaryCallback";
    private final Executor ioExecutor;
    private final NetworkAPI networkAPI;
    private final PagingRequestHelper helper;

    MessagesBoundaryCallback(Executor ioExecutor, NetworkAPI networkAPI) {
        this.ioExecutor = ioExecutor;
        this.networkAPI = networkAPI;
        this.helper = new PagingRequestHelper(ioExecutor);
    }

    @Override
    public void onZeroItemsLoaded() {
        Log.d(TAG, "onZeroItemsLoaded");
        super.onZeroItemsLoaded();
    }

    @Override
    public void onItemAtFrontLoaded(@NonNull Message itemAtFront) {
        Log.d(TAG, "onItemAtFrontLoaded");
        super.onItemAtFrontLoaded(itemAtFront);
    }

    @Override
    public void onItemAtEndLoaded(@NonNull Message itemAtEnd) {
        Log.d(TAG, "onItemAtEndLoaded");
        super.onItemAtEndLoaded(itemAtEnd);
    }
}
