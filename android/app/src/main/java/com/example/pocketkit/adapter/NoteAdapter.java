package com.example.pocketkit.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketkit.R;
import com.example.pocketkit.databinding.ItemNoteBinding;
import com.example.pocketkit.model.Note;
import com.example.pocketkit.ui.MotionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteInteractionListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);

        void onNoteCheckChanged(Note note, boolean isChecked);
    }

    private List<Note> noteList;
    private final OnNoteInteractionListener listener;
    private int recentlyToggledNoteId = -1;

    public NoteAdapter(List<Note> noteList, OnNoteInteractionListener listener) {
        this.noteList = noteList == null ? new ArrayList<>() : new ArrayList<>(noteList);
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        List<Note> oldList = this.noteList;
        List<Note> newList = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Note oldItem = oldList.get(oldItemPosition);
                Note newItem = newList.get(newItemPosition);
                return oldItem.getIsPinned() == newItem.getIsPinned()
                        && oldItem.getIsDone() == newItem.getIsDone()
                        && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                        && Objects.equals(oldItem.getContent(), newItem.getContent())
                        && Objects.equals(oldItem.getSubject(), newItem.getSubject())
                        && Objects.equals(oldItem.getUpdatedAt(), newItem.getUpdatedAt());
            }
        });
        this.noteList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        final ItemNoteBinding binding;

        NoteViewHolder(@NonNull ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        NoteViewHolder holder = new NoteViewHolder(binding);
        MotionHelper.attachCardPress(holder.itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        Context context = holder.itemView.getContext();

        String fallbackCategory = context.getString(R.string.note_default_category);
        String subject = !TextUtils.isEmpty(note.getSubject()) ? note.getSubject() : fallbackCategory;
        String content = note.getContent() != null ? note.getContent().trim() : "";

        holder.binding.tvNoteTitle.setText(note.getTitle());
        holder.binding.tvNoteSubject.setText(subject);
        holder.binding.tvNoteTime.setText(note.getUpdatedAt() != null ? note.getUpdatedAt() : "");
        applyPinIcon(holder.binding, context, note.getIsPinned() == 1);

        if (content.isEmpty()) {
            holder.binding.tvNoteContent.setVisibility(View.GONE);
        } else {
            holder.binding.tvNoteContent.setVisibility(View.VISIBLE);
            holder.binding.tvNoteContent.setText(content);
        }

        boolean done = note.getIsDone() == 1;
        applyDoneVisualState(holder.binding, context, done);
        if (note.getId() == recentlyToggledNoteId) {
            MotionHelper.animateCheckPop(holder.binding.ivNoteCheckBox);
            recentlyToggledNoteId = -1;
        } else {
            holder.binding.ivNoteCheckBox.animate().cancel();
            holder.binding.ivNoteCheckBox.setScaleX(1f);
            holder.binding.ivNoteCheckBox.setScaleY(1f);
        }

        holder.binding.btnNoteCheckContainer.setOnClickListener(v -> {
            if (listener != null) {
                recentlyToggledNoteId = note.getId();
                MotionHelper.performStateHaptic(v);
                listener.onNoteCheckChanged(note, !done);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onNoteLongClick(note);
            }
            return true;
        });
    }

    private void applyPinIcon(ItemNoteBinding binding, Context context, boolean pinned) {
        if (!pinned) {
            binding.tvNoteTitle.setCompoundDrawablesRelative(null, null, null, null);
            return;
        }
        Drawable pinDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pin);
        if (pinDrawable != null) {
            int size = context.getResources().getDimensionPixelSize(R.dimen.icon_size_xs);
            pinDrawable = pinDrawable.mutate();
            pinDrawable.setBounds(0, 0, size, size);
        }
        binding.tvNoteTitle.setCompoundDrawablesRelative(null, null, pinDrawable, null);
    }

    private void applyDoneVisualState(ItemNoteBinding binding, Context context, boolean done) {
        if (done) {
            binding.ivNoteCheckBox.setBackgroundResource(R.drawable.bg_check_checked);
            binding.ivNoteCheckBox.setImageResource(R.drawable.ic_check_white);
            binding.tvNoteTitle.setPaintFlags(binding.tvNoteTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvNoteTitle.setTextColor(ContextCompat.getColor(context, R.color.ink_muted));
            binding.tvNoteSubject.setBackgroundResource(R.drawable.bg_pill_success_soft);
            binding.tvNoteSubject.setTextColor(ContextCompat.getColor(context, R.color.accent_mint_text));
        } else {
            binding.ivNoteCheckBox.setBackgroundResource(R.drawable.bg_check_unchecked);
            binding.ivNoteCheckBox.setImageDrawable(null);
            binding.tvNoteTitle.setPaintFlags(binding.tvNoteTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvNoteTitle.setTextColor(ContextCompat.getColor(context, R.color.ink_primary));
            binding.tvNoteSubject.setBackgroundResource(R.drawable.bg_pill_neutral_soft);
            binding.tvNoteSubject.setTextColor(ContextCompat.getColor(context, R.color.ink_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }
}
