package com.gymbuddy.domain

/**
 * Approximate MET (metabolic equivalent) values for resistance training, derived from the
 * exercise's category and equipment. Based on the Compendium of Physical Activities
 * (resistance training ranges ~3.5 light → 6.0 vigorous).
 */
object MetTable {
    fun metFor(category: String, equipment: String, target: String): Double {
        val cat = category.lowercase()
        val eq = equipment.lowercase()

        val base = when {
            "cardio" in cat -> 7.0
            "upper legs" in cat || "lower legs" in cat -> 5.5 // compound leg work is demanding
            "back" in cat || "chest" in cat -> 5.0
            "shoulders" in cat -> 4.5
            "waist" in cat || "abs" in target.lowercase() -> 4.0
            "upper arms" in cat || "lower arms" in cat -> 3.8
            else -> 4.0
        }

        val equipFactor = when {
            "barbell" in eq || "leverage" in eq || "smith" in eq -> 1.15
            "body weight" in eq -> 1.05
            "dumbbell" in eq || "kettlebell" in eq -> 1.1
            "cable" in eq || "band" in eq -> 1.0
            else -> 1.0
        }
        return (base * equipFactor).coerceIn(3.0, 8.0)
    }
}
