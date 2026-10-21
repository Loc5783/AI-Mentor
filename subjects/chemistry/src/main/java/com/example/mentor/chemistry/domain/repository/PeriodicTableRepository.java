package com.example.mentor.chemistry.domain.repository;

import com.example.mentor.chemistry.domain.model.ChemicalElement;
import java.util.List;

public interface PeriodicTableRepository {

  List<ChemicalElement> getAll();

  List<ChemicalElement> search(String keyword);

  ChemicalElement findByAtomicNumber(int atomicNumber);
}
