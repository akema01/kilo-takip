package com.akema.kilotakip

import java.time.LocalDate
import kotlin.random.Random

/**
 * Denemek için ~3 aylık örnek veri: 13 hafta, arada 4'ü tamamlanmamış iki hafta (seri onlardan sonra başlar),
 * düzenli kilo artışları ve haftalık vücut ölçüleri.
 */
fun sampleData(): AppData {
    val rnd = Random(172)
    val today = LocalDate.now()
    val current = weekStart(today)
    val first = current.minusWeeks(12)
    var id = 1L

    val sessions = mutableListOf<Session>()
    val trainingDays = listOf(0L, 1L, 3L, 5L) // Pzt, Sal, Per, Cmt
    var week = first
    var index = 0
    while (!week.isAfter(current)) {
        val count = if (index == 3 || index == 5) 3 else WEEKLY_TARGET
        for (k in 0 until count) {
            val day = week.plusDays(trainingDays[k])
            if (day.isAfter(today)) break
            sessions += Session(id++, k, day.toString())
        }
        week = week.plusWeeks(1)
        index++
    }

    val base = mapOf(
        "Plate Loaded Chest Press" to 50.0,
        "Smith Machine Low Incline Press" to 40.0,
        "Chest Fly Machine" to 45.0,
        "Shoulder Press Machine" to 35.0,
        "Lateral Raise" to 8.0,
        "Triceps Pushdown" to 25.0,
        "Overhead Rope Extension" to 20.0,
        "Lat Pulldown" to 55.0,
        "Plate Loaded Wide Grip Row" to 50.0,
        "Cable Row" to 50.0,
        "Incline Dumbbell Curl" to 10.0,
        "Cable Curl" to 20.0,
        "Hammer Curl" to 12.0,
        "Reverse Barbell Curl" to 20.0,
        "Cable Rear Delt Fly" to 10.0,
    )
    val history = mutableListOf<WeightChange>()
    base.forEach { (name, start) ->
        val step = if (start < 15) 1.0 else 2.5
        var kg = start
        var day = first.plusDays(rnd.nextLong(0, 3))
        while (!day.isAfter(today)) {
            history += WeightChange(id++, name, day.toString(), kg)
            kg += step
            day = day.plusWeeks(rnd.nextLong(2, 4))
        }
    }

    val body = mutableListOf<BodyEntry>()
    var weight = 78.6
    var waist = 91.0
    var day = first
    while (!day.isAfter(today)) {
        body += BodyEntry(id++, day.toString(), round1(weight), round1(waist), if (body.isEmpty()) HEIGHT_CM else null)
        weight -= 0.35 + rnd.nextDouble(-0.25, 0.2)
        waist -= 0.45 + rnd.nextDouble(-0.3, 0.2)
        day = day.plusWeeks(1)
    }

    return AppData(
        history = history,
        sessions = sessions,
        cardio = Cardio(5.5, 10.0, 20),
        body = body,
        start = first.toString(),
    )
}

private fun round1(d: Double) = Math.round(d * 10) / 10.0
