package com.akro.agram.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.akro.agram.R;
import com.akro.agram.models.TGDialog;
import com.akro.agram.models.TGStory;
import com.akro.agram.network.TelegramController;
import com.akro.agram.ui.adapters.DialogsAdapter;
import com.akro.agram.ui.adapters.StoriesAdapter;
import com.akro.agram.utils.MockData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvDialogs, rvStories;
    private DialogsAdapter dialogsAdapter;
    private StoriesAdapter storiesAdapter;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private FloatingActionButton fabNewChat;
    private LinearLayout layoutStories;
    private BottomNavigationView bottomNav;
    private TextView tvTabChats, tvTabContacts, tvTabSettings;
    private View tabChats, tabContacts, tabSettings;

    private List<TGDialog> allDialogs = new ArrayList<>();
    private List<TGStory> stories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupStories();
        setupDialogs();
        setupSearch();
        setupBottomNav();
        loadData();
    }

    private void initViews() {
        rvDialogs    = findViewById(R.id.rvDialogs);
        rvStories    = findViewById(R.id.rvStories);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        etSearch     = findViewById(R.id.etSearch);
        fabNewChat   = findViewById(R.id.fabNewChat);
        layoutStories = findViewById(R.id.layoutStories);
        bottomNav    = findViewById(R.id.bottomNav);
    }

    private void setupStories() {
        storiesAdapter = new StoriesAdapter(stories, story -> {
            Intent intent = new Intent(this, StoryActivity.class);
            intent.putExtra("peer_id", story.peerId);
            intent.putExtra("peer_name", story.peerName);
            startActivity(intent);
        });
        rvStories.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storiesAdapter);
    }

    private void setupDialogs() {
        dialogsAdapter = new DialogsAdapter(allDialogs, dialog -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("dialog_id", dialog.id);
            intent.putExtra("dialog_title", dialog.title);
            intent.putExtra("is_group", dialog.isGroup);
            startActivity(intent);
        });
        rvDialogs.setLayoutManager(new LinearLayoutManager(this));
        rvDialogs.setAdapter(dialogsAdapter);

        swipeRefresh.setOnRefreshListener(() -> {
            loadData();
            swipeRefresh.setRefreshing(false);
        });

        fabNewChat.setOnClickListener(v -> {
            // TODO: فتح قائمة جهات الاتصال لبدء محادثة جديدة
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filterDialogs(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chats) {
                // already here
                return true;
            } else if (id == R.id.nav_contacts) {
                // TODO: contacts fragment
                return true;
            } else if (id == R.id.nav_settings) {
                // TODO: settings fragment
                return true;
            }
            return false;
        });
    }

    private void loadData() {
        // بيجيب البيانات من tgnet JNI
        // في الوقت الحالي بيستخدم mock data للتجربة
        allDialogs.clear();
        allDialogs.addAll(MockData.getDialogs());
        dialogsAdapter.notifyDataSetChanged();

        stories.clear();
        stories.addAll(MockData.getStories());
        storiesAdapter.notifyDataSetChanged();

        layoutStories.setVisibility(stories.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void filterDialogs(String query) {
        List<TGDialog> filtered = new ArrayList<>();
        if (query.isEmpty()) {
            filtered.addAll(allDialogs);
        } else {
            String q = query.toLowerCase();
            for (TGDialog d : allDialogs) {
                if (d.title.toLowerCase().contains(q) ||
                    (d.lastMessage != null && d.lastMessage.toLowerCase().contains(q))) {
                    filtered.add(d);
                }
            }
        }
        dialogsAdapter.updateList(filtered);
    }
}
