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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

private val Bg = Color(0xFF0D1330)
private val CardBg = Color(0xFF18224A)
private val Muted = Color(0xFFA9B3D9)
private val Up = Color(0xFF3DD68C)
private val Down = Color(0xFFFF6B6B)
private val Fire = Color(0xFFFFA94D)
private const val STEP = 2.5

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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // 0: yok, 1: antrenman bitti, 2: haftanın 4. antrenmanı bitti
    var celebrate by rememberSaveable { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
    when {
        running >= 0 -> WorkoutScreen(
            workout = PROGRAM[running],
            step = step,
            data = data,
            onStep = { step = it },
            onWeight = { ex, kg -> update(data.setWeight(ex, kg)) },
            onCardio = { update(data.copy(cardio = it)) },
            onFinish = {
                update(data.finish(running))
                running = -1
                celebrate = if (data.thisWeek().size == WEEKLY_TARGET) 2 else 1
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
                onWeight = { ex, kg -> update(data.setWeight(ex, kg)) },
                onDelete = { c -> update(data.copy(history = data.history - c)) },
            )
        }
        tab == 1 -> TrackScreen(
            data = data,
            tabBar = { TabBar(tab) { tab = it } },
            onBody = { w, b, h -> update(data.addBody(w, b, h)) },
            onGoals = { w, b -> update(data.copy(goalWeight = w, goalWaist = b)) },
            onOpen = { detail = it },
        )
        else -> HomeScreen(
            data = data,
            tabBar = { TabBar(tab) { tab = it } },
            onStart = { running = it; step = 0 },
            onOpen = { detail = it },
            onCardio = { update(data.copy(cardio = it)) },
            onImport = update,
        )
    }

    if (celebrate > 0) {
        val weekly = celebrate == 2
        val done = data.thisWeek().size
        Celebration(
            weekly = weekly,
            title = if (weekly) "Hafta tamamlandı!" else "Antrenman tamamlandı!",
            subtitle = if (weekly) "$WEEKLY_TARGET/$WEEKLY_TARGET · seri devam ediyor 🔥" else "Bu hafta $done/$WEEKLY_TARGET",
            onDone = { celebrate = 0 },
        )
    }
    }
}

@Composable
fun TabBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = CardBg) {
        NavigationBarItem(
            selected = tab == 1,
            onClick = { onTab(1) },
            icon = { Icon(Icons.Default.DateRange, null) },
            label = { Text("Takip") },
        )
        NavigationBarItem(
            selected = tab == 0,
            onClick = { onTab(0) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Antrenman") },
        )
    }
}

