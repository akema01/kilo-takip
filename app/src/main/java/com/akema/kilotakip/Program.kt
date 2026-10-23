package com.akema.kilotakip

import java.time.LocalDate

data class Exercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val note: String,
    val superset: Boolean = false,
) {
    val label: String
        get() = listOfNotNull("${sets}x$reps", note, if (superset) "Süper set" else null)
            .joinToString(" · ")
}

// dayOfWeek: 1 = Pazartesi ... 7 = Pazar
data class Day(val name: String, val dayOfWeek: Int, val exercises: List<Exercise>)

val PROGRAM = listOf(
    Day(
        "Pazartesi", 1, listOf(
            Exercise("Plate Loaded Chest Press", 2, "5-6", "RIR1"),
            Exercise("Smith Machine Low Incline Press", 2, "5-6", "RIR1"),
            Exercise("Chest Fly Machine", 1, "6-8", "Failure"),
            Exercise("Shoulder Press Machine", 2, "6-8", "RIR1"),
            Exercise("Lateral Raise", 3, "8-10", "Failure"),
            Exercise("Triceps Pushdown", 2, "6-8", "Failure"),
            Exercise("Overhead Rope Extension", 2, "8-10", "Failure"),
        )
    ),
    Day(
        "Salı", 2, listOf(
            Exercise("Lat Pulldown", 2, "6-8", "RIR1-Failure"),
            Exercise("Plate Loaded Wide Grip Row", 3, "6-8", "RIR1-Failure"),
            Exercise("Cable Row", 1, "8-10", "Failure"),
            Exercise("Incline Dumbbell Curl", 2, "6-8", "Failure"),
            Exercise("Cable Curl", 2, "6-8", "Failure"),
            Exercise("Hammer Curl", 2, "8-10", "Failure", superset = true),
            Exercise("Reverse Barbell Curl", 2, "8-10", "Failure", superset = true),
        )
    ),
    Day(
        "Çarşamba", 3, listOf(
            Exercise("Leg Press", 2, "6-8", "RIR1-2"),
            Exercise("Smith Machine Squat", 2, "6-8", "RIR1-2"),
            Exercise("Leg Extension", 2, "8-10", "Failure"),
            Exercise("Seated Leg Curl", 3, "8-10", "RIR1"),
        )
    ),
    Day(
        "Cuma", 5, listOf(
            Exercise("Shoulder Press Machine", 2, "6-8", "RIR1"),
            Exercise("Lateral Raise", 3, "8-10", "Failure"),
            Exercise("Smith Machine Low Incline Press", 2, "5-6", "RIR1"),
            Exercise("Chest Fly Machine", 2, "6-8", "Failure"),
            Exercise("Cable Rear Delt Fly", 2, "8-10", "Failure"),
            Exercise("Triceps Pushdown", 2, "6-8", "Failure"),
            Exercise("Overhead Rope Extension", 2, "8-10", "Failure"),
        )
    ),
    Day(
        "Cumartesi", 6, listOf(
            Exercise("Plate Loaded Wide Grip Row", 3, "6-8", "RIR1-Failure"),
            Exercise("Lat Pulldown", 3, "6-8", "RIR1-Failure"),
            Exercise("Romanian Deadlift", 2, "5-6", "RIR1-2"),
            Exercise("Cable Curl", 2, "6-8", "Failure"),
            Exercise("Hammer Curl", 2, "8-10", "Failure", superset = true),
            Exercise("Reverse Barbell Curl", 2, "8-10", "Failure", superset = true),
            Exercise("Leg Extension", 2, "6-8", "Failure"),
            Exercise("Seated Leg Curl", 1, "8-10", "Failure"),
        )
    ),
)

/** Bugünün antrenman günü; dinlenme günündeyse bir sonraki gün. */
fun todayIndex(): Int {
    val dow = LocalDate.now().dayOfWeek.value
    PROGRAM.indexOfFirst { it.dayOfWeek >= dow }.let { if (it >= 0) return it }
    return 0
}
