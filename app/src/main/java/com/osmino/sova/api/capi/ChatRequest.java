package com.osmino.sova.api.capi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @Expose
    @SerializedName("cuid")
    public String cuid;

    @Expose
    @SerializedName("text")
    public String text;

    public class Result {
        @Expose
        @SerializedName("result")
        public RequestResult result;

        @Expose
        @SerializedName("id")
        public int id;
    }
}
