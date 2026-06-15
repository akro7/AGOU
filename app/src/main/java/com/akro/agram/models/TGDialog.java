package com.akro.agram.models;

public class TGDialog {
    public long id;
    public String title;
    public String lastMessage;
    public long lastMessageDate;
    public int unreadCount;
    public String avatarUrl;
    public boolean isGroup;
    public boolean isChannel;
    public boolean isBot;
    public boolean isPinned;
    public boolean isMuted;
    public boolean hasStory;       // لو عنده قصة
    public boolean storyViewed;    // اتشافت القصة ولا لأ
    public int type; // 0=user, 1=group, 2=channel

    public TGDialog() {}

    public TGDialog(long id, String title, String lastMessage,
                    long date, int unread, boolean isGroup) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.lastMessageDate = date;
        this.unreadCount = unread;
        this.isGroup = isGroup;
    }
}
