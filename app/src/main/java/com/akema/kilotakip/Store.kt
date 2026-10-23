package com.akema.kilotakip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

data class SetEntry(val kg: Double, val reps: Int)

/** Bir hareketin bir günlük kaydı. Hareket adıyla eşleşir, aynı hareket farklı günlerde geçmişi paylaşır. */
data class LogEntry(val id: Long, val exercise: String, val date: String, val sets: List<SetEntry>) {
    fun best(): SetEntry = sets.maxWith(compareBy<SetEntry>({ it.kg }, { it.reps }))
}

data class Change(val text: String, val sign: Int)

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    fun load(): List<LogEntry> =
        prefs.getString("logs", null)?.let { runCatching { fromJson(it) }.getOrNull() } ?: emptyList()

    fun save(list: List<LogEntry>) {
        prefs.edit().putString("logs", toJson(list)).apply()
    }

    companion object {
        fun toJson(list: List<LogEntry>): String = JSONArray().apply {
            list.forEach { e ->
                put(
                    JSONObject()
                        .put("id", e.id)
                        .put("ex", e.exercise)
                        .put("date", e.date)
                        .put("sets", JSONArray().apply {
                            e.sets.forEach { s -> put(JSONObject().put("kg", s.kg).put("r", s.reps)) }
                        })
                )
            }
        }.toString()

        fun fromJson(s: String): List<LogEntry> {
            val a = JSONArray(s)
            return (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                val sa = o.getJSONArray("sets")
                LogEntry(
                    o.getLong("id"),
                    o.getString("ex"),
                    o.getString("date"),
                    (0 until sa.length()).map { j ->
                        val so = sa.getJSONObject(j)
                        SetEntry(so.getDouble("kg"), so.getInt("r"))
                    }
                )
            }
        }
    }
}

fun fmt(d: Double): String =
    String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

fun setsText(sets: List<SetEntry>): String = sets.joinToString("  ·  ") { "${fmt(it.kg)}×${it.reps}" }

/** En iyi seti önceki kayıtla karşılaştırır: önce kilo, kilo aynıysa tekrar. */
fun change(cur: LogEntry, prev: LogEntry?): Change? {
    if (prev == null) return null
    val a = cur.best()
    val b = prev.best()
    val dk = a.kg - b.kg
    if (abs(dk) > 0.001) return Change((if (dk > 0) "+" else "−") + fmt(abs(dk)) + " kg", if (dk > 0) 1 else -1)
    val dr = a.reps - b.reps
    if (dr != 0) return Change((if (dr > 0) "+" else "−") + abs(dr) + " tekrar", if (dr > 0) 1 else -1)
    return Change("aynı", 0)
}
