package com.example.mentor.chemistry;

import android.content.Context;
import com.example.mentor.chemistry.data.repository.AssetPeriodicTableRepository;
import com.example.mentor.chemistry.domain.repository.PeriodicTableRepository;

/** Chemistry-owned composition point, independent of app and other school subjects. */
public final class ChemistryServices {
  private ChemistryServices() {}

  public static PeriodicTableRepository periodicTable(Context context) {
    return new AssetPeriodicTableRepository(context);
  }
}
