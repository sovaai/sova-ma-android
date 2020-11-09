package com.osmino.sova.api;

import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.osmino.sova.BuildConfig;
import com.osmino.sova.api.capi.ChatEvent;
import com.osmino.sova.api.capi.ChatInit;
import com.osmino.sova.api.capi.ChatRequest;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CAPI {
    String TAG = "CAPI";
    String BASE_API_URL = "https://biz.nanosemantics.ru/api/bat/nkd/json/";

    class ResultInf {
        @Expose
        @SerializedName("name")
        public String name;
    }

    class ResultChatInitResult {
        @Expose
        @SerializedName("cuid")
        public String cuid;

        @Expose
        @SerializedName("inf")
        public ResultInf inf;

        @Expose
        @SerializedName("token")
        public String token;

        @Expose
        @SerializedName("root")
        public String root;
    }

    class ResultChatInit {
        @Expose
        @SerializedName("result")
        public ResultChatInitResult result;

        @Expose
        @SerializedName("id")
        public int id;
    }

    @POST("Chat.init")
    Call<ResultChatInit> ChatInit(@Body ChatInit body);

    @POST("Chat.request")
    Call<ChatRequest.Result> ChatRequest(@Body ChatRequest body);

    @POST("Chat.event")
    Call<ChatRequest.Result> ChatEvent(@Body ChatEvent body);

    static CAPI create() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(message -> Log.v(TAG, message));
        if (BuildConfig.DEBUG) {
            logger.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logger.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                                .addInterceptor(logger)
                                .build();
        HttpUrl url = HttpUrl.parse(BASE_API_URL);
        if (url == null) {
            throw new IllegalArgumentException("Wrong api URL");
        }

        return new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(CAPI.class);
    }
}
