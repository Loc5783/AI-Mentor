package com.example.modulelaptrinh.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modulelaptrinh.R;
import com.example.modulelaptrinh.database.ProgrammingNote;

import java.util.List;

public class ProgrammingNoteAdapter extends RecyclerView.Adapter<ProgrammingNoteAdapter.NoteViewHolder> {
    private List<ProgrammingNote> noteList;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(ProgrammingNote note);
        void onDeleteClick(int position, ProgrammingNote note);
    }

    public ProgrammingNoteAdapter(List<ProgrammingNote> noteList, OnNoteClickListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    public void updateData(List<ProgrammingNote> newList) {
        this.noteList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        ProgrammingNote note = noteList.get(position);
        holder.tvNoteTitle.setText(note.getTitle());
        holder.tvNotePreview.setText(note.getContent());
        holder.tvNoteDate.setText(note.getTimestamp());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoteClick(note);
        });

        holder.ivDeleteNote.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(position, note);
        });
    }

    @Override
    public int getItemCount() {
        return noteList != null ? noteList.size() : 0;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNotePreview, tvNoteDate;
        ImageView ivDeleteNote;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tv_note_title);
            tvNotePreview = itemView.findViewById(R.id.tv_note_preview);
            tvNoteDate = itemView.findViewById(R.id.tv_note_date);
            ivDeleteNote = itemView.findViewById(R.id.iv_delete_note);
        }
    }
}