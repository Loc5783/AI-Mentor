package com.example.asm_final.activities.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.asm_final.R;
import com.example.asm_final.utils.LocaleHelper;
import com.example.asm_final.utils.ReminderHelper;
import com.example.asm_final.utils.ThemeHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class SettingsFragment extends Fragment {

  private String username;
  private SwitchMaterial reminderSwitch;
  private boolean updatingReminderSwitch;
  private final androidx.activity.result.ActivityResultLauncher<String> notificationPermission =
      registerForActivityResult(
          new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
          granted -> {
            if (isAdded()) {
              applyReminderChoice(granted);
              if (!granted)
                Toast.makeText(
                        requireContext(), R.string.reminder_permission_needed, Toast.LENGTH_LONG)
                    .show();
            }
          });

  private final androidx.activity.result.ActivityResultLauncher<String> imagePicker =
      registerForActivityResult(
          new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
          uri -> {
            if (uri != null) {
              try {
                InputStream in = requireContext().getContentResolver().openInputStream(uri);
                File outDir = new File(requireContext().getFilesDir(), "backgrounds");
                if (!outDir.exists()) outDir.mkdirs();
                File outFile = new File(outDir, "custom_bg.jpg");
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                in.close();
                ThemeHelper.setCustomBackground(requireContext(), outFile.getAbsolutePath());
                updateBackgroundStatus();
                // Request activity recreation to apply global background
                requireActivity().recreate();
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_settings, container, false);

    if (getArguments() != null) {
      username = getArguments().getString("USERNAME");
    }

    setupEditProfile(view);
    setupReminders(view);
    setupThemeSwitch(view);
    setupLanguagePicker(view);
    setupShare(view);
    setupAbout(view);
    setupLogout(view);

    if (getArguments() != null && getArguments().getBoolean("embedded", false))
      view.findViewById(R.id.settings_heading).setVisibility(View.GONE);
    view.findViewById(R.id.setting_app_guide)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(
                            requireContext(),
                            com.example.asm_final.activities.OnboardingActivity.class)
                        .putExtra(
                            "USERNAME",
                            getArguments() == null ? "" : getArguments().getString("USERNAME", ""))
                        .putExtra("REPLAY", true)));
    view.findViewById(R.id.setting_learning_profile)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(
                            requireContext(),
                            com.example.asm_final.activities.ProfileSetupActivity.class)
                        .putExtra(
                            "USERNAME",
                            getArguments() == null ? "" : getArguments().getString("USERNAME", ""))
                        .putExtra("EDIT", true)));
    view.findViewById(R.id.setting_verify_email)
        .setOnClickListener(
            v ->
                startActivity(
                    new android.content.Intent(
                            requireContext(),
                            com.example.asm_final.activities.AuthRecoveryActivity.class)
                        .putExtra("MODE", 2)));
    return view;
  }

  private void setupEditProfile(View view) {
    LinearLayout layoutEditProfile = view.findViewById(R.id.setting_edit_profile);
    layoutEditProfile.setOnClickListener(
        v -> {
          ((com.example.asm_final.activities.HomeActivity) requireActivity())
              .navigateTo(R.id.nav_edit_profile, true);
        });
  }

  private void setupReminders(View view) {
    reminderSwitch = view.findViewById(R.id.switch_reminders);
    reminderSwitch.setChecked(ReminderHelper.isReminderEnabled(requireContext()));
    reminderSwitch.setOnCheckedChangeListener(
        (button, enabled) -> {
          if (updatingReminderSwitch) return;
          if (enabled
              && Build.VERSION.SDK_INT >= 33
              && requireContext()
                      .checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                  != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            updatingReminderSwitch = true;
            reminderSwitch.setChecked(false);
            updatingReminderSwitch = false;
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            return;
          }
          applyReminderChoice(enabled);
        });
  }

  private void applyReminderChoice(boolean requested) {
    boolean enabled =
        requested
            && androidx.core.app.NotificationManagerCompat.from(requireContext())
                .areNotificationsEnabled();
    ReminderHelper.setReminderEnabled(requireContext(), enabled);
    if (reminderSwitch != null) {
      updatingReminderSwitch = true;
      reminderSwitch.setChecked(enabled);
      updatingReminderSwitch = false;
    }
    int message =
        requested && !enabled
            ? R.string.reminder_permission_needed
            : enabled ? R.string.reminder_enabled_time : R.string.reminder_disabled;
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (reminderSwitch != null) {
      updatingReminderSwitch = true;
      reminderSwitch.setChecked(ReminderHelper.isReminderEnabled(requireContext()));
      updatingReminderSwitch = false;
    }
  }

  @Override
  public void onDestroyView() {
    reminderSwitch = null;
    super.onDestroyView();
  }

  private void setupThemeSwitch(View view) {
    SwitchMaterial switchDarkMode = view.findViewById(R.id.switch_dark_mode);

    // Read the stored value instead of relying on AppCompat's process-only state.
    switchDarkMode.setChecked(ThemeHelper.isDarkMode(requireContext()));
    switchDarkMode.setOnCheckedChangeListener(
        (buttonView, isChecked) -> ThemeHelper.setDarkMode(requireContext(), isChecked));

    LinearLayout customBgLayout = view.findViewById(R.id.setting_custom_background);
    if (customBgLayout != null) {
      customBgLayout.setOnClickListener(v -> {
         String currentBg = ThemeHelper.getCustomBackground(requireContext());
         if (currentBg != null) {
             new AlertDialog.Builder(requireContext())
                 .setTitle("Hình nền")
                 .setItems(new String[]{"Chọn ảnh mới", "Xóa hình nền"}, (d, w) -> {
                     if (w == 0) {
                         imagePicker.launch("image/*");
                     } else {
                         ThemeHelper.setCustomBackground(requireContext(), null);
                         updateBackgroundStatus();
                         requireActivity().recreate();
                     }
                 }).show();
         } else {
             imagePicker.launch("image/*");
         }
      });
      updateBackgroundStatus(view);
    }
  }

  private void updateBackgroundStatus() {
      View view = getView();
      if (view != null) updateBackgroundStatus(view);
  }

  private void updateBackgroundStatus(View view) {
      TextView status = view.findViewById(R.id.txt_custom_background_status);
      if (status != null) {
          boolean hasCustom = ThemeHelper.getCustomBackground(requireContext()) != null;
          status.setText(hasCustom ? "Tùy chỉnh" : "Mặc định");
      }
  }

  private void setupLanguagePicker(View view) {
    LinearLayout layoutLanguage = view.findViewById(R.id.setting_language);
    TextView txtCurrentLang = view.findViewById(R.id.txt_current_language);
    updateLanguageLabel(txtCurrentLang);

    layoutLanguage.setOnClickListener(
        v -> {
          String[] languages = {
            getString(R.string.language_vietnamese), getString(R.string.language_english)
          };
          String[] codes = {LocaleHelper.LANGUAGE_VIETNAMESE, LocaleHelper.LANGUAGE_ENGLISH};
          int selected = LocaleHelper.isEnglish(requireContext()) ? 1 : 0;

          new AlertDialog.Builder(requireContext())
              .setTitle(R.string.settings_language)
              .setSingleChoiceItems(
                  languages,
                  selected,
                  (dialog, which) -> {
                    String chosenLanguage = codes[which];
                    if (!chosenLanguage.equals(LocaleHelper.getLanguage(requireContext()))) {
                      LocaleHelper.setLocale(requireContext(), chosenLanguage);
                    }
                    dialog.dismiss();
                  })
              .setNegativeButton(android.R.string.cancel, null)
              .show();
        });
  }

  private void updateLanguageLabel(TextView textView) {
    textView.setText(
        LocaleHelper.isEnglish(requireContext())
            ? R.string.language_english
            : R.string.language_vietnamese);
  }

  private void setupShare(View view) {
    LinearLayout layoutShare = view.findViewById(R.id.setting_share);
    layoutShare.setOnClickListener(
        v -> {
          Intent intent = new Intent(Intent.ACTION_SEND);
          intent.setType("text/plain");
          intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_share));
          intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.settings_share_text));
          startActivity(Intent.createChooser(intent, getString(R.string.settings_share_chooser)));
        });
  }

  private void setupAbout(View view) {
    LinearLayout layoutAbout = view.findViewById(R.id.setting_about);
    layoutAbout.setOnClickListener(
        v ->
            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_about)
                .setMessage(R.string.settings_about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
  }

  private void setupLogout(View view) {
    Button btnLogout = view.findViewById(R.id.btn_logout);
    btnLogout.setOnClickListener(
        v -> {
          ((com.example.asm_final.activities.HomeActivity) requireActivity()).logout();
          Toast.makeText(requireContext(), R.string.menu_logout, Toast.LENGTH_SHORT).show();
        });
  }
}
