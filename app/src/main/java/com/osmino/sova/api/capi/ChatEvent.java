package com.osmino.sova.api.capi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatEvent {
    @Expose
    @SerializedName("cuid")
    public String cuid;

    @Expose
    @SerializedName("euid")
    public String euid;
}