@Composable
fun HomeScreen(
    data: AppData,
    tabBar: @Composable () -> Unit,
    onStart: (Int) -> Unit,
    onOpen: (String) -> Unit,
    onCardio: (Cardio?) -> Unit,
    onImport: (AppData) -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }
    var cardioDialog by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val week = data.thisWeek()
    val doneThisWeek = week.map { it.workout }.toSet()

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("Antreman Takip", fontWeight = FontWeight.Bold) },
                actions = {
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
                            DropdownMenuItem(
                                text = { Text("Verileri sıfırla") },
                                onClick = { menu = false; confirmReset = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        bottomBar = {
            Column {
                Button(
                    onClick = { chooser = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(56.dp),
                ) { Text("Antrenmanı başlat", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                tabBar()
            }
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
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Bu hafta", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(
                                "${week.size}/$WEEKLY_TARGET",
                                fontSize = 26.sp,
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
                                    "${PROGRAM.getOrNull(it.workout)?.name?.removePrefix("Antrenman ") ?: "?"} ${shortDate(it.date)}"
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
                        HorizontalDivider(color = Bg.copy(alpha = 0.6f))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { cardioDialog = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Kardiyo", fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text(
                                data.cardio?.text?.ifEmpty { null } ?: "—",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
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

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Veriler sıfırlansın mı?") },
            text = { Text("Bütün kilolar, antrenmanlar, ölçüler ve seri silinir. Geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    onImport(AppData(start = LocalDate.now().toString()))
                    confirmReset = false
                }) { Text("Sıfırla") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Vazgeç") } },
        )
    }

    if (cardioDialog) {
        var speed by remember { mutableStateOf(data.cardio?.speed?.let(::fmt) ?: "") }
        var incline by remember { mutableStateOf(data.cardio?.incline?.let(::fmt) ?: "") }
        var minutes by remember { mutableStateOf(data.cardio?.minutes?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { cardioDialog = false },
            title = { Text("Kardiyo") },
            text = {
                CardioFields(speed, incline, minutes, { speed = it }, { incline = it }, { minutes = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    onCardio(cardioOf(speed, incline, minutes))
                    cardioDialog = false
                    Toast.makeText(context, "Kaydedildi", Toast.LENGTH_SHORT).show()
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { cardioDialog = false }) { Text("Vazgeç") } },
        )
    }
}

private fun decimalOnly(v: String) = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6)

private fun cardioOf(speed: String, incline: String, minutes: String): Cardio? =
    Cardio(
        speed.replace(',', '.').toDoubleOrNull(),
        incline.replace(',', '.').toDoubleOrNull(),
        minutes.toIntOrNull(),
    ).takeIf { it.speed != null || it.incline != null || it.minutes != null }

@Composable
fun CardioFields(
    speed: String,
    incline: String,
    minutes: String,
    onSpeed: (String) -> Unit,
    onIncline: (String) -> Unit,
    onMinutes: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = speed,
            onValueChange = { onSpeed(decimalOnly(it)) },
            label = { Text("Hız") },
            suffix = { Text("km/s") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = incline,
            onValueChange = { onIncline(decimalOnly(it)) },
            label = { Text("Eğim") },
            suffix = { Text("%") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = minutes,
            onValueChange = { v -> onMinutes(v.filter { it.isDigit() }.take(3)) },
            label = { Text("Süre") },
            suffix = { Text("dk") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun WorkoutScreen(
    workout: Workout,
    step: Int,
    data: AppData,
    onStep: (Int) -> Unit,
    onWeight: (String, Double) -> Unit,
    onCardio: (Cardio?) -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    // Hareketlerden sonraki son adım kardiyo; o adımda ex null.
    val ex = workout.exercises.getOrNull(step)
    val total = workout.exercises.size + 1
    val isLast = step == total - 1
    var kg by remember(step) { mutableStateOf(ex?.let { data.weight(it.name) }?.let(::fmt) ?: "") }
    var confirmExit by remember { mutableStateOf(false) }
    var speed by rememberSaveable { mutableStateOf(data.cardio?.speed?.let(::fmt) ?: "") }
    var incline by rememberSaveable { mutableStateOf(data.cardio?.incline?.let(::fmt) ?: "") }
    var minutes by rememberSaveable { mutableStateOf(data.cardio?.minutes?.toString() ?: "") }

    // Girilen kilo kayıtlıdan farklıysa kaydeder; boş bırakılırsa kayıtlı kilo olduğu gibi kalır.
    fun commit(): Boolean {
        if (ex == null) {
            cardioOf(speed, incline, minutes)?.let { if (it != data.cardio) onCardio(it) }
            return true
        }
        if (kg.isBlank()) return true
        val v = kg.replace(',', '.').toDoubleOrNull()
        if (v == null) {
            Toast.makeText(context, "Kilo geçersiz", Toast.LENGTH_SHORT).show()
            return false
        }
        onWeight(ex.name, v)
        return true
    }

    fun bump(delta: Double) {
        val cur = kg.replace(',', '.').toDoubleOrNull() ?: 0.0
        kg = fmt((cur + delta).coerceAtLeast(0.0))
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
            Text(if (ex == null) "Son adım" else "Hareket ${step + 1}/${total - 1}", color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(28.dp))
            if (ex == null) {
                Text("Kardiyo", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                CardioFields(speed, incline, minutes, { speed = it }, { incline = it }, { minutes = it })
            } else {
            Text(ex.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            Spacer(Modifier.height(8.dp))
            Text(ex.label, color = Muted, fontSize = 16.sp)
            Spacer(Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { bump(-STEP) }, modifier = Modifier.height(56.dp)) {
                    Text("−${fmt(STEP)}", fontSize = 16.sp)
                }
                OutlinedTextField(
                    value = kg,
                    onValueChange = { v -> kg = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
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
            Spacer(Modifier.height(12.dp))
            val h = data.historyOf(ex.name)
            val lastTwo = h.takeLast(2)
            Text(
                when {
                    h.isEmpty() -> "İlk kez giriliyor"
                    lastTwo.size == 2 -> "Son artış: ${fmt(lastTwo[0].kg)} → ${fmt(lastTwo[1].kg)} kg (${shortDate(lastTwo[1].date)})"
                    else -> "${shortDate(h.last().date)} tarihinden beri ${fmt(h.last().kg)} kg"
                },
                color = Muted,
                fontSize = 14.sp,
            )
            }

            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
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
                        if (commit()) {
                            // Bitince kutlama animasyonu App'te gösterilir.
                            if (isLast) onFinish() else onStep(step + 1)
                        }
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                ) { Text(if (isLast) "Bitir" else "İleri", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
fun DetailScreen(
    exercise: String,
    hist: List<WeightChange>,
    onBack: () -> Unit,
    onWeight: (String, Double) -> Unit,
    onDelete: (WeightChange) -> Unit,
) {
    val context = LocalContext.current
    var toDelete by remember { mutableStateOf<WeightChange?>(null) }
    val rev = hist.asReversed()
    val current = hist.lastOrNull()?.kg
    var kg by remember(current) { mutableStateOf(current?.let(::fmt) ?: "") }

    fun bump(delta: Double) {
        val cur = kg.replace(',', '.').toDoubleOrNull() ?: 0.0
        kg = fmt((cur + delta).coerceAtLeast(0.0))
    }

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
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            current?.let { "Şu an: ${fmt(it)} kg" } ?: "Henüz kilo girilmedi",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        // Antrenmanı beklemeden kiloyu buradan değiştirebilmek için.
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { bump(-STEP) }, modifier = Modifier.height(56.dp)) {
                                Text("−${fmt(STEP)}", fontSize = 16.sp)
                            }
                            OutlinedTextField(
                                value = kg,
                                onValueChange = { v -> kg = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
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
                        Button(
                            onClick = {
                                val v = kg.replace(',', '.').toDoubleOrNull()
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
            if (hist.size >= 2) item { TimeChart("Kilo", hist.map { it.date to it.kg }, "kg", step = true) }
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

/** Programdaki bütün hareketler, ilk geçtikleri sırayla ve tekrarsız. */
private val ALL_EXERCISES = PROGRAM.flatMap { w -> w.exercises.map { it.name } }.distinct()

@Composable
fun TrackScreen(
    data: AppData,
    tabBar: @Composable () -> Unit,
    onBody: (Double?, Double?, Double?) -> Unit,
    onGoals: (Double, Double) -> Unit,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    var bodyDialog by remember { mutableStateOf(false) }
    var goalDialog by remember { mutableStateOf(false) }
    var bodyChart by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable {
        mutableStateOf(ALL_EXERCISES.firstOrNull { data.historyOf(it).isNotEmpty() } ?: ALL_EXERCISES.first())
    }
    val streak = data.streak()
    val weeks = data.weeks()

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("Takip", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        bottomBar = tabBar,
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
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Flame(active = streak > 0)
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(
                                if (streak > 0) "$streak haftalık seri" else "Seri henüz başlamadı",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (streak > 0) Fire else MaterialTheme.colorScheme.onSurface,
                            )
                            Text("Her hafta $WEEKLY_TARGET antrenman = seri devam", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }

            item { SectionTitle("Vücut") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(16.dp)) {
                        Row {
                            Stat("Kilo", data.bodyWeight(), "kg", Modifier.weight(1f))
                            Stat("Bel", data.waist(), "cm", Modifier.weight(1f))
                            Stat("Boy", data.height() ?: HEIGHT_CM, "cm", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        FilledTonalButton(onClick = { bodyDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Ölçü gir")
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Hedefler", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { goalDialog = true }) { Text("Düzenle") }
                        }
                        GoalBar("Kilo", "kg", data.firstBodyWeight(), data.bodyWeight(), data.goalWeight, Bg, MaterialTheme.colorScheme.primary, Up, Muted)
                        GoalBar("Bel çevresi", "cm", data.firstWaist(), data.waist(), data.goalWaist, Bg, MaterialTheme.colorScheme.primary, Up, Muted)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = bodyChart == 0, onClick = { bodyChart = 0 }, label = { Text("Kilo") })
                    FilterChip(selected = bodyChart == 1, onClick = { bodyChart = 1 }, label = { Text("Bel") })
                }
            }
            item {
                val series = if (bodyChart == 0) data.bodySeries { it.weight } else data.bodySeries { it.waist }
                if (series.isEmpty()) Text("Henüz ölçü girilmedi", color = Muted, fontSize = 14.sp)
                else TimeChart(if (bodyChart == 0) "Kilo" else "Bel çevresi", series, if (bodyChart == 0) "kg" else "cm", step = false)
            }

            item { SectionTitle("Haftalar") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column {
                        Text(
                            "Uygulamayı ${weeks.size} haftadır kullanıyorsun",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                        )
                        weeks.forEachIndexed { i, (start, count) ->
                            HorizontalDivider(color = Bg.copy(alpha = 0.6f))
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (i == 0) "Bu hafta" else "${weeks.size - i}. hafta", fontSize = 15.sp)
                                    Text("${shortDate(start.toString())} – ${shortDate(start.plusDays(6).toString())}", color = Muted, fontSize = 12.sp)
                                }
                                Text(
                                    "$count/$WEEKLY_TARGET",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (count >= WEEKLY_TARGET) Up else MaterialTheme.colorScheme.onSurface,
                                )
                                if (count >= WEEKLY_TARGET) {
                                    Spacer(Modifier.size(6.dp))
                                    Icon(Icons.Default.Check, null, tint = Up, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Ağırlık grafiği") }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ALL_EXERCISES.forEach { name ->
                        FilterChip(selected = name == selected, onClick = { selected = name }, label = { Text(name) })
                    }
                }
            }
            item {
                val hist = data.historyOf(selected)
                if (hist.isEmpty()) {
                    Text("$selected için henüz kilo girilmedi", color = Muted, fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.clickable { onOpen(selected) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${fmt(hist.first().kg)} kg → ${fmt(hist.last().kg)} kg",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            ChangeBadge(change(hist.last().kg, hist.first().kg))
                        }
                        TimeChart(selected, hist.map { it.date to it.kg }, "kg", step = true)
                    }
                }
            }
        }
    }

    if (goalDialog) {
        var gw by remember { mutableStateOf(fmt(data.goalWeight)) }
        var gb by remember { mutableStateOf(fmt(data.goalWaist)) }
        AlertDialog(
            onDismissRequest = { goalDialog = false },
            title = { Text("Hedefler") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnitField("Hedef kilo", "kg", gw) { gw = decimalOnly(it) }
                    UnitField("Hedef bel çevresi", "cm", gb) { gb = decimalOnly(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = gw.replace(',', '.').toDoubleOrNull()
                    val b = gb.replace(',', '.').toDoubleOrNull()
                    if (w == null || b == null) {
                        Toast.makeText(context, "Hedefler geçersiz", Toast.LENGTH_SHORT).show()
                    } else {
                        onGoals(w, b)
                        goalDialog = false
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { goalDialog = false }) { Text("Vazgeç") } },
        )
    }

    if (bodyDialog) {
        var weight by remember { mutableStateOf(data.bodyWeight()?.let(::fmt) ?: "") }
        var waist by remember { mutableStateOf(data.waist()?.let(::fmt) ?: "") }
        var height by remember { mutableStateOf(fmt(data.height() ?: HEIGHT_CM)) }
        AlertDialog(
            onDismissRequest = { bodyDialog = false },
            title = { Text("Ölçü gir") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnitField("Kilo", "kg", weight) { weight = decimalOnly(it) }
                    UnitField("Bel çevresi", "cm", waist) { waist = decimalOnly(it) }
                    UnitField("Boy", "cm", height) { height = decimalOnly(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = weight.replace(',', '.').toDoubleOrNull()
                    val b = waist.replace(',', '.').toDoubleOrNull()
                    val h = height.replace(',', '.').toDoubleOrNull()
                    if (w == null && b == null && h == null) {
                        Toast.makeText(context, "En az bir ölçü gir", Toast.LENGTH_SHORT).show()
                    } else {
                        onBody(w, b, h)
                        bodyDialog = false
                        Toast.makeText(context, "Kaydedildi", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { bodyDialog = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Stat(label: String, value: Double?, unit: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value?.let { "${fmt(it)} $unit" } ?: "—", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UnitField(label: String, unit: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        suffix = { Text(unit) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Değerlerin zamana göre grafiği; yatay eksen gerçek tarihler, çizgi bugüne uzanır.
 * step: kaldırılan kilo gibi bir sonraki değişikliğe kadar sabit kalan değerler için basamaklı çizim.
 * Grafiğe dokununca en yakın noktanın tarihi ve değeri başlıkta gösterilir.
 */
@Composable
fun TimeChart(title: String, points: List<Pair<String, Double>>, unit: String, step: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val first = LocalDate.parse(points.first().first)
    val days = ChronoUnit.DAYS.between(first, LocalDate.now()).coerceAtLeast(1).toFloat()
    val min = points.minOf { it.second }
    val max = points.maxOf { it.second }
    // Gün cinsinden konum, 0..1 arası; dokunma ve çizim aynı hesabı kullanır.
    val xs = remember(points) { points.map { ChronoUnit.DAYS.between(first, LocalDate.parse(it.first)) / days } }
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                val sel = selected?.let { points.getOrNull(it) }
                if (sel != null) {
                    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            "${shortDate(sel.first)}: ${fmt(sel.second)} $unit",
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    Text("${fmt(min)} – ${fmt(max)} $unit", color = Muted, fontSize = 13.sp)
                }
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 14.dp, bottom = 6.dp, start = 6.dp, end = 6.dp)
                    .pointerInput(points) {
                        detectTapGestures { tap ->
                            val fx = tap.x / size.width
                            selected = xs.indices.minByOrNull { abs(xs[it] - fx) * 10_000 - it * 0.0001f }
                        }
                    }
            ) {
                val range = (max - min).takeIf { it > 0 } ?: 1.0
                fun x(date: String) = ChronoUnit.DAYS.between(first, LocalDate.parse(date)) / days * size.width
                fun y(v: Double) = size.height - ((v - min) / range * size.height * 0.85 + size.height * 0.075).toFloat()
                selected?.let { i ->
                    val sx = xs[i] * size.width
                    drawLine(color.copy(alpha = 0.5f), Offset(sx, 0f), Offset(sx, size.height), 1.5.dp.toPx())
                }
                listOf(min, max).forEach {
                    drawLine(Muted.copy(alpha = 0.2f), Offset(0f, y(it)), Offset(size.width, y(it)), 1.dp.toPx())
                }
                val path = Path()
                points.forEachIndexed { i, (d, v) ->
                    if (i == 0) path.moveTo(x(d), y(v))
                    else {
                        if (step) path.lineTo(x(d), y(points[i - 1].second))
                        path.lineTo(x(d), y(v))
                    }
                }
                path.lineTo(size.width, y(points.last().second))
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
                points.forEachIndexed { i, (d, v) ->
                    if (i == selected) {
                        drawCircle(Color.White, 7.dp.toPx(), Offset(x(d), y(v)))
                        drawCircle(color, 5.dp.toPx(), Offset(x(d), y(v)))
                    } else {
                        drawCircle(color, 4.dp.toPx(), Offset(x(d), y(v)))
                    }
                }
            }
            Row {
                Text(shortDate(points.first().first), color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("Bugün", color = Muted, fontSize = 12.sp)
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
