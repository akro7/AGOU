package com.akro.agram.models;

public class TGMessage {
    public long id;
    public long dialogId;
    public long fromId;
    public String fromName;
    public String text;
    public long date;
    public boolean isOut;       // رسالة أنا بعتها
    public boolean isRead;
    public int mediaType;       // 0=text, 1=photo, 2=video, 3=file, 4=sticker, 5=voice
    public String mediaUrl;
    public String mediaThumbUrl;
    public long mediaSize;
    public String mediaName;
    public boolean isForwarded;
    public String forwardFromName;
    public long replyToMsgId;
    public String replyText;
    public int views;           // للقنوات

    // Send states
    public static final int STATE_SENDING = 0;
    public static final int STATE_SENT    = 1;
    public static final int STATE_READ    = 2;
    public static final int STATE_FAILED  = 3;
    public int sendState = STATE_SENT;

    public TGMessage() {}

    public TGMessage(long id, long dialogId, String text, long date, boolean isOut) {
        this.id = id;
        this.dialogId = dialogId;
        this.text = text;
        this.date = date;
        this.isOut = isOut;
    }

    public boolean isTextOnly() { return mediaType == 0; }
}
