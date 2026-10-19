package com.laundryhub.app.ui.rider;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.laundryhub.app.R;
import com.laundryhub.app.databinding.ItemBookingBinding;
import com.laundryhub.app.databinding.ItemRiderStateBinding;
import com.laundryhub.app.model.PesoFormat;
import com.laundryhub.app.model.rider.RiderJob;

/** The R1 list: booking cards, or the off-duty / empty card. */
// OOP: Inheritance — a RecyclerView ListAdapter.
public class BookingAdapter extends ListAdapter<RiderListItem, RecyclerView.ViewHolder> {

    /** What the card buttons do. */
    public interface Listener {
        /**
         * @param job "Accept booking"
         */
        void onAccept(@NonNull RiderJob job);

        /**
         * @param job "Decline"
         */
        void onDecline(@NonNull RiderJob job);

        /**
         * @param job "Start job" / "Continue job"
         */
        void onOpen(@NonNull RiderJob job);
    }

    private static final DiffUtil.ItemCallback<RiderListItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull RiderListItem a, @NonNull RiderListItem b) {
            return a.viewType() == b.viewType() && a.stableId() == b.stableId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RiderListItem a, @NonNull RiderListItem b) {
            return a.sameContent(b);
        }
    };

    // OOP: Encapsulation — the click handler is fixed at construction.
    private final Listener listener;

    /**
     * @param listener card button handler
     */
    public BookingAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // OOP: Polymorphism — each row reports its own type.
        return getItem(position).viewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == RiderListItem.TYPE_STATE) {
            return new StateHolder(ItemRiderStateBinding.inflate(inflater, parent, false));
        }
        return new BookingHolder(ItemBookingBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RiderListItem item = getItem(position);
        if (holder instanceof StateHolder state && item instanceof RiderListItem.State s) {
            state.bind(s);
        } else if (holder instanceof BookingHolder booking && item instanceof RiderListItem.Booking b) {
            booking.bind(b, listener);
        }
    }

    /** The off-duty or empty card. */
    static final class StateHolder extends RecyclerView.ViewHolder {
        private final ItemRiderStateBinding binding;

        StateHolder(ItemRiderStateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RiderListItem.State item) {
            binding.icon.setVisibility(item.isOffDuty() ? View.VISIBLE : View.GONE);
            binding.title.setVisibility(item.isOffDuty() ? View.VISIBLE : View.GONE);
            binding.title.setText(R.string.off_duty_title);
            binding.message.setText(item.getMessage());
        }
    }

    /** A booking card. */
    static final class BookingHolder extends RecyclerView.ViewHolder {
        private final ItemBookingBinding binding;

        BookingHolder(ItemBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RiderListItem.Booking item, Listener listener) {
            Context context = binding.getRoot().getContext();
            RiderJob job = item.getJob();
            JobFormat.badge(binding.badge, job.getType());
            binding.meta.setText(JobFormat.meta(context, job));
            binding.customer.setText(job.getCustomerName());
            binding.address.setText(job.getAddress());
            binding.address.setVisibility(job.getAddress().isEmpty() ? View.GONE : View.VISIBLE);
            binding.slot.setText(JobFormat.slot(context, job));
            binding.payNote.setText(job.getType().paymentNote(job.getPaymentMethod()));
            binding.total.setText(PesoFormat.amount(job.getTotal()));

            binding.actions.setVisibility(item.isAccepted() ? View.GONE : View.VISIBLE);
            binding.open.setVisibility(item.isAccepted() ? View.VISIBLE : View.GONE);
            binding.open.setText(job.isStarted() ? R.string.continue_job : R.string.start_job);
            binding.accept.setEnabled(!item.isBusy());
            binding.decline.setEnabled(!item.isBusy());
            binding.accept.setOnClickListener(v -> listener.onAccept(job));
            binding.decline.setOnClickListener(v -> listener.onDecline(job));
            binding.open.setOnClickListener(v -> listener.onOpen(job));
        }
    }

    /** 12 dp between cards (the design's list gap). */
    public static final class Spacing extends RecyclerView.ItemDecoration {
        private final int gap;

        /**
         * @param context for the dimension
         */
        public Spacing(@NonNull Context context) {
            gap = context.getResources().getDimensionPixelSize(R.dimen.card_gap);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.top = parent.getChildAdapterPosition(view) > 0 ? gap : 0;
        }
    }
}
