package com.akro.agram.network.auth;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Telegram Bot API / MTProto HTTPS endpoints للـ auth flow
 */
public interface TelegramAuthApi {

    // إرسال رقم الهاتف وطلب كود التحقق
    @FormUrlEncoded
    @POST("auth.sendCode")
    Call<AuthSendCodeResponse> sendCode(
        @Field("phone_number")  String phoneNumber,
        @Field("api_id")        int apiId,
        @Field("api_hash")      String apiHash,
        @Field("settings")      String settings  // JSON: {"_":"codeSettings"}
    );

    // تأكيد الكود
    @FormUrlEncoded
    @POST("auth.signIn")
    Call<AuthSignInResponse> signIn(
        @Field("phone_number")      String phoneNumber,
        @Field("phone_code_hash")   String phoneCodeHash,
        @Field("phone_code")        String phoneCode
    );

    // تسجيل مستخدم جديد
    @FormUrlEncoded
    @POST("auth.signUp")
    Call<AuthSignInResponse> signUp(
        @Field("phone_number")      String phoneNumber,
        @Field("phone_code_hash")   String phoneCodeHash,
        @Field("first_name")        String firstName,
        @Field("last_name")         String lastName
    );

    // تسجيل الخروج
    @FormUrlEncoded
    @POST("auth.logOut")
    Call<Void> logOut();
}
