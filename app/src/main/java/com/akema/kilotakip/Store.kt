package com.akema.kilotakip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

/** Bir hareketin kilosunun değiştiği an. Güncel kilo = hareketin son değişikliği. */
data class WeightChange(val id: Long, val exercise: String, val date: String, val kg: Double)

/** Bitirilmiş bir antrenman. */
data class Session(val id: Long, val workout: Int, val date: String)

data class Change(val text: String, val sign: Int)

data class AppData(
    val history: List<WeightChange> = emptyList(),
    val sessions: List<Session> = emptyList(),
) {
    fun historyOf(exercise: String): List<WeightChange> =
        history.filter { it.exercise == exercise }.sortedWith(compareBy({ it.date }, { it.id }))

    fun weight(exercise: String): Double? = historyOf(exercise).lastOrNull()?.kg

    /** Kilo aynıysa hiçbir şey değişmez; aynı gün içinde tekrar değişirse o günün kaydı düzeltilir. */
    fun setWeight(exercise: String, kg: Double): AppData {
        val h = historyOf(exercise)
        val last = h.lastOrNull()
        if (last != null && same(last.kg, kg)) return this
        val today = LocalDate.now().toString()
        var rest = history
        if (last != null && last.date == today) {
            rest = history - last
            val prev = h.getOrNull(h.size - 2)
            if (prev != null && same(prev.kg, kg)) return copy(history = rest)
        }
        return copy(history = rest + WeightChange(System.currentTimeMillis(), exercise, today, kg))
    }

    fun finish(workout: Int): AppData =
        copy(sessions = sessions + Session(System.currentTimeMillis(), workout, LocalDate.now().toString()))

    /** Bu haftanın (Pazartesi'den itibaren) antrenmanları. */
    fun thisWeek(): List<Session> {
        val start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return sessions.filter { !LocalDate.parse(it.date).isBefore(start) }
    }
}

private fun same(a: Double, b: Double) = abs(a - b) < 0.001

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    // "logs" ilk sürümün verisi; varsa bir kereliğine yeni yapıya çevrilir.
    fun load(): AppData =
        (prefs.getString("data", null) ?: prefs.getString("logs", null))
            ?.let { runCatching { fromJson(it) }.getOrNull() } ?: AppData()

    fun save(data: AppData) {
        prefs.edit().putString("data", toJson(data)).apply()
    }

    companion object {
        fun toJson(d: AppData): String = JSONObject()
            .put("v", 2)
            .put("history", JSONArray().apply {
                d.history.forEach {
                    put(JSONObject().put("id", it.id).put("ex", it.exercise).put("date", it.date).put("kg", it.kg))
                }
            })
            .put("sessions", JSONArray().apply {
                d.sessions.forEach { put(JSONObject().put("id", it.id).put("w", it.workout).put("date", it.date)) }
            })
            .toString()

        fun fromJson(s: String): AppData {
            if (s.trimStart().startsWith("[")) return fromV1(JSONArray(s))
            val o = JSONObject(s)
            val h = o.getJSONArray("history")
            val ss = o.getJSONArray("sessions")
            return AppData(
                history = (0 until h.length()).map { i ->
                    val e = h.getJSONObject(i)
                    WeightChange(e.getLong("id"), e.getString("ex"), e.getString("date"), e.getDouble("kg"))
                },
                sessions = (0 until ss.length()).map { i ->
                    val e = ss.getJSONObject(i)
                    Session(e.getLong("id"), e.getInt("w"), e.getString("date"))
                },
            )
        }

        /** İlk sürüm her sete kilo+tekrar tutuyordu; her kaydın en ağır seti kilo geçmişine dönüşür. */
        private fun fromV1(a: JSONArray): AppData {
            data class Old(val id: Long, val ex: String, val date: String, val kg: Double)
            val olds = (0 until a.length()).mapNotNull { i ->
                val o = a.getJSONObject(i)
                val sets = o.getJSONArray("sets")
                val best = (0 until sets.length()).maxOfOrNull { sets.getJSONObject(it).getDouble("kg") }
                    ?: return@mapNotNull null
                Old(o.getLong("id"), o.getString("ex"), o.getString("date"), best)
            }
            val history = olds.groupBy { it.ex }.flatMap { (_, list) ->
                var prev: Double? = null
                list.sortedWith(compareBy({ it.date }, { it.id })).mapNotNull { e ->
                    if (prev != null && same(prev!!, e.kg)) null
                    else WeightChange(e.id, e.ex, e.date, e.kg).also { prev = e.kg }
                }
            }
            return AppData(history = history)
        }
    }
}

fun fmt(d: Double): String =
    String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')

fun change(cur: Double, prev: Double?): Change? {
    if (prev == null) return null
    val dk = cur - prev
    if (abs(dk) < 0.001) return Change("aynı", 0)
    return Change((if (dk > 0) "+" else "−") + fmt(abs(dk)) + " kg", if (dk > 0) 1 else -1)
}
