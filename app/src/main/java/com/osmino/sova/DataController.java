package com.osmino.sova;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.osmino.sova.db.SovaDB;
import com.osmino.sova.model.Assistant;
import com.osmino.sova.model.AssistantController;
import com.osmino.sova.model.chat.Message;
import com.osmino.sova.preference.Preference_SovaChat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import leakcanary.AppWatcher;

import static com.osmino.sova.preference.SettingsActivity.ASSISTANT_CHANGED;

public class DataController {
    private static final String TAG = "DataController";
    private AssistantController assistant;
    private final SovaDB db;
    private final Preference_SovaChat mSovaChatPreference;
    private final Context mContext;

    private final ExecutorService diskIoExecutor = Executors.newSingleThreadExecutor();

    public static final String ASSISTANT_CONTROLLER_CHANGED = "com.osmino.sova.assistant.controller.changed";

    DataController(Context context) {
        mContext = context;
        db = SovaDB.create(context);
        mSovaChatPreference = Preference_SovaChat.getInstance(context.getApplicationContext());

        if (mSovaChatPreference.containsAssistantId()) {
            createAssistant();
        }

        if (!mSovaChatPreference.getInitialCompleted()) {
            diskIoExecutor.execute(() -> {
                if (db.assistantDao().getCount() == 0) {
                    db.assistantDao()
                            .insert(new Assistant("Элиза", "https://biz.nanosemantics.ru/", "ae83a6cc-8c54-4123-9fbe-1a4c9a8720d2", true, false));
                    db.assistantDao()
                            .insert(new Assistant("Лисичка", "https://biz.nanosemantics.ru/", "b03822f6-362d-478b-978b-bed603602d0e", false, true));
                    mSovaChatPreference.putAssistantId(db.assistantDao().getFirstAssistantId());
                    createAssistant();
                }
                mSovaChatPreference.putInitialCompleted(true);
            });
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ASSISTANT_CHANGED);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ASSISTANT_CHANGED.equals(intent.getAction())) {
                    createAssistant();
                }
            }
        }, filter);
    }

    private void createAssistant() {
        diskIoExecutor.execute(() -> {
            Log.d(TAG, "Try load from db assistant with id=" + mSovaChatPreference.getAssistantId());
            Assistant assistantRecord = db.assistantDao().getAssistant(mSovaChatPreference.getAssistantId());
            if (assistantRecord != null) {
                destroyAssistantController(assistant);
                Log.d(TAG, "Switch to assistant id: " + assistantRecord.getId() + ", name: "  + assistantRecord.getName() + ", token: " + assistantRecord.getToken());
                assistant = new AssistantController(mContext, assistantRecord, db, diskIoExecutor);
                mContext.sendBroadcast(new Intent(ASSISTANT_CONTROLLER_CHANGED));
            }
        });
    }

    private void destroyAssistantController(AssistantController assistantToDestroy) {
        if (assistantToDestroy != null) {
            AppWatcher.INSTANCE.getObjectWatcher().watch(assistantToDestroy, "Assistant switch");
            assistantToDestroy.onDestroy();
        }
    }

    public AssistantController getAssistantController() {
        return assistant;
    }

    private static volatile DataController instance;

    public static DataController getInstance(Context context) {
        DataController localInstance = instance;
        if (localInstance == null) {
            synchronized (DataController.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new DataController(context);
                }
            }
        }
        return localInstance;
    }

    public LiveData<List<Assistant>> getAssistantsList(){
        return db.assistantDao().getAll();
    }

    public void addAssistant(String name, String uri, String token, boolean listen){
        diskIoExecutor.execute(() -> {
            long insertedId = db.assistantDao().insert(new Assistant(name, uri, token, listen, false));
            if (!mSovaChatPreference.containsAssistantId()) {
                mSovaChatPreference.putAssistantId(insertedId);
                mContext.sendBroadcast(new Intent(ASSISTANT_CHANGED));
            }
        });
    }

    public void getAssistant(int assistantId, GetAssistantCallback callback) {
        diskIoExecutor.execute(() -> {
            Assistant assistant = db.assistantDao().getAssistant(assistantId);
            if (callback != null) {
                callback.onAssistant(assistant);
            }
        });
    }

    public void updateAssistant(Assistant assistantToUpdate) {
        diskIoExecutor.execute(() -> {
            db.assistantDao().update(assistantToUpdate);
        });
    }

    public void deleteAssistant(int assistantId) {
        diskIoExecutor.execute(() -> {
            db.assistantDao().delete(assistantId);
            if (assistantId == mSovaChatPreference.getAssistantId()) {
                destroyAssistantController(assistant);
                assistant = null;
                mSovaChatPreference.removeAssistantId();
                if (db.assistantDao().getCount() > 0) {
                    mSovaChatPreference.putAssistantId(db.assistantDao().getFirstAssistantId());
                }
                mContext.sendBroadcast(new Intent(ASSISTANT_CHANGED));
            }
        });
    }

    public interface GetMessagesCallback {
        void onMessages(List<Message> messages);
    }

    public interface GetAssistantCallback {
        void onAssistant(Assistant assistant);
    }

}
