package com.osmino.sova.api;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface TTS {
    String TAG = "TTS";
    String TTS_API_URL = "https://tts.ashmanov.org/";

    class Response {
        @Expose
        @SerializedName("name")
        public String name;

        @Expose
        @SerializedName("time")
        public Float time;

        @Expose
        @SerializedName("response_audio")
        public String response_audio;

        @Expose
        @SerializedName("response_audio_url")
        public String response_audio_url;
    }

    class MainResult {
        @Expose
        @SerializedName("response_code")
        public Integer response_code;

        @Expose
        @SerializedName("response")
        public TTS.Response[] response;
    }

    @Multipart
    @Headers("Authorization: Basic YW5uOjVDdWlIT0NTMlpRMQ==")
    @POST("tts")
    Call<MainResult> TextToSpeech(@Part("voice") RequestBody voice,
                              @Part("text") RequestBody text,
                              @Part("options") RequestBody options);

    static TTS create() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(message -> Log.v(TAG, message));
        logger.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logger)
                .build();
        HttpUrl url = HttpUrl.parse(TTS_API_URL);
        if (url == null) {
            throw new IllegalArgumentException("Wrong api URL");
        }

        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TTS.class);
    }
}
