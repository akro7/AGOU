package com.akro.agram.network.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * يدير الـ auth flow الكامل مع Telegram عبر HTTPS.
 *
 * الـ flow:
 *   1. sendCode(phone)      → يجيب phoneCodeHash
 *   2. signIn(code)         → يجيب auth.authorization أو auth.authorizationSignUpRequired
 *   3. (اختياري) signUp()   → لو مستخدم جديد
 */
public class TelegramAuthService {

    private static final String TAG = "AkroGram/Auth";

    // Telegram MTProto HTTPS endpoint
    // استخدم DC1 كـ default — بيتغير لو السيرفر بعت redirect
    private static final String BASE_URL = "https://149.154.167.50/";
    private static final int    DC1_PORT = 443;

    private final TelegramAuthApi api;
    private final Context context;

    private final int    apiId;
    private final String apiHash;

    private String currentPhone;
    private String phoneCodeHash;

    public interface AuthCallback {
        void onCodeSent(String phone, String codeType, int codeLength);
        void onAuthorized(long userId, String firstName, String username);
        void onSignUpRequired();
        void onError(int code, String message);
    }

    public TelegramAuthService(Context context, int apiId, String apiHash) {
        this.context = context.getApplicationContext();
        this.apiId   = apiId;
        this.apiHash  = apiHash;

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            // Telegram يستخدم self-signed على بعض الـ IPs — accept all للتطوير
            .build();

        Gson gson = new GsonBuilder()
            .setLenient()
            .create();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        api = retrofit.create(TelegramAuthApi.class);
    }

    // ─── Step 1: إرسال رقم الهاتف ────────────────────────────────────────────
    public void sendCode(String phone, AuthCallback callback) {
        this.currentPhone = phone;
        Log.i(TAG, "sendCode: " + phone);

        String settings = "{\"_\":\"codeSettings\"}";

        api.sendCode(phone, apiId, apiHash, settings).enqueue(new Callback<AuthSendCodeResponse>() {
            @Override
            public void onResponse(Call<AuthSendCodeResponse> call, Response<AuthSendCodeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSendCodeResponse body = response.body();
                    if (body.isSuccess()) {
                        phoneCodeHash = body.phoneCodeHash;
                        savePhoneCodeHash(phoneCodeHash);
                        Log.i(TAG, "✅ sendCode success, hash=" + phoneCodeHash);

                        String codeType   = body.sentCodeType != null ? body.sentCodeType.type : "unknown";
                        int    codeLength = body.sentCodeType != null ? body.sentCodeType.length : 5;
                        callback.onCodeSent(phone, codeType, codeLength);
                    } else {
                        Log.e(TAG, "sendCode error: " + body.errorCode + " " + body.errorMessage);
                        callback.onError(body.errorCode, body.errorMessage);
                    }
                } else {
                    handleHttpError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<AuthSendCodeResponse> call, Throwable t) {
                Log.e(TAG, "sendCode network error: " + t.getMessage());
                callback.onError(-1, "خطأ في الشبكة: " + t.getMessage());
            }
        });
    }

    // ─── Step 2: تأكيد الكود ─────────────────────────────────────────────────
    public void signIn(String code, AuthCallback callback) {
        if (currentPhone == null || phoneCodeHash == null) {
            callback.onError(-1, "لم يتم إرسال الكود بعد");
            return;
        }
        Log.i(TAG, "signIn: code=" + code);

        api.signIn(currentPhone, phoneCodeHash, code).enqueue(new Callback<AuthSignInResponse>() {
            @Override
            public void onResponse(Call<AuthSignInResponse> call, Response<AuthSignInResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSignInResponse body = response.body();
                    if (body.isAuthorized()) {
                        long userId = body.user.id;
                        saveUserId(userId);
                        Log.i(TAG, "✅ signIn success, userId=" + userId);
                        callback.onAuthorized(userId,
                            body.user.firstName != null ? body.user.firstName : "",
                            body.user.username  != null ? body.user.username  : "");
                    } else if (body.needsSignUp()) {
                        Log.i(TAG, "signIn → needsSignUp");
                        callback.onSignUpRequired();
                    } else {
                        Log.e(TAG, "signIn error: " + body.errorCode + " " + body.errorMessage);
                        callback.onError(body.errorCode, body.errorMessage);
                    }
                } else {
                    handleHttpError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<AuthSignInResponse> call, Throwable t) {
                Log.e(TAG, "signIn network error: " + t.getMessage());
                callback.onError(-1, "خطأ في الشبكة: " + t.getMessage());
            }
        });
    }

    // ─── Step 3 (اختياري): تسجيل مستخدم جديد ────────────────────────────────
    public void signUp(String firstName, String lastName, AuthCallback callback) {
        if (currentPhone == null || phoneCodeHash == null) {
            callback.onError(-1, "بيانات غير مكتملة");
            return;
        }

        api.signUp(currentPhone, phoneCodeHash, firstName, lastName).enqueue(new Callback<AuthSignInResponse>() {
            @Override
            public void onResponse(Call<AuthSignInResponse> call, Response<AuthSignInResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthSignInResponse body = response.body();
                    if (body.isAuthorized()) {
                        saveUserId(body.user.id);
                        callback.onAuthorized(body.user.id,
                            body.user.firstName != null ? body.user.firstName : firstName,
                            body.user.username  != null ? body.user.username  : "");
                    } else {
                        callback.onError(body.errorCode, body.errorMessage);
                    }
                } else {
                    handleHttpError(response, callback);
                }
            }

            @Override
            public void onFailure(Call<AuthSignInResponse> call, Throwable t) {
                callback.onError(-1, "خطأ في الشبكة: " + t.getMessage());
            }
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private <T> void handleHttpError(Response<T> response, AuthCallback callback) {
        String errBody = "";
        try {
            if (response.errorBody() != null) errBody = response.errorBody().string();
        } catch (IOException ignored) {}
        Log.e(TAG, "HTTP error " + response.code() + ": " + errBody);

        // parse Telegram error message من الـ body
        String msg = "خطأ من السيرفر (" + response.code() + ")";
        if (errBody.contains("PHONE_NUMBER_INVALID"))  msg = "رقم الهاتف غير صحيح";
        else if (errBody.contains("PHONE_CODE_INVALID")) msg = "الكود غير صحيح";
        else if (errBody.contains("PHONE_CODE_EXPIRED")) msg = "انتهت صلاحية الكود";
        else if (errBody.contains("FLOOD_WAIT"))         msg = "كثرة المحاولات — انتظر قليلاً";

        callback.onError(response.code(), msg);
    }

    private void savePhoneCodeHash(String hash) {
        context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
               .edit().putString("phone_code_hash", hash).apply();
    }

    private void saveUserId(long id) {
        context.getSharedPreferences("akrogram", Context.MODE_PRIVATE)
               .edit().putLong("user_id", id).apply();
    }

    public void restoreState() {
        SharedPreferences prefs = context.getSharedPreferences("akrogram", Context.MODE_PRIVATE);
        phoneCodeHash = prefs.getString("phone_code_hash", null);
    }
}
