package com.example.pocketkit.adapter;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pocketkit.R;
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
        CheckBox cbDone;
        TextView tvTitle, tvContent, tvSubject, tvTime;
        ImageView ivPinned;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cbDone = itemView.findViewById(R.id.cbNoteDone);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvContent = itemView.findViewById(R.id.tvNoteContent);
            tvSubject = itemView.findViewById(R.id.tvNoteSubject);
            tvTime = itemView.findViewById(R.id.tvNoteTime);
            ivPinned = itemView.findViewById(R.id.ivPinned);
        }
    }

    @NonNull
    @Override
    public NoteAdapter.NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteAdapter.NoteViewHolder holder, int position) {

        Note note = noteList.get(position);
        String subject = (note.getSubject() != null && !note.getSubject().isEmpty()) ? note.getSubject() : "Chung";
        holder.tvTitle.setText(note.getTitle());
        holder.tvContent.setText(note.getContent());
        holder.tvSubject.setText("● " + subject);
        holder.tvTime.setText(note.getUpdatedAt());
        holder.ivPinned.setVisibility(note.getIsPinned() == 1 ? View.VISIBLE : View.GONE);
        holder.cbDone.setOnCheckedChangeListener(null);

        boolean done  = note.getIsDone() == 1;
        holder.cbDone.setChecked(done);

        if(done) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.itemView.setAlpha(1.0f);
        }

        holder.cbDone.setOnCheckedChangeListener(((buttonView, isChecked) ->
        {
            if(listener != null) {
                listener.onNoteCheckChanged(note, isChecked);
            }
        }));

        holder.itemView.setOnClickListener(v -> {
            if(listener != null) {
                listener.onNoteClick(note);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if(listener != null) {
                listener.onNoteLongClick(note);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }
}
