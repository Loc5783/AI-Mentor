package com.example.asm_final.competition;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asm_final.R;
import com.example.asm_final.utils.ScreenViews;
import java.util.*;
import java.util.function.Consumer;

/** A single scrolling surface keeps the header and rows usable on small screens and large text. */
public final class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.Holder> {
  private final View header;
  private final Consumer<PublicLearner> open;
  private RankingBoard board;

  public RankingAdapter(View header, Consumer<PublicLearner> open) {
    this.header = header;
    this.open = open;
  }

  public void submit(RankingBoard value) {
    board = value;
    notifyDataSetChanged();
  }

  @Override
  public int getItemCount() {
    return 1 + (board == null ? 0 : board.entries.size());
  }

  @Override
  public int getItemViewType(int position) {
    return position == 0 ? 0 : 1;
  }

  static final class Holder extends RecyclerView.ViewHolder {
    TextView rank, name, score;

    Holder(View view) {
      super(view);
    }
  }

  @Override
  public Holder onCreateViewHolder(ViewGroup parent, int type) {
    if (type == 0) return new Holder(header);
    android.content.Context c = parent.getContext();
    LinearLayout outer = new LinearLayout(c);
    outer.setPadding(ScreenViews.dp(c, 20), 0, ScreenViews.dp(c, 20), ScreenViews.dp(c, 10));
    outer.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
    LinearLayout card = ScreenViews.card(c);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setMinimumHeight(ScreenViews.dp(c, 72));
    TextView rank = ScreenViews.text(c, "", 20, true);
    rank.setPadding(0, 0, ScreenViews.dp(c, 14), 0);
    card.addView(rank);
    LinearLayout labels = new LinearLayout(c);
    labels.setOrientation(LinearLayout.VERTICAL);
    TextView name = ScreenViews.text(c, "", 18, true), score = ScreenViews.text(c, "", 15, false);
    labels.addView(name);
    labels.addView(score);
    card.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
    outer.addView(card, new LinearLayout.LayoutParams(-1, -2));
    Holder holder = new Holder(outer);
    holder.rank = rank;
    holder.name = name;
    holder.score = score;
    outer.setFocusable(true);
    outer.setClickable(true);
    android.util.TypedValue ripple = new android.util.TypedValue();
    c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
    outer.setForeground(androidx.core.content.ContextCompat.getDrawable(c, ripple.resourceId));
    return holder;
  }

  @Override
  public void onBindViewHolder(Holder holder, int position) {
    if (position == 0) return;
    RankingBoard.Entry entry = board.entries.get(position - 1);
    holder.rank.setText(Integer.toString(entry.rank));
    holder.name.setText(entry.learner.nickname);
    String score =
        holder
            .itemView
            .getContext()
            .getString(
                board.query.metric == CompetitionQuery.Metric.DAILY_XP
                    ? R.string.competition_xp_value
                    : R.string.competition_streak_value,
                entry.value);
    holder.score.setText(score);
    holder.itemView.setContentDescription(
        holder
            .itemView
            .getContext()
            .getString(R.string.ranking_open_profile, entry.learner.nickname, entry.rank, score));
    holder.itemView.setOnClickListener(v -> open.accept(entry.learner));
  }
}
