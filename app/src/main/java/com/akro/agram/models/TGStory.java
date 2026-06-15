package com.akro.agram.models;

public class TGStory {
    public long id;
    public long peerId;
    public String peerName;
    public String peerAvatarUrl;
    public int mediaType; // 0=photo, 1=video
    public String mediaUrl;
    public String caption;
    public long date;
    public long expireDate;
    public boolean viewed;
    public int views;
    public boolean pinned;
    public boolean isPrivate;

    public TGStory() {}
}
