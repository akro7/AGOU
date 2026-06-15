package com.akro.agram.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akro.agram.R;
import com.akro.agram.models.TGMessage;
import com.akro.agram.network.TelegramController;
import com.akro.agram.ui.adapters.MessagesAdapter;
import com.akro.agram.utils.MockData;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, btnAttach, btnEmoji, btnBack;
    private TextView tvTitle, tvSubtitle;
    private View btnCall, btnMore;

    private MessagesAdapter messagesAdapter;
    private List<TGMessage> messages = new ArrayList<>();

    private long dialogId;
    private String dialogTitle;
    private boolean isGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dialogId    = getIntent().getLongExtra("dialog_id", 0);
        dialogTitle = getIntent().getStringExtra("dialog_title");
        isGroup     = getIntent().getBooleanExtra("is_group", false);

        initViews();
        setupToolbar();
        setupMessages();
        loadMessages();
    }

    private void initViews() {
        rvMessages  = findViewById(R.id.rvMessages);
        etMessage   = findViewById(R.id.etMessage);
        btnSend     = findViewById(R.id.btnSend);
        btnAttach   = findViewById(R.id.btnAttach);
        btnBack     = findViewById(R.id.btnBack);
        tvTitle     = findViewById(R.id.tvTitle);
        tvSubtitle  = findViewById(R.id.tvSubtitle);
    }

    private void setupToolbar() {
        tvTitle.setText(dialogTitle != null ? dialogTitle : "محادثة");
        tvSubtitle.setText(isGroup ? "مجموعة" : "آخر ظهور مؤخراً");
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupMessages() {
        messagesAdapter = new MessagesAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messagesAdapter);

        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnSend.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadMessages() {
        messages.clear();
        messages.addAll(MockData.getMessages(dialogId));
        messagesAdapter.notifyDataSetChanged();
        if (!messages.isEmpty()) {
            rvMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        // إضافة الرسالة فوراً في الـ UI (optimistic)
        TGMessage msg = new TGMessage(
            System.currentTimeMillis(),
            dialogId,
            text,
            System.currentTimeMillis() / 1000,
            true
        );
        msg.sendState = TGMessage.STATE_SENDING;
        messages.add(msg);
        messagesAdapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
        etMessage.setText("");

        // TODO: إرسال عبر tgnet JNI
        // TelegramController.getInstance().sendMessage(dialogId, text, result -> {
        //     msg.sendState = result ? STATE_SENT : STATE_FAILED;
        //     messagesAdapter.notifyItemChanged(messages.indexOf(msg));
        // });
    }
}
