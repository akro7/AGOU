package com.akro.agram.ui.adapters;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akro.agram.R;
import com.akro.agram.models.TGMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.VH> {

    private static final int TYPE_OUT = 1;
    private static final int TYPE_IN  = 2;

    private final List<TGMessage> messages;

    public MessagesAdapter(List<TGMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isOut ? TYPE_OUT : TYPE_IN;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_OUT ?
            R.layout.item_message_out : R.layout.item_message_in;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        TGMessage msg = messages.get(position);

        h.tvText.setText(msg.text);
        h.tvTime.setText(formatTime(msg.date));

        // حالة الإرسال (للرسائل الصادرة)
        if (h.ivStatus != null) {
            switch (msg.sendState) {
                case TGMessage.STATE_SENDING:
                    h.ivStatus.setImageResource(R.drawable.ic_msg_clock);
                    break;
                case TGMessage.STATE_SENT:
                    h.ivStatus.setImageResource(R.drawable.ic_msg_check);
                    break;
                case TGMessage.STATE_READ:
                    h.ivStatus.setImageResource(R.drawable.ic_msg_dcheck);
                    break;
                case TGMessage.STATE_FAILED:
                    h.ivStatus.setImageResource(R.drawable.ic_msg_error);
                    break;
            }
        }

        // Reply bubble
        if (msg.replyToMsgId != 0 && msg.replyText != null && h.layoutReply != null) {
            h.layoutReply.setVisibility(View.VISIBLE);
            h.tvReply.setText(msg.replyText);
        } else if (h.layoutReply != null) {
            h.layoutReply.setVisibility(View.GONE);
        }

        // Forward
        if (msg.isForwarded && h.tvForward != null) {
            h.tvForward.setVisibility(View.VISIBLE);
            h.tvForward.setText("تمت إعادة التوجيه من: " + msg.forwardFromName);
        } else if (h.tvForward != null) {
            h.tvForward.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    private String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(new Date(timestamp * 1000));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvText, tvTime, tvReply, tvForward;
        ImageView ivStatus;
        View layoutReply;

        VH(@NonNull View v) {
            super(v);
            tvText     = v.findViewById(R.id.tvText);
            tvTime     = v.findViewById(R.id.tvTime);
            ivStatus   = v.findViewById(R.id.ivStatus);
            layoutReply = v.findViewById(R.id.layoutReply);
            tvReply    = v.findViewById(R.id.tvReply);
            tvForward  = v.findViewById(R.id.tvForward);
        }
    }
}
