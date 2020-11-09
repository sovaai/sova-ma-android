package com.osmino.sova.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "assistants")
public class Assistant {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "uri")
    private String apiURI;

    @ColumnInfo(name = "token")
    private String token;

    @ColumnInfo(name = "listen")
    private boolean listen;

    @ColumnInfo(name = "cuid")
    private String cuid;

    @ColumnInfo(name = "has_avatar")
    private boolean hasAvatar;

    public Assistant(){
    }

    public Assistant(String name, String uri, String token, boolean listen, boolean hasAvatar) {
        this.name = name;
        this.apiURI = uri;
        this.token = token;
        this.listen = listen;
        this.hasAvatar = hasAvatar;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getApiURI() {
        return apiURI;
    }
    public void setApiURI(String apiURI) {
        this.apiURI = apiURI;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public boolean getListen(){
        return listen;
    }
    public void setListen(boolean listen) {
        this.listen = listen;
    }

    public String getCuid(){
        return cuid;
    }
    public void setCuid(String cuid) {
        this.cuid = cuid;
    }

    public boolean getHasAvatar() {
        return hasAvatar;
    }
    public void setHasAvatar(final boolean hasAvatar) {
        this.hasAvatar = hasAvatar;
    }
}
