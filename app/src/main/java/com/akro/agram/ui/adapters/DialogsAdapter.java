package com.akro.agram.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akro.agram.R;
import com.akro.agram.models.TGDialog;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DialogsAdapter extends RecyclerView.Adapter<DialogsAdapter.VH> {

    public interface OnDialogClick { void onClick(TGDialog dialog); }

    private List<TGDialog> items;
    private final OnDialogClick listener;

    // ألوان avatars تلقائية
    private static final int[] AVATAR_COLORS = {
        0xFF2CA5E0, 0xFF5EC245, 0xFFFF6B6B, 0xFFFFD93D,
        0xFF6C5CE7, 0xFFFD79A8, 0xFF00CEC9, 0xFFE17055
    };

    public DialogsAdapter(List<TGDialog> items, OnDialogClick listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateList(List<TGDialog> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_dialog, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TGDialog d = items.get(position);
        h.tvName.setText(d.title);
        h.tvMessage.setText(d.lastMessage != null ? d.lastMessage : "");
        h.tvTime.setText(formatTime(d.lastMessageDate));

        if (d.unreadCount > 0) {
            h.tvUnread.setVisibility(View.VISIBLE);
            h.tvUnread.setText(d.unreadCount > 99 ? "99+" : String.valueOf(d.unreadCount));
        } else {
            h.tvUnread.setVisibility(View.GONE);
        }

        // Story ring
        if (d.hasStory) {
            h.storyRing.setVisibility(View.VISIBLE);
            h.storyRing.setAlpha(d.storyViewed ? 0.4f : 1.0f);
        } else {
            h.storyRing.setVisibility(View.GONE);
        }

        // Avatar
        int color = AVATAR_COLORS[(int)(Math.abs(d.id) % AVATAR_COLORS.length)];
        if (d.avatarUrl != null && !d.avatarUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                 .load(d.avatarUrl)
                 .circleCrop()
                 .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageDrawable(null);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            h.ivAvatar.setBackground(circle);
            // Initial letter
            h.tvAvatarLetter.setVisibility(View.VISIBLE);
            h.tvAvatarLetter.setText(d.title.isEmpty() ? "?" :
                String.valueOf(d.title.charAt(0)).toUpperCase());
        }

        // Type icon
        if (d.isChannel) h.ivType.setImageResource(R.drawable.ic_channel);
        else if (d.isGroup) h.ivType.setImageResource(R.drawable.ic_group);
        else if (d.isBot) h.ivType.setImageResource(R.drawable.ic_bot);
        else h.ivType.setImageDrawable(null);

        h.itemView.setOnClickListener(v -> listener.onClick(d));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "";
        long now = System.currentTimeMillis() / 1000;
        long diff = now - timestamp;
        if (diff < 86400) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(timestamp * 1000));
        } else if (diff < 604800) {
            String[] days = {"الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"};
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(timestamp * 1000);
            return days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1];
        } else {
            return new SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                .format(new Date(timestamp * 1000));
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivType;
        TextView tvName, tvMessage, tvTime, tvUnread, tvAvatarLetter;
        View storyRing;

        VH(@NonNull View v) {
            super(v);
            ivAvatar       = v.findViewById(R.id.ivAvatar);
            ivType         = v.findViewById(R.id.ivType);
            tvName         = v.findViewById(R.id.tvName);
            tvMessage      = v.findViewById(R.id.tvMessage);
            tvTime         = v.findViewById(R.id.tvTime);
            tvUnread       = v.findViewById(R.id.tvUnread);
            tvAvatarLetter = v.findViewById(R.id.tvAvatarLetter);
            storyRing      = v.findViewById(R.id.storyRing);
        }
    }
}
