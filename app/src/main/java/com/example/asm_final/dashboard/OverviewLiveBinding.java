package com.example.asm_final.dashboard;

import androidx.fragment.app.Fragment;
import com.example.asm_final.AiMentorApplication;
import com.example.mentor.data.ActiveUser;

/** View-lifecycle adapter. Call start on resume and close the subscription on pause. */
public final class OverviewLiveBinding {
  private OverviewLiveBinding() {}

  public static OverviewUpdates.Subscription start(Fragment screen, Runnable refresh) {
    return ((AiMentorApplication) screen.requireActivity().getApplication())
        .services()
        .overviewUpdates()
        .observe(ActiveUser.get(screen.requireContext()), refresh);
  }
}
