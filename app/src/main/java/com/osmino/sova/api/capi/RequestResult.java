package com.osmino.sova.api.capi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RequestResult {
    static public class Animation {
        @Expose
        @SerializedName("type")
        public int type;

        @Expose
        @SerializedName("duration")
        public int duration;
    }

    public class Text {
        @Expose
        public String value;

        @Expose
        public int delay;

        @Expose
        public int status;

        @Expose
        public boolean showRate;
    }

    @Expose
    @SerializedName("text")
    public RequestResult.Text text;

    @Expose
    @SerializedName("rubric")
    public String rubric;

    @Expose
    @SerializedName("token")
    public String token;

    @Expose
    @SerializedName("id")
    public String id;

    @Expose
    @SerializedName("cuid")
    public String cuid;

    @Expose
    @SerializedName("animation")
    public Animation animation;
}
