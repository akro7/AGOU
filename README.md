# AkroGram — Telegram Client by AKRO

تطبيق تليجرام مخصص مبني على سورس DrKLO/Telegram مع مميزات حصرية.

---

## هيكل المشروع

```
AkroGram/
├── .github/workflows/build.yml   ← GitHub Actions (Debug + Release + Lint)
├── app/
│   └── src/main/java/com/akro/agram/
│       ├── network/TelegramController.java  ← JNI bridge + stealth + last-seen
│       ├── ui/activities/StoryActivity.java ← قصص fullscreen مع مميزات موسّعة
│       └── ...
└── TMessagesProj/
    └── jni/CMakeLists.txt  ← إصلاح: atomic + sqlite flags + AKROGRAM_STEALTH
```

---

## المميزات الجديدة

### 📖 شريط القصص — مميزات جديدة
| الميزة | الوصف |
|--------|-------|
| **🔕 إخفاء المشاهدة** | شاهد القصة بدون إشعار صاحبها (Stealth Mode) |
| **💾 حفظ القصة** | حفظ الصورة/الفيديو في ألبوم AkroGram |
| **⏸ إيقاف مؤقت** | اضغط مطوّل على القصة لإيقافها مؤقتاً |
| **⏩ تخطي** | تخطي القصة من قائمة الخيارات |
| **🚫 كتم** | إخفاء قصص شخص معين مستقبلاً |
| **شارة الستيلث** | تظهر علامة 🔕 مخفي في الشريط العلوي عند التفعيل |

### 👁 آخر ظهور المختفين
- `TelegramController.fetchLastSeen(userId, callback)` يحاول استرداد آخر ظهور حقيقي
- يستغل `updateUserStatus` الذي يصل حتى عند privacy=nobody في بعض الحالات
- نتيجة الاسترداد تُعرض في شاشة الشات بدل "آخر ظهور مخفي"

---

## إعداد المشروع

### 1. الملفات النيتيف (يدوي)

من `Telegram-master.zip` استخرج في `TMessagesProj/jni/`:
```
tgnet/       sqlite/       tde2e/
TgNetWrapper.cpp    jni.c
```
من الريبو الأصلي أضف مجلد `boringssl/`.

### 2. API Credentials

روح [my.telegram.org](https://my.telegram.org) → API development tools

**للبيلد المحلي** — في `TelegramController.java`:
```java
"YOUR_API_ID"   →  رقمك
"YOUR_API_HASH" →  الهاش
```

**للبيلد عبر GitHub Actions** — أضف Secrets:
```
TG_API_ID     = رقمك
TG_API_HASH   = الهاش
```

### 3. GitHub Actions — Secrets المطلوبة

| Secret | الوصف |
|--------|-------|
| `TG_API_ID` | رقم الـ API من my.telegram.org |
| `TG_API_HASH` | الهاش |
| `KEYSTORE_BASE64` | ملف الـ keystore بـ base64 (للـ release) |
| `KEY_ALIAS` | اسم الـ key |
| `KEY_PASSWORD` | كلمة مرور الـ key |
| `STORE_PASSWORD` | كلمة مرور الـ keystore |

### 4. افتح في Android Studio

```
File → Open → مجلد AkroGram
Build → Make Project
```

---

## Credits
**Ahmed Younis / AKRO**
Based on DrKLO/Telegram (GPL-2.0)
