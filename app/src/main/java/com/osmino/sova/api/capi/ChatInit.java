package com.osmino.sova.api.capi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatInit {
    @Expose
    @SerializedName("uuid")
    public String uuid;

    @Expose
    @SerializedName("cuid")
    public String cuid;
}
