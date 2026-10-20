package com.example.asm_final.chat;

import android.content.Context;
import android.text.InputFilter;
import android.widget.*;
import com.example.asm_final.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.*;
import java.util.*;
import java.util.function.Consumer;

public final class ChatMenus {
  private ChatMenus() {}

  public static void name(
      Context context, int heading, String current, int max, Consumer<String> saved) {
    TextInputLayout field = new TextInputLayout(context);
    int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
    field.setPadding(padding, padding, padding, 0);
    field.setHint(context.getString(R.string.chat_name_hint));
    TextInputEditText input = new TextInputEditText(field.getContext());
    input.setSingleLine(true);
    input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(max)});
    input.setText(current);
    field.addView(input);
    androidx.appcompat.app.AlertDialog dialog =
        new MaterialAlertDialogBuilder(context)
            .setTitle(heading)
            .setView(field)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.chat_save, null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String text =
                          input.getText() == null ? "" : input.getText().toString().trim();
                      if (text.isEmpty()) {
                        field.setError(context.getString(R.string.chat_name_invalid, max));
                        return;
                      }
                      saved.accept(text);
                      dialog.dismiss();
                    }));
    dialog.show();
    input.requestFocus();
  }

  public static void thread(
      Context context, android.view.View anchor, ChatViewModel model, ChatRecords.Thread thread) {
    PopupMenu menu = new PopupMenu(context, anchor);
    menu.getMenu().add(0, 1, 0, R.string.chat_rename);
    menu.getMenu().add(0, 2, 1, R.string.chat_move);
    menu.getMenu().add(0, 3, 2, R.string.chat_delete);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 1)
            name(
                context,
                R.string.chat_rename,
                thread.title,
                120,
                title -> model.renameChat(thread.id, title));
          if (item.getItemId() == 2) move(context, model, thread.id);
          if (item.getItemId() == 3)
            new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.chat_delete_chat_title)
                .setMessage(R.string.chat_delete_chat_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.chat_delete, (d, w) -> model.deleteChat(thread.id))
                .show();
          return true;
        });
    menu.show();
  }

  private static void move(Context context, ChatViewModel model, String threadId) {
    List<ChatRecords.Folder> folders = new ArrayList<>(model.folders);
    String[] labels = new String[folders.size() + 1];
    labels[0] = context.getString(R.string.chat_unfiled);
    for (int i = 0; i < folders.size(); i++) labels[i + 1] = folders.get(i).title;
    new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.chat_move)
        .setItems(
            labels,
            (d, which) -> model.moveChat(threadId, which == 0 ? null : folders.get(which - 1).id))
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  public static void folder(
      Context context, android.view.View anchor, ChatViewModel model, ChatRecords.Folder folder) {
    PopupMenu menu = new PopupMenu(context, anchor);
    menu.getMenu().add(0, 1, 0, R.string.chat_rename);
    menu.getMenu().add(0, 2, 1, R.string.chat_delete);
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 1)
            name(
                context,
                R.string.chat_rename,
                folder.title,
                100,
                title -> model.renameFolder(folder.id, title));
          else
            new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.chat_delete_folder_title)
                .setMessage(R.string.chat_delete_folder_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(
                    R.string.chat_keep_chats, (d, w) -> model.deleteFolder(folder.id, false))
                .setNeutralButton(
                    R.string.chat_delete_folder_chats,
                    (d, w) -> model.deleteFolder(folder.id, true))
                .show();
          return true;
        });
    menu.show();
  }
}
