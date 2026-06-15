package com.akro.agram.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.akro.agram.R;
import com.akro.agram.network.TelegramController;

public class LoginActivity extends AppCompatActivity implements TelegramController.AuthListener {

    private LinearLayout layoutPhone, layoutCode, layoutPassword;
    private EditText etPhone, etCode, etPassword;
    private Button btnPhone, btnCode, btnPassword;
    private ProgressBar progressBar;
    private TextView tvTitle, tvSubtitle;

    private TelegramController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        controller = TelegramController.getInstance();
        controller.addAuthListener(this);

        layoutPhone    = findViewById(R.id.layoutPhone);
        layoutCode     = findViewById(R.id.layoutCode);
        layoutPassword = findViewById(R.id.layoutPassword);
        etPhone        = findViewById(R.id.etPhone);
        etCode         = findViewById(R.id.etCode);
        etPassword     = findViewById(R.id.etPassword);
        btnPhone       = findViewById(R.id.btnPhone);
        btnCode        = findViewById(R.id.btnCode);
        btnPassword    = findViewById(R.id.btnPassword);
        progressBar    = findViewById(R.id.progressBar);
        tvTitle        = findViewById(R.id.tvTitle);
        tvSubtitle     = findViewById(R.id.tvSubtitle);

        btnPhone.setOnClickListener(v -> submitPhone());
        btnCode.setOnClickListener(v -> submitCode());
        btnPassword.setOnClickListener(v -> submitPassword());

        showPhoneStep();
    }

    private void showPhoneStep() {
        tvTitle.setText("AkroGram");
        tvSubtitle.setText("أدخل رقم هاتفك بالكود الدولي");
        layoutPhone.setVisibility(View.VISIBLE);
        layoutCode.setVisibility(View.GONE);
        layoutPassword.setVisibility(View.GONE);
    }

    private void showCodeStep() {
        tvTitle.setText("كود التحقق");
        tvSubtitle.setText("أدخل الكود المرسل إلى " + controller.getCurrentPhone());
        layoutPhone.setVisibility(View.GONE);
        layoutCode.setVisibility(View.VISIBLE);
        layoutPassword.setVisibility(View.GONE);
    }

    private void showPasswordStep() {
        tvTitle.setText("كلمة المرور");
        tvSubtitle.setText("أدخل كلمة مرور التحقق بخطوتين");
        layoutPhone.setVisibility(View.GONE);
        layoutCode.setVisibility(View.GONE);
        layoutPassword.setVisibility(View.VISIBLE);
    }

    private void submitPhone() {
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("أدخل رقم الهاتف");
            return;
        }
        setLoading(true);
        controller.sendPhone(phone, this);
    }

    private void submitCode() {
        String code = etCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            etCode.setError("أدخل الكود");
            return;
        }
        setLoading(true);
        controller.sendCode(code, this);
    }

    private void submitPassword() {
        String pass = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(pass)) {
            etPassword.setError("أدخل كلمة المرور");
            return;
        }
        setLoading(true);
        controller.sendPassword(pass, this);
    }

    @Override
    public void onAuthStateChanged(int state) {
        setLoading(false);
        switch (state) {
            case TelegramController.AUTH_STATE_WAIT_PHONE:
                showPhoneStep();
                break;
            case TelegramController.AUTH_STATE_WAIT_CODE:
                showCodeStep();
                break;
            case TelegramController.AUTH_STATE_WAIT_PASSWORD:
                showPasswordStep();
                break;
            case TelegramController.AUTH_STATE_AUTHORIZED:
                goToMain();
                break;
        }
    }

    @Override
    public void onError(String error) {
        setLoading(false);
        Toast.makeText(this, error != null ? error : "حدث خطأ", Toast.LENGTH_LONG).show();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPhone.setEnabled(!loading);
        btnCode.setEnabled(!loading);
        btnPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.removeAuthListener(this);
    }
}
