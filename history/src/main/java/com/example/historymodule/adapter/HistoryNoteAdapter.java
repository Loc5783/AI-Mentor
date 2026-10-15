package com.example.historymodule.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.historymodule.R;
import com.example.historymodule.databases.HistoryNoteItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class HistoryNoteAdapter
        extends RecyclerView.Adapter<HistoryNoteAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(HistoryNoteItem item);
        void onDelete(HistoryNoteItem item);
        void onTogglePin(HistoryNoteItem item);
    }

    private final List<HistoryNoteItem> noteList;
    private final Listener listener;
    private final SimpleDateFormat formatter =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public HistoryNoteAdapter(List<HistoryNoteItem> noteList, Listener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item_note_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryNoteItem item = noteList.get(position);
        holder.title.setText(item.getTitle());
        holder.content.setText(item.getContent());
        holder.time.setText(item.getUpdatedAt() <= 0L
                ? ""
                : "Cập nhật " + formatter.format(new Date(item.getUpdatedAt())));
        holder.pin.setImageResource(item.isPinned()
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
        holder.pin.setContentDescription(item.isPinned() ? "Bỏ ghim" : "Ghim ghi chú");
        holder.pin.setOnClickListener(v -> listener.onTogglePin(item));
        holder.edit.setOnClickListener(v -> listener.onEdit(item));
        holder.delete.setOnClickListener(v -> listener.onDelete(item));
        holder.itemView.setOnClickListener(v -> listener.onEdit(item));
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView content;
        final TextView time;
        final ImageButton pin;
        final ImageButton edit;
        final ImageButton delete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            content = itemView.findViewById(R.id.note_content);
            time = itemView.findViewById(R.id.note_updated_at);
            pin = itemView.findViewById(R.id.btn_note_pin);
            edit = itemView.findViewById(R.id.btn_note_edit);
            delete = itemView.findViewById(R.id.btn_note_delete);
        }
    }
}
