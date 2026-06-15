package com.akro.agram.network.auth;

import com.google.gson.annotations.SerializedName;

public class AuthSendCodeResponse {

    @SerializedName("_")
    public String type;                  // "auth.sentCode"

    @SerializedName("type")
    public SentCodeType sentCodeType;

    @SerializedName("phone_code_hash")
    public String phoneCodeHash;         // ← مهم جداً — بيتبعت مع signIn

    @SerializedName("next_type")
    public SentCodeType nextType;

    @SerializedName("timeout")
    public int timeout;

    // error fields
    @SerializedName("error_code")
    public int errorCode;

    @SerializedName("error_message")
    public String errorMessage;

    public boolean isSuccess() {
        return "auth.sentCode".equals(type) && phoneCodeHash != null;
    }

    public static class SentCodeType {
        @SerializedName("_")
        public String type;   // "auth.sentCodeTypeSms", "auth.sentCodeTypeApp", etc.

        @SerializedName("length")
        public int length;
    }
}
