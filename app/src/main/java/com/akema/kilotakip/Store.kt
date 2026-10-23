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

/** Her antrenmanın sonundaki kardiyo; kilo gibi kayıtlı kalır, değişince güncellenir. */
data class Cardio(val speed: Double?, val incline: Double?, val minutes: Int?) {
    val text: String
        get() = listOfNotNull(
            speed?.let { "${fmt(it)} km/s" },
            incline?.let { "%${fmt(it)} eğim" },
            minutes?.let { "$it dk" },
        ).joinToString(" · ")
}

/** Vücut ölçüsü kaydı; boş bırakılan alan null kalır, güncel değer her alanın son dolu kaydıdır. */
data class BodyEntry(val id: Long, val date: String, val weight: Double?, val waist: Double?, val height: Double?)

fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class AppData(
    val history: List<WeightChange> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val cardio: Cardio? = null,
    val body: List<BodyEntry> = emptyList(),
    /** Uygulamanın ilk açıldığı gün; kaç haftadır kullanıldığı buradan sayılır. */
    val start: String? = null,
    val goalWeight: Double = GOAL_WEIGHT,
    val goalWaist: Double = GOAL_WAIST,
) {
    private fun bodySorted() = body.sortedWith(compareBy({ it.date }, { it.id }))

    fun firstBodyWeight() = bodySorted().firstOrNull { it.weight != null }?.weight
    fun firstWaist() = bodySorted().firstOrNull { it.waist != null }?.waist

    fun bodyWeight() = bodySorted().lastOrNull { it.weight != null }?.weight
    fun waist() = bodySorted().lastOrNull { it.waist != null }?.waist
    fun height() = bodySorted().lastOrNull { it.height != null }?.height

    fun bodySeries(pick: (BodyEntry) -> Double?): List<Pair<String, Double>> =
        bodySorted().mapNotNull { e -> pick(e)?.let { e.date to it } }

    /** Aynı gün tekrar girilirse o günün kaydı güncellenir. */
    fun addBody(weight: Double?, waist: Double?, height: Double?): AppData {
        val today = LocalDate.now().toString()
        return copy(body = body.filterNot { it.date == today } + BodyEntry(System.currentTimeMillis(), today, weight, waist, height))
    }

    fun firstDay(): LocalDate =
        (listOfNotNull(start) + sessions.map { it.date } + history.map { it.date }).minOrNull()
            ?.let { LocalDate.parse(it) } ?: LocalDate.now()

    /** İlk haftadan bu haftaya kadar her hafta (Pazartesi başlangıcı) ve tamamlanan antrenman sayısı, yeniden eskiye. */
    fun weeks(): List<Pair<LocalDate, Int>> {
        val perWeek = sessions.groupingBy { weekStart(LocalDate.parse(it.date)) }.eachCount()
        val current = weekStart(LocalDate.now())
        return generateSequence(weekStart(firstDay())) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(current) }
            .map { it to (perWeek[it] ?: 0) }
            .toList()
            .asReversed()
    }

    /** Hedefi tamamlanmış art arda hafta sayısı; bu hafta henüz bitmediği için eksikse seriyi bozmaz. */
    fun streak(): Int {
        val w = weeks()
        var n = if ((w.firstOrNull()?.second ?: 0) >= WEEKLY_TARGET) 1 else 0
        for ((_, count) in w.drop(1)) {
            if (count >= WEEKLY_TARGET) n++ else break
        }
        return n
    }

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
        val start = weekStart(LocalDate.now())
        return sessions.filter { !LocalDate.parse(it.date).isBefore(start) }
    }
}

private fun same(a: Double, b: Double) = abs(a - b) < 0.001

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    // "logs" ilk sürümün verisi; varsa bir kereliğine yeni yapıya çevrilir.
    fun load(): AppData {
        val d = (prefs.getString("data", null) ?: prefs.getString("logs", null))
            ?.let { runCatching { fromJson(it) }.getOrNull() } ?: AppData()
        if (d.start != null) return d
        return d.copy(start = d.firstDay().toString()).also { save(it) }
    }

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
            .apply {
                d.cardio?.let { c ->
                    put("cardio", JSONObject().apply {
                        c.speed?.let { put("s", it) }
                        c.incline?.let { put("i", it) }
                        c.minutes?.let { put("m", it) }
                    })
                }
                d.start?.let { put("start", it) }
                put("gw", d.goalWeight)
                put("gb", d.goalWaist)
                put("body", JSONArray().apply {
                    d.body.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("date", b.date)
                            b.weight?.let { put("w", it) }
                            b.waist?.let { put("bel", it) }
                            b.height?.let { put("boy", it) }
                        })
                    }
                })
            }
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
                cardio = o.optJSONObject("cardio")?.let { c ->
                    Cardio(
                        if (c.has("s")) c.getDouble("s") else null,
                        if (c.has("i")) c.getDouble("i") else null,
                        if (c.has("m")) c.getInt("m") else null,
                    )
                },
                body = o.optJSONArray("body")?.let { b ->
                    (0 until b.length()).map { i ->
                        val e = b.getJSONObject(i)
                        fun d(k: String) = if (e.has(k)) e.getDouble(k) else null
                        BodyEntry(e.getLong("id"), e.getString("date"), d("w"), d("bel"), d("boy"))
                    }
                } ?: emptyList(),
                start = o.optString("start").ifEmpty { null },
                goalWeight = o.optDouble("gw", GOAL_WEIGHT),
                goalWaist = o.optDouble("gb", GOAL_WAIST),
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
