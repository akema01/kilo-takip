@file:OptIn(ExperimentalMaterial3Api::class)

package com.akema.kilotakip

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Bg = Color(0xFF0D1330)
private val CardBg = Color(0xFF18224A)
private val Muted = Color(0xFFA9B3D9)
private val Up = Color(0xFF3DD68C)
private val Down = Color(0xFFFF6B6B)
private val Fire = Color(0xFFFFA94D)
private const val STEP = 2.5

/** Programdaki bütün hareketler, ilk geçtikleri sırayla ve tekrarsız. */
private val ALL_EXERCISES = PROGRAM.flatMap { w -> w.exercises.map { it.name } }.distinct()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C9BFF),
                    onPrimary = Color(0xFF0B1230),
                    secondaryContainer = Color(0xFF2A3A78),
                    onSecondaryContainer = Color.White,
                    background = Bg,
                    onBackground = Color(0xFFE8EBF7),
                    surface = Bg,
                    onSurface = Color(0xFFE8EBF7),
                    onSurfaceVariant = Muted,
                )
            ) { App(store) }
        }
    }
}

@Composable
fun App(store: Store) {
    var data by remember { mutableStateOf(store.load()) }
    val update: (AppData) -> Unit = { data = it; store.save(it) }
    var running by rememberSaveable { mutableIntStateOf(-1) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var progress by rememberSaveable { mutableStateOf(false) }
    val setWeight: (String, Double) -> Unit = { ex, kg -> update(data.setWeight(ex, kg)) }

    when {
        running >= 0 -> WorkoutScreen(
            workout = PROGRAM[running],
            step = step,
            data = data,
            onStep = { step = it },
            onWeight = setWeight,
            onFinish = { cardio ->
                update(data.finish(running, cardio))
                running = -1
            },
            onExit = { running = -1 },
        )
        detail != null -> {
            val name = detail!!
            BackHandler { detail = null }
            DetailScreen(
                exercise = name,
                hist = data.historyOf(name),
                onBack = { detail = null },
                onWeight = setWeight,
                onDelete = { c -> update(data.copy(history = data.history - c)) },
            )
        }
        progress -> {
            BackHandler { progress = false }
            ProgressScreen(data = data, onBack = { progress = false }, onOpen = { detail = it })
        }
        else -> HomeScreen(
            data = data,
            onStart = { running = it; step = 0 },
            onOpen = { detail = it },
            onProgress = { progress = true },
            onImport = update,
        )
    }
}

@Composable
fun HomeScreen(
    data: AppData,
    onStart: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onProgress: () -> Unit,
    onImport: (AppData) -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf(false) }
    val week = data.thisWeek()
    val doneThisWeek = week.map { it.workout }.toSet()
    val streak = data.streak()

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("Kilo Takip", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onProgress) { Text("Grafik") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Yedeği paylaş") },
                                onClick = { menu = false; shareBackup(context, data) },
                            )
                            DropdownMenuItem(
                                text = { Text("Yedek yükle") },
                                onClick = { menu = false; importDialog = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        bottomBar = {
            Button(
                onClick = { chooser = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(56.dp),
            ) { Text("Antrenmanı başlat", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = pad.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 30.sp)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (streak > 0) "$streak haftalık seri" else "Seri henüz başlamadı",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (streak > 0) Fire else MaterialTheme.colorScheme.onSurface,
                                )
                                Text("Her hafta $WEEKLY_TARGET antrenman = seri devam", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Bu hafta", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                "${week.size}/$WEEKLY_TARGET",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (week.size >= WEEKLY_TARGET) Up else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (week.size.toFloat() / WEEKLY_TARGET).coerceAtMost(1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (week.size >= WEEKLY_TARGET) Up else MaterialTheme.colorScheme.primary,
                        )
                        if (week.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                week.sortedBy { it.id }.joinToString("  ·  ") {
                                    val name = PROGRAM.getOrNull(it.workout)?.name?.removePrefix("Antrenman ") ?: "?"
                                    "$name ${shortDate(it.date)}" + if (it.cardio != null) " 🏃" else ""
                                },
                                color = Muted,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
            itemsIndexed(PROGRAM) { wi, w ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column {
                        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(w.focus, color = Muted, fontSize = 13.sp)
                            }
                            if (wi in doneThisWeek) Icon(Icons.Default.Check, "Bu hafta yapıldı", tint = Up)
                        }
                        w.exercises.forEach { ex ->
                            HorizontalDivider(color = Bg.copy(alpha = 0.6f))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpen(ex.name) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.name, fontSize = 15.sp)
                                    Text("${ex.sets}x${ex.reps} · ${ex.note}", color = Muted, fontSize = 12.sp)
                                }
                                Text(
                                    data.weight(ex.name)?.let { "${fmt(it)} kg" } ?: "—",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (chooser) {
        AlertDialog(
            onDismissRequest = { chooser = false },
            title = { Text("Hangi antrenman?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PROGRAM.forEachIndexed { i, w ->
                        val done = i in doneThisWeek
                        Surface(
                            color = if (done) CardBg.copy(alpha = 0.5f) else CardBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chooser = false; onStart(i) },
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(w.name, fontWeight = FontWeight.SemiBold, color = if (done) Muted else Color.Unspecified)
                                    Text(w.focus, color = Muted, fontSize = 13.sp)
                                }
                                if (done) Icon(Icons.Default.Check, "Bu hafta yapıldı", tint = Up)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooser = false }) { Text("Vazgeç") } },
        )
    }

    if (importDialog) {
        ImportDialog(
            onDismiss = { importDialog = false },
            onImport = { onImport(it); importDialog = false },
        )
    }
}

private fun parseNum(s: String): Double? = s.replace(',', '.').toDoubleOrNull()
private fun numFilter(v: String) = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6)

@Composable
fun KgEditor(kg: String, onChange: (String) -> Unit) {
    fun bump(delta: Double) = onChange(fmt(((parseNum(kg) ?: 0.0) + delta).coerceAtLeast(0.0)))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilledTonalButton(onClick = { bump(-STEP) }, modifier = Modifier.height(56.dp)) {
            Text("−${fmt(STEP)}", fontSize = 16.sp)
        }
        OutlinedTextField(
            value = kg,
            onValueChange = { onChange(numFilter(it)) },
            suffix = { Text("kg") },
            placeholder = { Text("kilo") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(onClick = { bump(STEP) }, modifier = Modifier.height(56.dp)) {
            Text("+${fmt(STEP)}", fontSize = 16.sp)
        }
    }
}

@Composable
fun WorkoutScreen(
    workout: Workout,
    step: Int,
    data: AppData,
    onStep: (Int) -> Unit,
    onWeight: (String, Double) -> Unit,
    onFinish: (Cardio?) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val n = workout.exercises.size
    // Hareketlerden sonra son adım kardiyo.
    val isCardio = step == n
    val ex = workout.exercises.getOrNull(step)
    var kg by remember(step) { mutableStateOf(ex?.let { data.weight(it.name) }?.let(::fmt) ?: "") }
    var confirmExit by remember { mutableStateOf(false) }

    val lastCardio = remember { data.lastCardio() }
    var cardioDone by rememberSaveable { mutableStateOf(true) }
    var speed by rememberSaveable { mutableStateOf(lastCardio?.speed?.let(::fmt) ?: "") }
    var incline by rememberSaveable { mutableStateOf(lastCardio?.incline?.let(::fmt) ?: "") }
    var minutes by rememberSaveable { mutableStateOf(lastCardio?.minutes?.toString() ?: "") }

    // Girilen kilo kayıtlıdan farklıysa kaydeder; boş bırakılırsa kayıtlı kilo olduğu gibi kalır.
    fun commit(): Boolean {
        if (ex == null || kg.isBlank()) return true
        val v = parseNum(kg)
        if (v == null) {
            Toast.makeText(context, "Kilo geçersiz", Toast.LENGTH_SHORT).show()
            return false
        }
        onWeight(ex.name, v)
        return true
    }

    BackHandler { confirmExit = true }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(workout.name) },
                navigationIcon = {
                    IconButton(onClick = { confirmExit = true }) { Icon(Icons.Default.Close, "Çık") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 20.dp),
        ) {
            Text(if (isCardio) "Son adım" else "Hareket ${step + 1}/$n", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / (n + 1) },
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(28.dp))
                if (ex != null) {
                    Text(ex.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(ex.label, color = Muted, fontSize = 16.sp)
                    Spacer(Modifier.height(32.dp))
                    KgEditor(kg) { kg = it }
                    Spacer(Modifier.height(12.dp))
                    val h = data.historyOf(ex.name)
                    val lastTwo = h.takeLast(2)
                    Text(
                        when {
                            h.isEmpty() -> "İlk kez giriliyor"
                            lastTwo.size == 2 -> "Son değişiklik: ${fmt(lastTwo[0].kg)} → ${fmt(lastTwo[1].kg)} kg (${shortDate(lastTwo[1].date)})"
                            else -> "${shortDate(h.last().date)} tarihinden beri ${fmt(h.last().kg)} kg"
                        },
                        color = Muted,
                        fontSize = 14.sp,
                    )
                } else {
                    Text("Kardiyo", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Kardiyo yaptım", fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Switch(checked = cardioDone, onCheckedChange = { cardioDone = it })
                    }
                    if (cardioDone) {
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CardioField("Hız", "km/s", speed, KeyboardType.Decimal) { speed = numFilter(it) }
                            CardioField("Eğim", "%", incline, KeyboardType.Decimal) { incline = numFilter(it) }
                            CardioField("Süre", "dk", minutes, KeyboardType.Number) { minutes = it.filter(Char::isDigit).take(3) }
                        }
                        if (lastCardio != null) {
                            Spacer(Modifier.height(10.dp))
                            Text("Geçen sefer: ${cardioText(lastCardio)}", color = Muted, fontSize = 14.sp)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { if (commit()) onStep(step - 1) },
                    enabled = step > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                ) { Text("Geri", fontSize = 16.sp) }
                Button(
                    onClick = {
                        if (isCardio) {
                            val c = if (cardioDone) Cardio(parseNum(speed), parseNum(incline), minutes.toIntOrNull()) else null
                            onFinish(c)
                            Toast.makeText(context, "${workout.name} tamamlandı 💪", Toast.LENGTH_SHORT).show()
                        } else if (commit()) onStep(step + 1)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                ) { Text(if (isCardio) "Bitir" else "İleri", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Antrenmandan çık?") },
            text = { Text("Şimdiye kadar girdiğin kilolar kayıtlı kalır, ama antrenman bu haftaya sayılmaz.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; commit(); onExit() }) { Text("Çık") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Devam et") } },
        )
    }
}

@Composable
private fun CardioField(label: String, unit: String, value: String, type: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun DetailScreen(
    exercise: String,
    hist: List<WeightChange>,
    onBack: () -> Unit,
    onWeight: (String, Double) -> Unit,
    onDelete: (WeightChange) -> Unit,
) {
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<WeightChange?>(null) }
    val current = hist.lastOrNull()?.kg
    var kg by remember(current) { mutableStateOf(current?.let(::fmt) ?: "") }
    val rev = hist.asReversed()
    val usedIn = PROGRAM.filter { w -> w.exercises.any { it.name == exercise } }
        .joinToString(", ") { w -> w.name.removePrefix("Antrenman ") + ": " + w.exercises.first { it.name == exercise }.let { "${it.sets}x${it.reps}" } }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(exercise, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Antrenman $usedIn", color = Muted, fontSize = 14.sp) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            current?.let { "Şu an: ${fmt(it)} kg" } ?: "Henüz kilo girilmedi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        KgEditor(kg) { kg = it }
                        Button(
                            onClick = {
                                val v = parseNum(kg)
                                if (v == null) {
                                    Toast.makeText(context, "Kilo geçersiz", Toast.LENGTH_SHORT).show()
                                } else {
                                    onWeight(exercise, v)
                                    Toast.makeText(context, "Kaydedildi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Kiloyu kaydet") }
                    }
                }
            }
            if (hist.isNotEmpty()) item { WeightChart(hist) }
            if (hist.isNotEmpty()) item {
                Text("Kilo değişiklikleri", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
            }
            itemsIndexed(rev, key = { _, c -> c.id }) { idx, c ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(longDate(c.date), color = Muted, fontSize = 13.sp)
                            Text("${fmt(c.kg)} kg", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        ChangeBadge(change(c.kg, rev.getOrNull(idx + 1)?.kg))
                        IconButton(onClick = { toDelete = c }) {
                            Icon(Icons.Default.Delete, "Sil", tint = Muted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("${longDate(c.date)}: ${fmt(c.kg)} kg") },
            confirmButton = { TextButton(onClick = { onDelete(c); toDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
        )
    }
}

private const val CARDIO = "Kardiyo"

@Composable
fun ProgressScreen(data: AppData, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var selected by rememberSaveable {
        mutableStateOf(ALL_EXERCISES.firstOrNull { data.historyOf(it).isNotEmpty() } ?: ALL_EXERCISES.first())
    }
    val cardioSessions = data.sessions.filter { it.cardio != null }.sortedByDescending { it.id }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("İlerleme") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (ALL_EXERCISES + CARDIO).forEach { name ->
                        FilterChip(selected = name == selected, onClick = { selected = name }, label = { Text(name) })
                    }
                }
            }
            if (selected == CARDIO) {
                if (cardioSessions.isEmpty()) item { Text("Henüz kardiyo kaydı yok", color = Muted) }
                items(cardioSessions, key = { it.id }) { s ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("${longDate(s.date)} · ${PROGRAM.getOrNull(s.workout)?.name ?: ""}", color = Muted, fontSize = 13.sp)
                            Text(cardioText(s.cardio!!), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                val hist = data.historyOf(selected)
                if (hist.isEmpty()) {
                    item { Text("$selected için henüz kilo girilmedi", color = Muted) }
                } else {
                    item {
                        val first = hist.first()
                        val last = hist.last()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${fmt(first.kg)} kg → ${fmt(last.kg)} kg",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            ChangeBadge(change(last.kg, first.kg))
                        }
                    }
                    item { WeightChart(hist) }
                }
            }
            item {
                Text("Tüm hareketler", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column {
                        ALL_EXERCISES.forEachIndexed { i, name ->
                            if (i > 0) HorizontalDivider(color = Bg.copy(alpha = 0.6f))
                            val h = data.historyOf(name)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpen(name) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontSize = 15.sp)
                                    Text(
                                        if (h.isEmpty()) "—" else "${fmt(h.first().kg)} → ${fmt(h.last().kg)} kg",
                                        color = Muted,
                                        fontSize = 13.sp,
                                    )
                                }
                                if (h.size >= 2) ChangeBadge(change(h.last().kg, h.first().kg))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeBadge(c: Change?) {
    if (c == null) return
    val color = when (c.sign) {
        1 -> Up
        -1 -> Down
        else -> Muted
    }
    val arrow = when (c.sign) {
        1 -> "▲ "
        -1 -> "▼ "
        else -> ""
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
        Text(
            arrow + c.text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Kilonun zamana göre basamaklı grafiği: kilo bir sonraki değişikliğe kadar sabit kalır, çizgi bugüne uzanır. */
@Composable
fun WeightChart(hist: List<WeightChange>) {
    val color = MaterialTheme.colorScheme.primary
    val first = LocalDate.parse(hist.first().date)
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(first, today).coerceAtLeast(1).toFloat()
    val min = hist.minOf { it.kg }
    val max = hist.maxOf { it.kg }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text("Kilo zaman içinde", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${fmt(min)} – ${fmt(max)} kg", color = Muted, fontSize = 13.sp)
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 14.dp, bottom = 6.dp, start = 6.dp, end = 6.dp)
            ) {
                val range = (max - min).takeIf { it > 0 } ?: 1.0
                fun x(date: String) = ChronoUnit.DAYS.between(first, LocalDate.parse(date)) / days * size.width
                fun y(v: Double) = size.height - ((v - min) / range * size.height * 0.85 + size.height * 0.075).toFloat()
                listOf(min, max).forEach {
                    drawLine(Muted.copy(alpha = 0.2f), Offset(0f, y(it)), Offset(size.width, y(it)), 1.dp.toPx())
                }
                val path = Path()
                hist.forEachIndexed { i, c ->
                    if (i == 0) path.moveTo(x(c.date), y(c.kg))
                    else {
                        path.lineTo(x(c.date), y(hist[i - 1].kg))
                        path.lineTo(x(c.date), y(c.kg))
                    }
                }
                path.lineTo(size.width, y(hist.last().kg))
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
                hist.forEach { drawCircle(color, 4.dp.toPx(), Offset(x(it.date), y(it.kg))) }
            }
            Row {
                Text(shortDate(hist.first().date), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Bugün", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (AppData) -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yedek yükle") },
        text = {
            Column {
                Text("Paylaştığın yedek metnini yapıştır. Mevcut kayıtların yerini alır.", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, maxLines = 6, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching { Store.fromJson(text.trim()) }
                    .onSuccess {
                        onImport(it)
                        Toast.makeText(context, "Yedek yüklendi", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { Toast.makeText(context, "Yedek okunamadı", Toast.LENGTH_SHORT).show() }
            }) { Text("Yükle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

private fun shareBackup(context: Context, data: AppData) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, Store.toJson(data))
    }
    context.startActivity(Intent.createChooser(send, "Yedeği paylaş"))
}

private val TR = Locale("tr")
private fun shortDate(d: String) = LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMM", TR))
private fun longDate(d: String) = LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR))
