package com.example.asm_final.dashboard;

import android.content.Context;
import com.example.mentor.data.WeeklyQuizPolicy;
import java.util.TimeZone;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Weekly XP across subjects, never lifetime EXP or a rank fabricated from local accounts. */
public final class LocalWeeklyQuizRepository implements WeeklyQuizRepository {
  private final Context context;
  private final LongSupplier clock;
  private final Supplier<TimeZone> zone;

  public LocalWeeklyQuizRepository(Context context) {
    this(context, System::currentTimeMillis, TimeZone::getDefault);
  }

  public LocalWeeklyQuizRepository(Context context, LongSupplier clock, Supplier<TimeZone> zone) {
    this.context = context.getApplicationContext();
    this.clock = clock;
    this.zone = zone;
  }

  @Override
  public WeeklyQuizProgress load(int userId) {
    long now = clock.getAsLong();
    long[] week = WeeklyQuizPolicy.week(now, zone.get());
    QuizProgressReader.Totals totals =
        QuizProgressReader.load(context, userId, week[0], week[1], now);
    return new WeeklyQuizProgress(totals.xp, totals.attempts, week[0], week[1]);
  }
}
