package com.cookandroid.justspeakapp.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cookandroid.justspeakapp.R;
import com.cookandroid.justspeakapp.model.ConversationMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.MessageViewHolder> {
    private List<ConversationMessage> messages;
    private SimpleDateFormat timeFormat;

    public ConversationAdapter(List<ConversationMessage> messages) {
        this.messages = messages;
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ConversationMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout messageContainer;
        private CardView cardMessage;
        private TextView tvSpeaker;
        private TextView tvMessage;
        private TextView tvTimestamp;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = (LinearLayout) itemView;
            cardMessage = itemView.findViewById(R.id.card_message);
            tvSpeaker = itemView.findViewById(R.id.tv_speaker);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        public void bind(ConversationMessage message) {
            tvMessage.setText(message.getText());
            tvTimestamp.setText(timeFormat.format(message.getTimestamp()));

            if ("user".equals(message.getSpeaker())) {
                tvSpeaker.setText("YOU");
                tvSpeaker.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_blue_dark));

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardMessage.getLayoutParams();
                params.gravity = Gravity.END;
                cardMessage.setLayoutParams(params);

                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_blue_light));
            } else {
                tvSpeaker.setText("AI");
                tvSpeaker.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_green_dark));

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardMessage.getLayoutParams();
                params.gravity = Gravity.START;
                cardMessage.setLayoutParams(params);

                cardMessage.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.white));
            }
        }
    }
}
