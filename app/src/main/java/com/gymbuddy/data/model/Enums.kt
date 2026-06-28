package com.gymbuddy.data.model

enum class Sex { MALE, FEMALE, OTHER }

enum class Units {
    METRIC, IMPERIAL;

    val weightLabel: String get() = if (this == METRIC) "kg" else "lb"
    val heightLabel: String get() = if (this == METRIC) "cm" else "in"
}

enum class Goal(val label: String, val defaultSets: Int, val repLow: Int, val repHigh: Int, val restSec: Int) {
    LOSE_FAT("Lose fat", 3, 12, 15, 45),
    BUILD_MUSCLE("Build muscle", 4, 8, 12, 75),
    STRENGTH("Get stronger", 5, 4, 6, 150),
    ENDURANCE("Endurance", 3, 15, 20, 40),
    GENERAL("General fitness", 3, 10, 12, 60),
}

enum class Experience(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

enum class AchievementType(val title: String, val emoji: String, val description: String) {
    FIRST_WORKOUT("First Steps", "👟", "Complete your first workout"),
    STREAK_3("On a Roll", "🔥", "3-day workout streak"),
    STREAK_7("Unstoppable", "⚡", "7-day workout streak"),
    STREAK_30("Iron Will", "🏆", "30-day workout streak"),
    VOLUME_10K("Heavy Lifter", "💪", "Move 10,000 kg in a single session"),
    PR_FIRST("New Record", "🎯", "Set your first personal record"),
    TEN_WORKOUTS("Committed", "📅", "Complete 10 workouts"),
    HUNDRED_WORKOUTS("Centurion", "💯", "Complete 100 workouts"),
}

/** Display language for instructions. */
enum class Language(val code: String, val label: String) {
    EN("en", "English"),
    TR("tr", "Türkçe"),
    IT("it", "Italiano"),
}
