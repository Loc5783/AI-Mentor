package com.example.asm_final.learning

import com.example.mentor.learning.SubjectId

object SubjectToolResolver {
    /**
     * Resolves the specialized tool Activity for supported subjects according to curriculum design.
     * - MATH: calculator bubble attached directly to the lesson Activity
     * - PHYSICS: CircuitDiagramActivity (Circuit simulator)
     * - CHEMISTRY: PeriodicTableActivity (Periodic table of elements)
     * - Other subjects: null (core lesson & quiz curriculum)
     */
    @JvmStatic
    fun resolveToolActivity(id: SubjectId?): Class<*>? {
        return when (id) {
            SubjectId.PHYSICS -> com.example.mentor.physics.presentation.CircuitDiagramActivity::class.java
            SubjectId.CHEMISTRY -> com.example.mentor.chemistry.presentation.PeriodicTableActivity::class.java
            SubjectId.MATH -> null
            else -> null
        }
    }
}
