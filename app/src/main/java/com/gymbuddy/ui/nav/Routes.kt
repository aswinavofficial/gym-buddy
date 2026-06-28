package com.gymbuddy.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val BROWSE = "browse"
    const val PLANS = "plans"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"

    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/$id"

    const val PLAN_EDITOR = "plan_editor/{planId}"
    fun planEditor(planId: Long) = "plan_editor/$planId"

    const val PICKER = "picker"

    const val WORKOUT = "workout/{sessionId}"
    fun workout(sessionId: Long) = "workout/$sessionId"

    const val HISTORY = "history"
    const val SETTINGS = "settings"

    // savedStateHandle keys for returning a picked exercise.
    const val RESULT_PICKED_EXERCISE = "picked_exercise_id"
}

/** Top-level destinations shown in the bottom navigation bar. */
enum class TopDestination(val route: String, val label: String) {
    HOME(Routes.HOME, "Home"),
    BROWSE(Routes.BROWSE, "Browse"),
    PLANS(Routes.PLANS, "Plans"),
    PROGRESS(Routes.PROGRESS, "Progress"),
    PROFILE(Routes.PROFILE, "Profile"),
}
