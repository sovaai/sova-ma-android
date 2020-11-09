package com.osmino.sova.workers;

import android.content.Context;
import android.text.Html;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

import com.osmino.sova.R;
import com.osmino.sova.api.TTS;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;


public class VoicingWorker extends Worker {
    static final String TAG = "VoicingWorker";

    private final TTS tts;
    public final static String textKey = "text";
    public final static String VoicingAudio = "audio";
    public final static String VoicingError = "error";

    private int mError = 0;

    public VoicingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        tts = TTS.create();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: start");

        String text = getInputData().getString(textKey);
        if (text == null) {
            Log.e(TAG, "Wrong input data");
            return Result.failure();
        }





        byte[] audio = TextToSpeech(text);
        Log.d(TAG, "doWork: end");
        if (audio == null) {
            return Result.failure(new Data.Builder().putInt(VoicingError, mError).build());
        }

        File tempAudioFile = null;
        try {
            tempAudioFile = File.createTempFile("response_audio", "wav", getApplicationContext().getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempAudioFile);
            fos.write(audio);
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error in write temp file", e);
            return Result.failure(new Data.Builder().putInt(VoicingError, R.string.error_worker_system_error).build());
        }

        Data output = new Data.Builder()
                .putString(VoicingAudio, tempAudioFile.getAbsolutePath())
                .build();

        return Result.success(output);
    }

    public String stripHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public byte[] TextToSpeech(String text) {
        text = stripHtml(text);
        
        RequestBody voice = RequestBody.Companion.create("Belenkaya", MediaType.parse("text/plain"));
        RequestBody text_body = RequestBody.Companion.create(text, MediaType.parse("text/plain"));
        RequestBody options = RequestBody.Companion.create("", MediaType.parse("text/plain"));

        try {
            Response<TTS.MainResult> response = tts.TextToSpeech(voice, text_body, options).execute();
            if (response.code() == 200) {
                TTS.MainResult result = response.body();
                if (result == null || result.response == null || result.response.length == 0) {
                    Log.e(TAG, "Result is empty");
                    mError = R.string.error_worker_empty_response;
                } else {
                    return Base64.decode(result.response[0].response_audio, Base64.DEFAULT);
                }
            } else {
                Log.e(TAG, "Unexpected response code: " + response.code());
                mError = R.string.error_worker_wrong_response;
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Response timeout");
            mError = R.string.error_worker_timeout;
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host", e);
            mError = R.string.error_worker_cannot_connect;
        } catch (IOException e) {
            Log.e(TAG, "Exception", e);
            mError = R.string.error_worker_system_error;
        }
        return null;
    }
}
