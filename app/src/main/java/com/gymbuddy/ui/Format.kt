package com.gymbuddy.ui

import com.gymbuddy.data.model.Units
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Format {
    private const val KG_TO_LB = 2.2046226218

    fun weight(kg: Double, units: Units): String = when (units) {
        Units.METRIC -> "${trim(kg)} kg"
        Units.IMPERIAL -> "${trim(kg * KG_TO_LB)} lb"
    }

    fun weightValue(kg: Double, units: Units): Double =
        if (units == Units.METRIC) kg else kg * KG_TO_LB

    fun toKg(value: Double, units: Units): Double =
        if (units == Units.METRIC) value else value / KG_TO_LB

    fun height(cm: Double, units: Units): String = when (units) {
        Units.METRIC -> "${cm.roundToInt()} cm"
        Units.IMPERIAL -> {
            val totalInches = cm / 2.54
            val feet = (totalInches / 12).toInt()
            val inches = (totalInches % 12).roundToInt()
            "${feet}'${inches}\""
        }
    }

    fun trim(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

    fun date(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    fun shortDate(millis: Long): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))

    fun duration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun titleCase(value: String): String =
        value.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}
