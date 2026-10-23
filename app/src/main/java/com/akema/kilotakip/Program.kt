package com.akema.kilotakip

/** Set ve tekrar sayıları programın parçası, uygulamada değişmez; sadece kilo girilir. */
data class Exercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val note: String,
    val superset: Boolean = false,
) {
    val label: String
        get() = listOfNotNull("$sets set × $reps tekrar", note, if (superset) "Süper set" else null)
            .joinToString(" · ")
}

/** Haftanın gününe bağlı değil; hafta içinde herhangi bir gün yapılabilir. */
data class Workout(val name: String, val focus: String, val exercises: List<Exercise>)

const val WEEKLY_TARGET = 4

// Uygulama 172 cm, spor yapan bir erkek için: BMI ~24 ve bel/boy oranı ~0.47 varsayılan hedefler.
const val HEIGHT_CM = 172.0
const val GOAL_WEIGHT = 72.0
const val GOAL_WAIST = 80.0

val PROGRAM = listOf(
    Workout(
        "Antrenman A", "Göğüs · Omuz · Arka kol", listOf(
            Exercise("Plate Loaded Chest Press", 2, "5-6", "RIR1"),
            Exercise("Smith Machine Low Incline Press", 2, "5-6", "RIR1"),
            Exercise("Chest Fly Machine", 1, "6-8", "Failure"),
            Exercise("Shoulder Press Machine", 2, "6-8", "RIR1"),
            Exercise("Lateral Raise", 3, "8-10", "Failure"),
            Exercise("Triceps Pushdown", 2, "6-8", "Failure"),
            Exercise("Overhead Rope Extension", 2, "8-10", "Failure"),
        )
    ),
    Workout(
        "Antrenman B", "Sırt · Ön kol", listOf(
            Exercise("Lat Pulldown", 2, "6-8", "RIR1-Failure"),
            Exercise("Plate Loaded Wide Grip Row", 3, "6-8", "RIR1-Failure"),
            Exercise("Cable Row", 1, "8-10", "Failure"),
            Exercise("Incline Dumbbell Curl", 2, "6-8", "Failure"),
            Exercise("Cable Curl", 2, "6-8", "Failure"),
            Exercise("Hammer Curl", 2, "8-10", "Failure", superset = true),
            Exercise("Reverse Barbell Curl", 2, "8-10", "Failure", superset = true),
        )
    ),
    Workout(
        "Antrenman C", "Omuz · Göğüs · Arka kol", listOf(
            Exercise("Shoulder Press Machine", 2, "6-8", "RIR1"),
            Exercise("Lateral Raise", 3, "8-10", "Failure"),
            Exercise("Smith Machine Low Incline Press", 2, "5-6", "RIR1"),
            Exercise("Chest Fly Machine", 2, "6-8", "Failure"),
            Exercise("Cable Rear Delt Fly", 2, "8-10", "Failure"),
            Exercise("Triceps Pushdown", 2, "6-8", "Failure"),
            Exercise("Overhead Rope Extension", 2, "8-10", "Failure"),
        )
    ),
    Workout(
        "Antrenman D", "Sırt · Ön kol", listOf(
            Exercise("Plate Loaded Wide Grip Row", 3, "6-8", "RIR1-Failure"),
            Exercise("Lat Pulldown", 3, "6-8", "RIR1-Failure"),
            Exercise("Cable Curl", 2, "6-8", "Failure"),
            Exercise("Hammer Curl", 2, "8-10", "Failure", superset = true),
            Exercise("Reverse Barbell Curl", 2, "8-10", "Failure", superset = true),
        )
    ),
)
