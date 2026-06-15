package com.akro.agram.utils;

import com.akro.agram.models.TGDialog;
import com.akro.agram.models.TGMessage;
import com.akro.agram.models.TGStory;

import java.util.ArrayList;
import java.util.List;

/**
 * بيانات تجريبية لاختبار الـ UI قبل ربط tgnet
 * بعد ربط الـ JNI هتتشال وتتعوض ببيانات حقيقية
 */
public class MockData {

    public static List<TGDialog> getDialogs() {
        List<TGDialog> list = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000;

        TGDialog d1 = new TGDialog(1001, "أحمد يونس", "تمام جاهز", now - 300, 3, false);
        d1.hasStory = true;
        list.add(d1);

        TGDialog d2 = new TGDialog(1002, "مجموعة AKRO Devs", "شيل الـ boringssl وحطه يدوي", now - 900, 12, true);
        d2.isGroup = true;
        list.add(d2);

        list.add(new TGDialog(1003, "محمد كريم", "ايوه بكره", now - 3600, 0, false));

        TGDialog d4 = new TGDialog(1004, "قناة AKRO Tools", "إصدار جديد EKOTURBO v2.1", now - 7200, 5, false);
        d4.isChannel = true;
        list.add(d4);

        list.add(new TGDialog(1005, "سارة أحمد", "شكراً جزيلاً!", now - 86400, 0, false));
        list.add(new TGDialog(1006, "خالد عمر", "هتبعتلك الملف", now - 172800, 2, false));

        TGDialog d7 = new TGDialog(1007, "Android Devs Egypt", "أي حد جرب KernelSU؟", now - 259200, 88, true);
        d7.isGroup = true;
        list.add(d7);

        return list;
    }

    public static List<TGMessage> getMessages(long dialogId) {
        List<TGMessage> list = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000;

        list.add(new TGMessage(1, dialogId, "أهلاً!", now - 3600, false));
        list.add(new TGMessage(2, dialogId, "أهلاً وسهلاً 👋", now - 3500, true));
        list.add(new TGMessage(3, dialogId, "عامل إيه؟", now - 3400, false));
        list.add(new TGMessage(4, dialogId, "تمام الحمد لله، شغلان على مشروع", now - 3300, true));
        list.add(new TGMessage(5, dialogId, "مشروع إيه؟", now - 3200, false));
        list.add(new TGMessage(6, dialogId, "AkroGram - تليجرام client خاص بيا 😄", now - 3100, true));
        list.add(new TGMessage(7, dialogId, "جامد! بتستخدم إيه؟", now - 3000, false));
        list.add(new TGMessage(8, dialogId, "tgnet من سورس DrKLO مباشرة", now - 2900, true));

        TGMessage reply = new TGMessage(9, dialogId, "والله يجيب", now - 60, false);
        reply.replyToMsgId = 8;
        reply.replyText = "tgnet من سورس DrKLO مباشرة";
        list.add(reply);

        TGMessage sending = new TGMessage(10, dialogId, "ربنا يوفقك يا AKRO 🙏", now - 10, true);
        sending.sendState = TGMessage.STATE_READ;
        list.add(sending);

        return list;
    }

    public static List<TGStory> getStories() {
        List<TGStory> list = new ArrayList<>();
        long now = System.currentTimeMillis() / 1000;

        TGStory s1 = new TGStory();
        s1.id = 1; s1.peerId = 1001; s1.peerName = "أحمد";
        s1.date = now - 3600; s1.viewed = false;
        list.add(s1);

        TGStory s2 = new TGStory();
        s2.id = 2; s2.peerId = 1005; s2.peerName = "سارة";
        s2.date = now - 7200; s2.viewed = true;
        list.add(s2);

        TGStory s3 = new TGStory();
        s3.id = 3; s3.peerId = 1006; s3.peerName = "خالد";
        s3.date = now - 1800; s3.viewed = false;
        list.add(s3);

        return list;
    }
}
