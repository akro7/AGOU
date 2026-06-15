package com.akro.agram.ui.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akro.agram.R;
import com.akro.agram.models.TGStory;
import com.bumptech.glide.Glide;

import java.util.List;

public class StoriesAdapter extends RecyclerView.Adapter<StoriesAdapter.VH> {

    public interface OnStoryClick { void onClick(TGStory story); }

    private final List<TGStory> stories;
    private final OnStoryClick listener;

    private static final int[] COLORS = {
        0xFF2CA5E0, 0xFF5EC245, 0xFFFF6B6B, 0xFFFFD93D,
        0xFF6C5CE7, 0xFFFD79A8, 0xFF00CEC9
    };

    public StoriesAdapter(List<TGStory> stories, OnStoryClick listener) {
        this.stories = stories;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_story, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TGStory story = stories.get(position);
        h.tvName.setText(story.peerName);

        // Ring color - أزرق لو لم تشاهد، رمادي لو شاهدت
        int ringColor = story.viewed ? 0xFFAAAAAA : 0xFF2CA5E0;
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setStroke(3, ringColor);
        ring.setColor(0x00000000);
        h.storyRing.setBackground(ring);

        if (story.peerAvatarUrl != null && !story.peerAvatarUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                 .load(story.peerAvatarUrl)
                 .circleCrop()
                 .into(h.ivAvatar);
        } else {
            int color = COLORS[(int)(Math.abs(story.peerId) % COLORS.length)];
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            h.ivAvatar.setBackground(circle);
            h.tvLetter.setVisibility(View.VISIBLE);
            h.tvLetter.setText(story.peerName.isEmpty() ? "?" :
                String.valueOf(story.peerName.charAt(0)).toUpperCase());
        }

        h.itemView.setOnClickListener(v -> listener.onClick(story));
    }

    @Override
    public int getItemCount() { return stories.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLetter;
        View storyRing;

        VH(@NonNull View v) {
            super(v);
            ivAvatar  = v.findViewById(R.id.ivAvatar);
            tvName    = v.findViewById(R.id.tvName);
            tvLetter  = v.findViewById(R.id.tvLetter);
            storyRing = v.findViewById(R.id.storyRing);
        }
    }
}
