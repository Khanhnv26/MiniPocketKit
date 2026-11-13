package com.example.pocketkit.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocketkit.R;
import com.example.pocketkit.databinding.ItemNoteBinding;
import com.example.pocketkit.model.Note;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    public interface OnNoteInteractionListener {
        void onNoteClick(Note note);

        void onNoteLongClick(Note note);

        void onNoteCheckChanged(Note note, boolean isChecked);
    }

    private List<Note> noteList;
    private final OnNoteInteractionListener listener;

    public NoteAdapter(List<Note> noteList, OnNoteInteractionListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    public void setNotes(List<Note> notes) {
        this.noteList = notes;
        notifyDataSetChanged();
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
        return new NoteViewHolder(binding);
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
        holder.binding.ivNotePinned.setVisibility(note.getIsPinned() == 1 ? View.VISIBLE : View.GONE);

        if (content.isEmpty()) {
            holder.binding.tvNoteContent.setVisibility(View.GONE);
        } else {
            holder.binding.tvNoteContent.setVisibility(View.VISIBLE);
            holder.binding.tvNoteContent.setText(content);
        }

        boolean done = note.getIsDone() == 1;
        applyDoneVisualState(holder.binding, context, done);

        holder.binding.btnNoteCheckContainer.setOnClickListener(v -> {
            if (listener != null) {
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

    private void applyDoneVisualState(ItemNoteBinding binding, Context context, boolean done) {
        if (done) {
            binding.viewNoteCheckBox.setBackgroundResource(R.drawable.bg_check_checked);
            binding.ivNoteCheck.setVisibility(View.VISIBLE);
            binding.tvNoteTitle.setPaintFlags(binding.tvNoteTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.tvNoteTitle.setTextColor(ContextCompat.getColor(context, R.color.ink_muted));
            binding.tvNoteSubject.setBackgroundResource(R.drawable.bg_pill_success_soft);
            binding.tvNoteSubject.setTextColor(ContextCompat.getColor(context, R.color.accent_mint_text));
        } else {
            binding.viewNoteCheckBox.setBackgroundResource(R.drawable.bg_check_unchecked);
            binding.ivNoteCheck.setVisibility(View.GONE);
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
