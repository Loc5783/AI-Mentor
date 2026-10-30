package com.example.asm_final;

import android.app.Application;
import com.example.asm_final.utils.LocaleHelper;
import com.example.asm_final.utils.ThemeHelper;

/** Applies the saved visual and language preferences before any Activity is created. */
public class AiMentorApplication extends Application implements com.example.mentor.schedule.domain.ScheduleRepositoryProvider {
  private AppServices services;

  public AppServices services() {
    return services;
  }

  @Override
  public com.example.mentor.schedule.domain.ScheduleRepository scheduleRepository() {
    return com.example.asm_final.supabase.SupabaseHttp.enabled(this)
        ? new com.example.asm_final.supabase.SupabaseScheduleRepository(this)
        : new com.example.mentor.schedule.data.ScheduleRepositoryImpl(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    com.example.asm_final.accounts.FreshSchoolInstall.prepare(this);
    services = new AppServices(this);
    ThemeHelper.applySavedTheme(this);
    LocaleHelper.applySavedLocale(this);
    com.example.asm_final.utils.SystemSafeArea.install(this);
    com.example.asm_final.utils.AppMotion.install(this);
    com.example.mathmodule.calculator.MathCalculator.install(this);
    com.example.asm_final.utils.DebugAccounts.initialize(this);
  }
}
