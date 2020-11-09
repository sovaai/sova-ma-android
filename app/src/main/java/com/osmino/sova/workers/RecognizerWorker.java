package com.osmino.sova.workers;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

import com.osmino.sova.R;
import com.osmino.sova.api.ASR;

public class RecognizerWorker extends Worker {
    private final String TAG = "RecognizerWorker";

    public final static String audioFileNameKey = "audioFileName";
    public final static String RecognizedText = "text";
    public final static String RecognizedError = "error";

    private final ASR asr;
    private int mError = 0;

    public RecognizerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d(TAG, "RecognizerWorker created");

        asr = ASR.create();
    }

    @NonNull
    @Override
    public Result doWork() {
        String audioFileName = getInputData().getString(audioFileNameKey);
        if (audioFileName == null) {
            Log.e(TAG, "Wrong input data");
            return Result.failure();
        }

        String text = recognizeSpeech(audioFileName);

        File audioF = new File(audioFileName);
        if (audioF.exists()) {
            if (!audioF.delete()) {
                Log.e(TAG, "File not deleted " + audioFileName);
            }
        }

        if (text == null) {
            return Result.failure(new Data.Builder().putInt(RecognizedError, mError).build());
        }

        Data output = new Data.Builder()
                .putString(RecognizedText, text)
                .build();
        return Result.success(output);
    }

    private void setError(int errorStrId) {
        mError = errorStrId;
    }

    private String recognizeSpeech(String audioFileName) {
        Log.d(TAG, "Start recognize from file" + audioFileName);
        File file = new File(audioFileName);

        RequestBody model_type = RequestBody.Companion.create("ASR", MediaType.parse("text/plain"));
        RequestBody filename = RequestBody.Companion.create(file.getName(), MediaType.parse("text/plain"));
        RequestBody requestFile = RequestBody.Companion.create(file, MediaType.parse("multipart/form-data"));
        MultipartBody.Part audio_blob = MultipartBody.Part.createFormData("audio_blob", file.getName(), requestFile);

        try {
            Response<ASR.Result> response = asr.SpeechToText(model_type, filename, audio_blob).execute();
            if (response.code() == 200) {
                ASR.Result result = response.body();
                if (result == null) {
                    Log.e(TAG, "result is null");
                    setError(R.string.error_worker_empty_response);
                    return null;
                }
                if (result.r.length > 0 && result.r[0].response.length > 0) {
                    String text =result.r[0].response[0].text;
                    Log.d(TAG, "Text: " + text);
                    return text;
                } else {
                    Log.e(TAG, "Empty response from server");
                    setError(R.string.error_worker_empty_response);
                }
            } else {
                Log.e(TAG, "Wrong response code " + response.code());
                setError(R.string.error_worker_wrong_response);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Response timeout");
            setError(R.string.error_worker_timeout);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host", e);
            setError(R.string.error_worker_cannot_connect);
        }
        catch (Exception e) {
            Log.e(TAG, "Exception in ASR api call", e);
            setError(R.string.error_worker_system_error);
        }
        Log.e(TAG, "Something wrong");
        return null;
    }
}
