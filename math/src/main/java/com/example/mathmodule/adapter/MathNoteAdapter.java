package com.example.mathmodule.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mathmodule.R;
import com.example.mathmodule.databases.MathNoteItem;

import java.util.List;

public class MathNoteAdapter extends RecyclerView.Adapter<MathNoteAdapter.NoteViewHolder> {

    public interface NoteActionListener {
        void onPin(MathNoteItem item);
        void onEdit(MathNoteItem item);
        void onDelete(MathNoteItem item);
    }

    private List<MathNoteItem> noteList;
    private final NoteActionListener listener;

    public MathNoteAdapter(List<MathNoteItem> noteList, NoteActionListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    public void updateData(List<MathNoteItem> newList) {
        noteList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_row, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        MathNoteItem item = noteList.get(position);
        holder.txtTitle.setText(item.getTitle());
        holder.txtFormula.setText(item.getFormula());
        holder.txtCategory.setText(item.getCategory());

        if (item.getIsPinned() == 1) {
            holder.imgStar.setImageResource(android.R.drawable.btn_star_big_on);
            holder.imgStar.setColorFilter(Color.parseColor("#FFC107"));
            holder.imgStar.setContentDescription("Bỏ ghim ghi chú");
        } else {
            holder.imgStar.setImageResource(android.R.drawable.btn_star_big_off);
            holder.imgStar.setColorFilter(Color.parseColor("#616161"));
            holder.imgStar.setContentDescription("Ghim ghi chú");
        }

        holder.imgStar.setOnClickListener(v -> {
            if (listener != null) listener.onPin(item);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }

    static final class NoteViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitle;
        final TextView txtFormula;
        final TextView txtCategory;
        final ImageView imgStar;
        final Button btnEdit;
        final Button btnDelete;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_note_title);
            txtFormula = itemView.findViewById(R.id.txt_note_formula);
            txtCategory = itemView.findViewById(R.id.txt_note_category);
            imgStar = itemView.findViewById(R.id.img_star);
            btnEdit = itemView.findViewById(R.id.btn_edit_note);
            btnDelete = itemView.findViewById(R.id.btn_delete_note);
        }
    }
}
