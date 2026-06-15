package com.akro.agram.ui.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.akro.agram.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class StoryActivity extends AppCompatActivity {

    private static final int REQ_WRITE_STORAGE = 101;
    private static final int STORY_DURATION    = 5000; // 5 ثواني

    // Views
    private ImageView ivStory, ivAvatar, ivClose, ivMore;
    private TextView  tvName, tvTime, tvCaption, tvStealthBadge;
    private ProgressBar progressStory;
    private View btnReply;

    // State
    private String peerName;
    private long   peerId;
    private boolean stealthMode = false;   // إخفاء مشاهدة القصة
    private boolean isPaused    = false;
    private long    pausedAt    = 0;
    private long    remainingMs = STORY_DURATION;

    private Handler  handler = new Handler();
    private Runnable autoCloseRunnable;
    private Thread   progressThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        peerName    = getIntent().getStringExtra("peer_name");
        peerId      = getIntent().getLongExtra("peer_id", 0);
        stealthMode = getIntent().getBooleanExtra("stealth_mode", false);

        initViews();
        loadStory();
        startAutoClose(STORY_DURATION);

        // إذا الستيلث مفعّل من البداية نظهر البادج
        if (stealthMode) applyStealthMode(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void initViews() {
        ivStory        = findViewById(R.id.ivStory);
        ivAvatar       = findViewById(R.id.ivAvatar);
        ivClose        = findViewById(R.id.ivClose);
        ivMore         = findViewById(R.id.ivMore);          // ⋮ زرار القائمة
        tvName         = findViewById(R.id.tvName);
        tvTime         = findViewById(R.id.tvTime);
        tvCaption      = findViewById(R.id.tvCaption);
        tvStealthBadge = findViewById(R.id.tvStealthBadge);  // شارة الستيلث
        progressStory  = findViewById(R.id.progressStory);
        btnReply       = findViewById(R.id.btnReply);

        // زرار الإغلاق
        ivClose.setOnClickListener(v -> finish());

        // ⋮ قائمة الخيارات
        ivMore.setOnClickListener(this::showStoryMenu);

        // لمس يمين = التالي، يسار = السابق؛ مطوّل = إيقاف مؤقت
        ivStory.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pauseStory();
                    return true;
                case MotionEvent.ACTION_UP:
                    float x    = event.getX();
                    float half = v.getWidth() / 2f;
                    resumeStory();
                    if (x > half) finish(); // التالي
                    return true;
            }
            return false;
        });
    }

    // ─── قائمة الخيارات ⋮ ────────────────────────────────────────────────────
    private void showStoryMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, stealthMode
                ? "✅ إخفاء المشاهدة مفعّل"
                : "🔕 تفعيل إخفاء المشاهدة");
        popup.getMenu().add(0, 2, 1, "💾 حفظ القصة");
        popup.getMenu().add(0, 3, 2, "⏩ تخطي القصة");
        popup.getMenu().add(0, 4, 3, "🚫 كتم هذا الشخص");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: toggleStealthMode(); return true;
                case 2: saveStory();         return true;
                case 3: finish();            return true;
                case 4: muteUser();          return true;
            }
            return false;
        });
        popup.show();
    }

    // ─── إخفاء المشاهدة (Stealth Mode) ──────────────────────────────────────
    private void toggleStealthMode() {
        stealthMode = !stealthMode;
        applyStealthMode(stealthMode);
    }

    private void applyStealthMode(boolean enabled) {
        tvStealthBadge.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            Toast.makeText(this,
                "🔕 إخفاء المشاهدة مفعّل — لن يعرف صاحب القصة أنك شاهدتها",
                Toast.LENGTH_SHORT).show();
            // TODO: عند ربط tgnet — لا ترسل updateReadStories لهذا الـ peer
            // TelegramController.getInstance().markStoryAsReadSilently(peerId);
        } else {
            Toast.makeText(this, "إخفاء المشاهدة أُلغي", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── حفظ القصة ───────────────────────────────────────────────────────────
    private void saveStory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_WRITE_STORAGE);
                return;
            }
        }
        doSave();
    }

    private void doSave() {
        // حالياً نحفظ الـ screenshot من الـ ImageView
        // عند ربط tgnet: احفظ الميديا الحقيقية (صورة أو فيديو)
        ivStory.setDrawingCacheEnabled(true);
        Bitmap bmp = Bitmap.createBitmap(ivStory.getDrawingCache());
        ivStory.setDrawingCacheEnabled(false);

        if (bmp == null) {
            Toast.makeText(this, "لا توجد صورة لحفظها حالياً", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            boolean ok = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Images.Media.DISPLAY_NAME,
                            "story_" + System.currentTimeMillis() + ".jpg");
                    cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    cv.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/AkroGram");
                    Uri uri = getContentResolver()
                            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out);
                            ok = true;
                        }
                    }
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "AkroGram");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, "story_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out);
                        ok = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final boolean saved = ok;
            runOnUiThread(() -> Toast.makeText(this,
                    saved ? "✅ تم حفظ القصة في الصور" : "❌ فشل الحفظ",
                    Toast.LENGTH_SHORT).show());
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_WRITE_STORAGE
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            doSave();
        }
    }

    // ─── كتم المستخدم ────────────────────────────────────────────────────────
    private void muteUser() {
        new AlertDialog.Builder(this)
            .setTitle("كتم " + peerName)
            .setMessage("هل تريد إخفاء قصص " + peerName + " مستقبلاً؟")
            .setPositiveButton("نعم، أخفِ", (d, w) -> {
                // TODO: اربط بـ TelegramController.muteStories(peerId)
                Toast.makeText(this, "تم كتم قصص " + peerName, Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    // ─── تحميل القصة ─────────────────────────────────────────────────────────
    private void loadStory() {
        tvName.setText(peerName != null ? peerName : "");
        tvTime.setText("الآن");
        progressStory.setProgress(0);
        // TODO: Glide.with(this).load(story.mediaUrl).into(ivStory);
    }

    // ─── التحكم في التقدم ────────────────────────────────────────────────────
    private void startAutoClose(long durationMs) {
        remainingMs = durationMs;
        progressStory.setMax(STORY_DURATION);

        autoCloseRunnable = this::finish;
        handler.postDelayed(autoCloseRunnable, remainingMs);

        startProgressThread(remainingMs);
    }

    private void startProgressThread(long totalMs) {
        int startProgress = (int)(STORY_DURATION - totalMs);
        progressThread = new Thread(() -> {
            for (int i = startProgress; i <= STORY_DURATION; i += 50) {
                if (isPaused || isFinishing()) break;
                final int p = i;
                runOnUiThread(() -> progressStory.setProgress(p));
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
        });
        progressThread.start();
    }

    private void pauseStory() {
        if (isPaused) return;
        isPaused  = true;
        pausedAt  = System.currentTimeMillis();
        handler.removeCallbacks(autoCloseRunnable);
        if (progressThread != null) progressThread.interrupt();
    }

    private void resumeStory() {
        if (!isPaused) return;
        isPaused     = false;
        long elapsed = System.currentTimeMillis() - pausedAt;
        remainingMs  = Math.max(0, remainingMs - elapsed);
        handler.postDelayed(autoCloseRunnable, remainingMs);
        startProgressThread((long)(STORY_DURATION - progressStory.getProgress()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoCloseRunnable);
        if (progressThread != null) progressThread.interrupt();

        // إذا الستيلث مش مفعّل — سجّل المشاهدة (عند ربط tgnet)
        if (!stealthMode) {
            // TelegramController.getInstance().markStoryViewed(peerId, storyId);
        }
    }
}
