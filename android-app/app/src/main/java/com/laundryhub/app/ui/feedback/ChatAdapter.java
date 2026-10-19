package com.laundryhub.app.ui.feedback;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ItemChatMessageBinding;
import com.laundryhub.app.model.feedback.ChatMessage;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Chat bubbles: the user's own on the right in accent, the shop's on the left in white with the sender. */
// OOP: Inheritance — a RecyclerView ListAdapter.
public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.Holder> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            return a.id() == b.id();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            return a.equals(b);
        }
    };

    /** Creates the adapter. */
    public ChatAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    /** One bubble. */
    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        Holder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            boolean mine = message.mine();
            LinearLayout root = binding.getRoot();
            root.setGravity(mine ? Gravity.END : Gravity.START);
            binding.sender.setVisibility(mine ? View.GONE : View.VISIBLE);
            binding.sender.setText(root.getContext().getString(R.string.chat_shop_sender, message.senderName()));
            binding.body.setText(message.body());
            binding.body.setBackgroundResource(mine ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_theirs);
            binding.body.setTextColor(ContextCompat.getColor(root.getContext(), mine ? R.color.surface : R.color.ink));
            binding.time.setText(TIME.format(message.sentAt()));
            root.setContentDescription((mine ? root.getContext().getString(R.string.chat_you)
                    : binding.sender.getText()) + ": " + message.body());
        }
    }
}
