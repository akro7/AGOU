package com.akro.agram.network.auth;

import com.google.gson.annotations.SerializedName;

public class AuthSignInResponse {

    @SerializedName("_")
    public String type;   // "auth.authorization" أو "auth.authorizationSignUpRequired"

    @SerializedName("user")
    public TGUser user;

    // error
    @SerializedName("error_code")
    public int errorCode;

    @SerializedName("error_message")
    public String errorMessage;

    public boolean isAuthorized() {
        return "auth.authorization".equals(type) && user != null;
    }

    public boolean needsSignUp() {
        return "auth.authorizationSignUpRequired".equals(type);
    }

    public static class TGUser {
        @SerializedName("id")
        public long id;

        @SerializedName("first_name")
        public String firstName;

        @SerializedName("last_name")
        public String lastName;

        @SerializedName("username")
        public String username;

        @SerializedName("phone")
        public String phone;
    }
}
