package com.osmino.sova.model;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkInfo.State;
import androidx.work.WorkManager;
import com.osmino.sova.DataController.GetMessagesCallback;
import com.osmino.sova.R;
import com.osmino.sova.api.NetworkAPI;
import com.osmino.sova.db.SovaDB;
import com.osmino.sova.model.chat.Message;
import com.osmino.sova.workers.RecognizerWorker;
import com.osmino.sova.workers.VoicingWorker;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class AssistantController implements NetworkAPI.NetworkAPICallback {
    static final String TAG = "Assistant";

    private final SovaDB db;
    private final ExecutorService executorService;
    private long lastActionTime;
    private final NetworkAPI networkApi;
    private final Assistant assistantRecord;
    private final int assistantId;
    private final WorkManager workManager;
    private final Context mContext;
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private final List<Message> pendingMessages = new ArrayList<>();
    private Message activeMessage = null;
    private final Timer idleStateTimer = new Timer();

    public static final String ASSISTANT_EMOTION = "com.osmino.sova.assistant.emotion";
    public static final String ASSISTANT_EMOTION_PARAM = "emotion";

    class IdleTask extends TimerTask {
        @Override
        public void run() {
            synchronized (pendingMessages) {
                long delay = Math.round((4 + new Random().nextDouble() * 3) * 1000);
                if ((System.currentTimeMillis() - lastActionTime > 8000) &&
                        pendingMessages.isEmpty() &&
                        activeMessage == null) {
                    sendEmotion(4); // idle
                }
            }
        }
    }

    public AssistantController(@NonNull Context context,
                               @NonNull Assistant assistant,
                               @NonNull SovaDB db,
                               @NonNull ExecutorService executor) {
        mContext = context;
        this.db = db;
        this.networkApi = new NetworkAPI(assistant.getToken(), assistant.getCuid(), this);
        assistantRecord = assistant;
        assistantId = assistant.getId();
        executorService = executor;
        workManager = WorkManager.getInstance(mContext);
        mediaPlayer.setOnCompletionListener(mp -> {
            activeMessage = null;
            processPendingMessages();
        });

        lastActionTime = System.currentTimeMillis();
        idleStateTimer.scheduleAtFixedRate(new IdleTask(), 0, 1000);
    }

    public Assistant getAssistant() {
        return assistantRecord;
    }

    public void onDestroy() {
        idleStateTimer.cancel();
        idleStateTimer.purge();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        networkApi.setNetworkAPICallback(null);
    }

    protected void finalize() {
        Log.v(TAG, "Assistant id=" + assistantId + " deleted");
    }

    public int getId() {
        return assistantId;
    }

    public LiveData<PagedList<Message>> getMessages(int lastKey){
        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();

        MessagesBoundaryCallback boundaryCallback = new MessagesBoundaryCallback(executorService, this.networkApi);

        return new LivePagedListBuilder<>(this.db.messageDao().getAll(assistantId), config)
                .setBoundaryCallback(boundaryCallback)
                .setInitialLoadKey(lastKey)
                .build();
    }

    public void deleteAllMessages() {
        executorService.execute(()->{
            db.messageDao().deleteAll(assistantId);
        });
    }

    public void getAllMessages(GetMessagesCallback callback) {
        executorService.execute(()-> {
            List<Message> messages = db.messageDao().getAllList(assistantId);
            callback.onMessages(messages);
        });
    }

    public void addMessage(Message message) {
        executorService.execute(() -> {
                    db.messageDao().insert(message);
                });
    }

    private Message getMessage(String text, boolean isIncoming) {
        return new Message(getRandomId(), assistantId, text, isIncoming);
    }

    private String getRandomId() {
        return Long.toString(UUID.randomUUID().getLeastSignificantBits());
    }

    public Message getMessageFromMe(String text) {
        return getMessage(text, false);
    }

    public Message getMessageFromAssist(String text) {
        return getMessage(text, true);
    }

    public boolean sendRequest(String request) {
        outFromIdle();

        Message message = getMessageFromMe(request);
        addMessage(message);

        networkApi.sendRequest(message);
        return true;
    }

    private boolean playWav(String audioFilePath) {
        try {
            mediaPlayer.reset();
            FileInputStream fis = new FileInputStream(audioFilePath);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Error in mediaPlayer", e);
            return false;
        }
        return true;
    }

    public void stopPlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void processPendingMessages() {
        synchronized (pendingMessages) {
            if (pendingMessages.isEmpty()) {
                return;
            }

            if (/*!mediaPlayer.isPlaying() && */activeMessage != null) {
                activeMessage = null;
            }

            Message message = pendingMessages.get(0);
            if (message.voicingState == State.SUCCEEDED ||
                    message.voicingState == State.FAILED) {
                if (activeMessage == null) {
                    if (message.getEmotionNumber() > 0) {
                        sendEmotion(message.getEmotionNumber());
                    }
                    if (message.voicingState == State.SUCCEEDED) {
                        int delay = 0;
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            delay = 500;
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (playWav(message.voicingWavPath)) {
                                activeMessage = message;
                            }
                        }, delay);
                    }
                    pendingMessages.remove(0);
                }
            }
        }
    }

    private void sendEmotion(int emotion) {
        lastActionTime = System.currentTimeMillis();

        Intent intent = new Intent(ASSISTANT_EMOTION);
        intent.putExtra(ASSISTANT_EMOTION_PARAM, emotion);
        mContext.sendBroadcast(intent);
    }

    private void outFromIdle() {
        sendEmotion(0);
    }

    private void showWorkerErrorToast(WorkInfo workStatus, int baseError, String errorParamName) {
        String errTxt = mContext.getString(baseError);
        int errStrId = workStatus.getOutputData().getInt(errorParamName, 0);
        if (errStrId != 0) {
            errTxt += ": " + mContext.getString(errStrId);
        }
        Toast.makeText(mContext, errTxt, Toast.LENGTH_LONG).show();
    }

    public void voicingMessage(Message message) {
        outFromIdle();

        Data workerData = new Data.Builder()
                .putString(VoicingWorker.textKey, message.getText())
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest voicingRequest = new OneTimeWorkRequest.Builder(VoicingWorker.class)
                .setInputData(workerData)
                .setConstraints(constraints)
                .build();
        workManager.enqueue(voicingRequest);

        workManager
                .getWorkInfoByIdLiveData(voicingRequest.getId())
                .observeForever(workStatus -> {
                    Log.v(TAG, "Recognize voice worker status changed: " + workStatus.getState());
                    message.voicingState = workStatus.getState();
                    if (workStatus.getState() == WorkInfo.State.SUCCEEDED) {
                        String audioFilePath = workStatus.getOutputData().getString(VoicingWorker.VoicingAudio);
                        if (audioFilePath != null && !audioFilePath.isEmpty()) {
                            message.voicingWavPath = audioFilePath;
                        } else {
                            message.voicingState = WorkInfo.State.FAILED;
                        }
                        processPendingMessages();
                    } else if (workStatus.getState() == WorkInfo.State.FAILED) {
                        Log.e(TAG, "Voicing worker fail");
                        message.voicingState = WorkInfo.State.FAILED;
                        showWorkerErrorToast(workStatus, R.string.voicing_failed, VoicingWorker.VoicingError);
                        processPendingMessages();
                    }
                });
    }

    public void RecognizeVoice(String audioFileName) {
        outFromIdle();

        Data workerData = new Data.Builder()
                .putString(RecognizerWorker.audioFileNameKey, audioFileName)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest recognizeRequest = new OneTimeWorkRequest.Builder(RecognizerWorker.class)
                .setInputData(workerData)
                .setConstraints(constraints)
                .build();

        workManager
//                .beginUniqueWork("ASR", ExistingWorkPolicy.APPEND, recognizeRequest)
                .enqueue(recognizeRequest);

        workManager
                .getWorkInfoByIdLiveData(recognizeRequest.getId())
                .observeForever(workStatus -> {
                            Log.v(TAG, "Recognize voice worker status changed: " + workStatus.getState());
                            if (workStatus.getState() == WorkInfo.State.SUCCEEDED) {
                                String text = workStatus.getOutputData().getString(RecognizerWorker.RecognizedText);
                                if (text != null && !text.isEmpty()) {
                                    Log.d(TAG, "Recognized Text " + text);

                                    this.sendRequest(text);
                                }
                            } else if (workStatus.getState() == WorkInfo.State.FAILED) {
                                Log.e(TAG, "Recognize worker fail");
                                showWorkerErrorToast(workStatus, R.string.recognize_failed, RecognizerWorker.RecognizedError);
                            }
                        }
                );
    }

    public void saveCUID(String cuid) {
        executorService.execute(() -> {
            db.assistantDao().saveCUID(assistantId, cuid);
        });
    }

    @Override
    public void onChatInited(final String cuid) {
        saveCUID(cuid);

        executorService.execute(() -> {
            if (db.messageDao().getMessagesCount(assistantId) == 0) {
                Log.d(TAG, "Message history with assistant is empty, get READY event");
                networkApi.requestReadyChatEvent();
            }
        });
    }

    @Override
    public void onCUIDChanged(final String cuid) {
        saveCUID(cuid);
    }

    @Override
    public void onNewResponse(String response, int emotion) {
        Message message = getMessageFromAssist(response);
        message.setEmotionNumber(emotion);
        addMessage(message);
        voicingMessage(message);
        pendingMessages.add(message);
    }

    @Override
    public void onNetworkError(String errorText) {
        Toast.makeText(mContext, errorText, Toast.LENGTH_LONG).show();
    }
}
