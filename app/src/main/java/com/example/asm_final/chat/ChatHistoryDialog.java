package com.example.asm_final.chat;

import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.example.asm_final.R;
import com.example.mentor.ui.SearchText;
import com.google.android.material.bottomsheet.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.*;
import java.util.*;

/** Folders and recent conversations, scoped to the Activity's verified learner. */
public final class ChatHistoryDialog extends BottomSheetDialogFragment {
  private ChatViewModel model;
  private String folderId, query = "";
  private Rows adapter;
  private MaterialButton breadcrumb;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      folderId = state.getString("folder");
      query = state.getString("query", "");
    }
  }

  private int dp(int n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
    model = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(16), dp(16), dp(16), dp(12));
    root.setLayoutParams(
        new ViewGroup.LayoutParams(
            -1, Math.round(getResources().getDisplayMetrics().heightPixels * .82f)));
    LinearLayout header = new LinearLayout(requireContext());
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = new TextView(requireContext());
    title.setText(R.string.chat_history);
    title.setTextSize(22);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    MaterialButton create = new MaterialButton(requireContext());
    create.setBackgroundTintList(
        android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
    create.setTextColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), com.example.mentor.ui.R.color.primaryColor));
    create.setElevation(0f);
    create.setStateListAnimator(null);
    create.setCornerRadius(dp(18));
    create.setText(R.string.chat_folder_new);
    create.setOnClickListener(
        v ->
            ChatMenus.name(
                requireContext(), R.string.chat_folder_new, "", 100, model::createFolder));
    header.addView(create, new LinearLayout.LayoutParams(-2, -2));
    root.addView(header);
    TextInputLayout searchBox = new TextInputLayout(requireContext());
    searchBox.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
    searchBox.setBoxCornerRadii(dp(20), dp(20), dp(20), dp(20));
    searchBox.setHint(getString(R.string.chat_search));
    TextInputEditText search = new TextInputEditText(searchBox.getContext());
    search.setSingleLine(true);
    search.setText(query);
    searchBox.addView(search);
    root.addView(searchBox);
    breadcrumb = new MaterialButton(requireContext());
    breadcrumb.setBackgroundTintList(
        android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
    breadcrumb.setTextColor(
        androidx.core.content.ContextCompat.getColor(requireContext(), com.example.mentor.ui.R.color.primaryColor));
    breadcrumb.setElevation(0f);
    breadcrumb.setStateListAnimator(null);
    breadcrumb.setCornerRadius(dp(18));
    breadcrumb.setOnClickListener(
        v -> {
          folderId = null;
          render();
        });
    root.addView(breadcrumb);
    RecyclerView list = new RecyclerView(requireContext());
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    adapter = new Rows();
    list.setAdapter(adapter);
    root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
    TextView local = new TextView(requireContext());
    local.setText(R.string.chat_local_hint);
    local.setTextSize(12);
    local.setPadding(dp(8), dp(8), dp(8), 0);
    root.addView(local);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            query = s.toString();
            render();
          }

          public void afterTextChanged(Editable e) {}
        });
    return root;
  }

  @Override
  public void onViewCreated(View view, Bundle state) {
    model.changes().observe(getViewLifecycleOwner(), ignored -> render());
    render();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getDialog() instanceof BottomSheetDialog) {
      BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
      behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      behavior.setSkipCollapsed(true);
    }
  }

  private void render() {
    if (adapter == null) return;
    ChatRecords.Folder selected = null;
    for (ChatRecords.Folder folder : model.folders)
      if (folder.id.equals(folderId)) selected = folder;
    if (folderId != null && selected == null) folderId = null;
    breadcrumb.setText(
        selected == null
            ? getString(R.string.chat_all)
            : getString(R.string.chat_all) + "  /  " + selected.title);
    List<Object> rows = new ArrayList<>();
    if (folderId == null)
      for (ChatRecords.Folder folder : model.folders)
        if (SearchText.contains(folder.title, query)) rows.add(folder);
    for (ChatRecords.Thread thread : model.threads) {
      if (folderId != null && !Objects.equals(thread.folderId, folderId)) continue;
      if (thread.title.isEmpty() && thread.draft.isEmpty()) continue;
      String folderName = "";
      for (ChatRecords.Folder folder : model.folders)
        if (folder.id.equals(thread.folderId)) folderName = folder.title;
      if (SearchText.contains(thread.title, query)
          || SearchText.contains(thread.draft, query)
          || SearchText.contains(folderName, query)) rows.add(thread);
    }
    if (rows.isEmpty())
      rows.add(
          query.isEmpty()
              ? folderId == null ? R.string.chat_history_empty : R.string.chat_folder_empty
              : R.string.chat_search_empty);
    adapter.rows = rows;
    adapter.notifyDataSetChanged();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle out) {
    super.onSaveInstanceState(out);
    out.putString("folder", folderId);
    out.putString("query", query);
  }

  private final class Rows extends RecyclerView.Adapter<Row> {
    List<Object> rows = Collections.emptyList();

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int type) {
      LinearLayout row = new LinearLayout(parent.getContext());
      row.setGravity(Gravity.CENTER_VERTICAL);
      row.setMinimumHeight(dp(64));
      row.setPadding(dp(8), dp(4), 0, dp(4));
      row.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
      TextView text = new TextView(parent.getContext());
      text.setTextSize(16);
      text.setMaxLines(2);
      text.setEllipsize(TextUtils.TruncateAt.END);
      row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
      ImageButton more = new ImageButton(parent.getContext());
      more.setImageResource(R.drawable.ic_chat_more);
      more.setColorFilter(
          androidx.core.content.ContextCompat.getColor(
              parent.getContext(), com.example.mentor.ui.R.color.textColorPrimary));
      more.setBackgroundResource(android.R.color.transparent);
      more.setPadding(dp(12), dp(12), dp(12), dp(12));
      row.addView(more, new LinearLayout.LayoutParams(dp(48), dp(48)));
      return new Row(row, text, more);
    }

    @Override
    public void onBindViewHolder(@NonNull Row holder, int position) {
      Object item = rows.get(position);
      holder.itemView.setOnClickListener(null);
      holder.more.setVisibility(item instanceof Integer ? View.GONE : View.VISIBLE);
      if (item instanceof ChatRecords.Folder) {
        ChatRecords.Folder folder = (ChatRecords.Folder) item;
        holder.text.setText(getString(R.string.chat_folder) + " · " + folder.title);
        holder.text.setTypeface(null, android.graphics.Typeface.BOLD);
        holder.more.setContentDescription(
            getString(R.string.chat_folder_actions) + ": " + folder.title);
        holder.itemView.setOnClickListener(
            v -> {
              folderId = folder.id;
              render();
            });
        holder.more.setOnClickListener(v -> ChatMenus.folder(requireContext(), v, model, folder));
      } else if (item instanceof ChatRecords.Thread) {
        ChatRecords.Thread thread = (ChatRecords.Thread) item;
        holder.text.setTypeface(null, android.graphics.Typeface.NORMAL);
        holder.text.setText(thread.title.isEmpty() ? getString(R.string.chat_new) : thread.title);
        holder.more.setContentDescription(
            getString(R.string.chat_options) + ": " + holder.text.getText());
        holder.itemView.setOnClickListener(
            v -> {
              if (!model.loading && !model.importing) {
                model.select(thread.id);
                dismiss();
              }
            });
        holder.more.setOnClickListener(v -> ChatMenus.thread(requireContext(), v, model, thread));
      } else {
        holder.text.setText((Integer) item);
        holder.text.setTypeface(null, android.graphics.Typeface.NORMAL);
      }
    }

    @Override
    public int getItemCount() {
      return rows.size();
    }
  }

  private static final class Row extends RecyclerView.ViewHolder {
    final TextView text;
    final ImageButton more;

    Row(View view, TextView text, ImageButton more) {
      super(view);
      this.text = text;
      this.more = more;
    }
  }
}
