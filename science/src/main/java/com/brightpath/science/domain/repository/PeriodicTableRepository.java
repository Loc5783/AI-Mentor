package com.brightpath.science.domain.repository;

import com.brightpath.science.domain.model.ChemicalElement;

import java.util.List;

public interface PeriodicTableRepository {
    List<ChemicalElement> getAll();
    List<ChemicalElement> search(String keyword);
    ChemicalElement findByAtomicNumber(int atomicNumber);
}
