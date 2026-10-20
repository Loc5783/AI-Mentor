package com.example.asm_final.chat;

import android.content.*;
import android.content.ClipboardManager;
import android.graphics.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import androidx.annotation.*;
import androidx.core.content.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.ListAdapter;
import com.example.asm_final.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class ChatMessagesAdapter
    extends ListAdapter<ChatRecords.Message, ChatMessagesAdapter.Holder> implements AutoCloseable {
  public interface Actions {
    void retry(String messageId);

    void file(ChatRecords.FileItem file);

    boolean retryablePending(String messageId);
  }

  private final int owner;
  private final Actions actions;
  private final ExecutorService images = Executors.newFixedThreadPool(2);
  private volatile boolean closed;
  public boolean requesting;

  public void setRequesting(boolean value) {
    if (requesting != value) {
      requesting = value;
      notifyItemRangeChanged(0, getItemCount());
    }
  }

  private static final DiffUtil.ItemCallback<ChatRecords.Message> DIFF =
      new DiffUtil.ItemCallback<ChatRecords.Message>() {
        public boolean areItemsTheSame(ChatRecords.Message a, ChatRecords.Message b) {
          return a.id.equals(b.id);
        }

        public boolean areContentsTheSame(ChatRecords.Message a, ChatRecords.Message b) {
          return a.text.equals(b.text)
              && a.status.equals(b.status)
              && a.error.equals(b.error)
              && a.truncated == b.truncated
              && a.files.size() == b.files.size();
        }
      };

  public ChatMessagesAdapter(int owner, Actions actions) {
    super(DIFF);
    this.owner = owner;
    this.actions = actions;
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
    return new Holder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_general_chat_message, parent, false));
  }

  private int dp(View view, int n) {
    return Math.round(n * view.getResources().getDisplayMetrics().density);
  }

  private CharSequence format(String source) {
    // Plain text remains selectable and copyable; only local emphasis/code styling is applied.
    SpannableStringBuilder result = new SpannableStringBuilder(source);
    for (String marker : new String[] {"**", "`"}) {
      int start = 0;
      while ((start = result.toString().indexOf(marker, start)) >= 0) {
        int end = result.toString().indexOf(marker, start + marker.length());
        if (end < 0) break;
        result.delete(end, end + marker.length());
        result.delete(start, start + marker.length());
        result.setSpan(
            marker.equals("**") ? new StyleSpan(Typeface.BOLD) : new TypefaceSpan("monospace"),
            start,
            end - marker.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        start = end - marker.length();
      }
    }
    return result;
  }

  @Override
  public void onBindViewHolder(@NonNull Holder h, int position) {
    ChatRecords.Message message = getItem(position);
    Context context = h.itemView.getContext();
    boolean user = message.role.equals("USER");
    ((LinearLayout) h.itemView).setGravity(user ? Gravity.END : Gravity.START);
    h.role.setText(user ? R.string.chat_user : R.string.chat_assistant);
    h.text.setVisibility(message.text.isEmpty() ? View.GONE : View.VISIBLE);
    h.text.setText(user ? message.text : format(message.text));
    h.text.setBackgroundResource(user ? R.drawable.chat_user_bubble : android.R.color.transparent);
    if (!user) h.text.setPadding(0, dp(h.text, 4), 0, dp(h.text, 4));
    h.files.removeAllViews();
    for (ChatRecords.FileItem file : message.files) {
      if (file.mime.startsWith("image/")) {
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(file.name);
        image.setTag(file.id);
        h.files.addView(image, new LinearLayout.LayoutParams(dp(h.files, 192), dp(h.files, 144)));
        image.setOnClickListener(v -> actions.file(file));
        images.execute(
            () -> {
              try {
                java.io.File source = new LocalChatFiles(context).resolve(owner, file);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(source.getPath(), options);
                int sample = 1;
                while (Math.max(options.outWidth, options.outHeight) / sample > 512) sample *= 2;
                options.inJustDecodeBounds = false;
                options.inSampleSize = sample;
                Bitmap bitmap = BitmapFactory.decodeFile(source.getPath(), options);
                image.post(
                    () -> {
                      if (!closed && file.id.equals(image.getTag())) image.setImageBitmap(bitmap);
                      else if (bitmap != null) bitmap.recycle();
                    });
              } catch (RuntimeException invalid) {
              }
            });
      }
      TextView label = new TextView(context);
      label.setText(file.name);
      label.setTextSize(13);
      label.setMaxWidth(dp(label, 240));
      label.setMaxLines(2);
      label.setMinHeight(dp(label, 48));
      label.setGravity(Gravity.CENTER_VERTICAL);
      label.setTextColor(ContextCompat.getColor(context, R.color.primaryColor));
      label.setPadding(dp(label, 8), 0, dp(label, 8), 0);
      label.setOnClickListener(v -> actions.file(file));
      h.files.addView(label);
    }
    int status = 0;
    if (user) {
      if (message.status.equals("PENDING")) status = R.string.chat_thinking;
      else if (message.status.equals("CANCELLED")) status = R.string.chat_message_stopped;
      else if (message.status.equals("FAILED")) status = R.string.chat_no_response;
    } else if (message.truncated) status = R.string.chat_truncated;
    h.status.setVisibility(status == 0 ? View.GONE : View.VISIBLE);
    if (status != 0) h.status.setText(status);
    h.copy.setOnClickListener(
        v -> {
          ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE))
              .setPrimaryClip(ClipData.newPlainText("AI Mentor", message.text));
          Toast.makeText(context, R.string.chat_copied, Toast.LENGTH_SHORT).show();
        });
    h.copy.setVisibility(message.text.isEmpty() ? View.GONE : View.VISIBLE);
    h.retry.setVisibility(
        user
                && !requesting
                && (message.status.equals("FAILED")
                    || message.status.equals("CANCELLED")
                    || actions.retryablePending(message.id))
            ? View.VISIBLE
            : View.GONE);
    h.retry.setOnClickListener(v -> actions.retry(message.id));
    boolean formulas =
        !user
            && (message.text.contains("$")
                || message.text.contains("\\(")
                || message.text.contains("\\["));
    h.formula.setVisibility(formulas ? View.VISIBLE : View.GONE);
    h.formula.setOnClickListener(
        v -> {
          WebView web = new WebView(context);
          web.setMinimumHeight(dp(web, 300));
          String tex =
              message
                  .text
                  .replace("\\(", "$")
                  .replace("\\)", "$")
                  .replace("\\[", "$$")
                  .replace("\\]", "$$");
          com.example.mentor.ui.MathTextRenderer.render(web, tex, 16);
          androidx.appcompat.app.AlertDialog dialog =
              new MaterialAlertDialogBuilder(context)
                  .setTitle(R.string.chat_formula)
                  .setView(web)
                  .setPositiveButton(android.R.string.ok, null)
                  .create();
          dialog.setOnDismissListener(d -> web.destroy());
          dialog.show();
        });
  }

  @Override
  public void close() {
    closed = true;
    images.shutdownNow();
  }

  public static final class Holder extends RecyclerView.ViewHolder {
    final TextView role, text, status;
    final LinearLayout files;
    final View copy, retry, formula;

    Holder(View view) {
      super(view);
      role = view.findViewById(R.id.chat_message_role);
      text = view.findViewById(R.id.chat_message_text);
      status = view.findViewById(R.id.chat_message_status);
      files = view.findViewById(R.id.chat_message_files);
      copy = view.findViewById(R.id.chat_message_copy);
      retry = view.findViewById(R.id.chat_message_retry);
      formula = view.findViewById(R.id.chat_message_formula);
    }
  }
}
