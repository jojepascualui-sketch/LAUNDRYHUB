package com.laundryhub.app.ui.feedback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.laundryhub.app.AppContainer;
import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.databinding.ActivityChatBinding;
import com.laundryhub.app.model.feedback.ChatMessage;
import com.laundryhub.app.ui.common.SystemBars;
import com.laundryhub.app.ui.common.UiText;
import com.laundryhub.app.ui.common.ViewModelFactory;

import java.util.List;

/**
 * Feedback chat with the shop, for customers and riders. There is no design for it (CLAUDE.md §7): it uses
 * the 07 header, existing cards, inputs and the accent colour. New replies arrive every {@value #POLL_MS} ms.
 */
// OOP: Inheritance — an AppCompatActivity.
public class ChatActivity extends AppCompatActivity {

    /** Refresh interval while the screen is open. */
    public static final long POLL_MS = 5_000L;

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private final ChatAdapter adapter = new ChatAdapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            viewModel.refresh();
            handler.postDelayed(this, POLL_MS);
        }
    };

    /**
     * @param context any context
     * @return the intent
     */
    @NonNull
    public static Intent intent(@NonNull Context context) {
        return new Intent(context, ChatActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SystemBars.enableForLightScreen(this);
        super.onCreate(savedInstanceState);
        AppContainer app = LaundryHubApp.container(this);
        if (app.sessions().getActive() == null) {
            finish();
            return;
        }
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.padForSystemBars(binding.getRoot());
        viewModel = new ViewModelProvider(this, new ViewModelFactory<>(ChatViewModel.class,
                () -> new ChatViewModel(app.feedbackRepository()))).get(ChatViewModel.class);

        LinearLayoutManager layout = new LinearLayoutManager(this);
        layout.setStackFromEnd(true);
        binding.messages.setLayoutManager(layout);
        binding.messages.setAdapter(adapter);

        binding.back.setOnClickListener(v -> finish());
        binding.send.setOnClickListener(v -> viewModel.send(text()));
        binding.input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSend();
            }
        });

        viewModel.getMessages().observe(this, this::render);
        viewModel.getLoading().observe(this, loading -> {
            binding.loading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            render(viewModel.getMessages().getValue());
        });
        viewModel.getSending().observe(this, busy -> updateSend());
        viewModel.getError().observe(this, this::renderError);
        viewModel.getSent().observe(this, event -> {
            if (event != null && event.take() != null) {
                binding.input.setText("");
            }
        });
        updateSend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            handler.postDelayed(poll, POLL_MS);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(poll);
        super.onPause();
    }

    private String text() {
        return binding.input.getText() == null ? "" : binding.input.getText().toString();
    }

    private void updateSend() {
        boolean busy = Boolean.TRUE.equals(viewModel.getSending().getValue());
        binding.send.setEnabled(!busy && !text().trim().isEmpty());
        binding.send.setAlpha(binding.send.isEnabled() ? 1f : DISABLED_ALPHA);
    }

    private static final float DISABLED_ALPHA = 0.4f;

    private void render(@Nullable List<ChatMessage> list) {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean empty = list == null || list.isEmpty();
        binding.empty.setVisibility(!loading && empty ? View.VISIBLE : View.GONE);
        adapter.submitList(list, () -> {
            if (list != null && !list.isEmpty()) {
                binding.messages.scrollToPosition(list.size() - 1);
            }
        });
    }

    private void renderError(@Nullable UiText text) {
        binding.banner.setVisibility(text == null ? View.GONE : View.VISIBLE);
        binding.banner.setText(text == null ? null : text.resolve(this));
    }
}
