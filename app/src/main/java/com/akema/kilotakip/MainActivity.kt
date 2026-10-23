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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Bg = Color(0xFF0D1330)
private val CardBg = Color(0xFF18224A)
private val Muted = Color(0xFFA9B3D9)
private val Up = Color(0xFF3DD68C)
private val Down = Color(0xFFFF6B6B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7C9BFF),
                    onPrimary = Color(0xFF0B1230),
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
    var logs by remember { mutableStateOf(store.load()) }
    val update: (List<LogEntry>) -> Unit = { logs = it; store.save(it) }
    var day by rememberSaveable { mutableIntStateOf(todayIndex()) }
    var openEx by rememberSaveable { mutableIntStateOf(-1) }

    if (openEx >= 0) {
        val ex = PROGRAM[day].exercises[openEx]
        BackHandler { openEx = -1 }
        ExerciseScreen(
            ex = ex,
            hist = logs.filter { it.exercise == ex.name }.sortedBy { it.date },
            onBack = { openEx = -1 },
            onSave = { sets ->
                val today = LocalDate.now().toString()
                update(
                    logs.filterNot { it.exercise == ex.name && it.date == today } +
                        LogEntry(System.currentTimeMillis(), ex.name, today, sets)
                )
            },
            onDelete = { e -> update(logs.filterNot { it.id == e.id }) },
        )
    } else {
        DayScreen(
            day = day,
            onDay = { day = it },
            logs = logs,
            onOpen = { openEx = it },
            onImport = update,
        )
    }
}

@Composable
fun DayScreen(
    day: Int,
    onDay: (Int) -> Unit,
    logs: List<LogEntry>,
    onOpen: (Int) -> Unit,
    onImport: (List<LogEntry>) -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var importDialog by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()

    Scaffold(
        containerColor = Bg,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Kilo Takip", fontWeight = FontWeight.Bold) },
                    actions = {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menü") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Yedeği paylaş") },
                                    onClick = { menu = false; shareBackup(context, logs) },
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
                ScrollableTabRow(selectedTabIndex = day, edgePadding = 8.dp, containerColor = Bg) {
                    PROGRAM.forEachIndexed { i, d ->
                        Tab(selected = i == day, onClick = { onDay(i) }, text = { Text(d.name) })
                    }
                }
            }
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 12.dp,
                bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(PROGRAM[day].exercises) { i, ex ->
                val hist = logs.filter { it.exercise == ex.name }.sortedBy { it.date }
                ExerciseCard(ex, hist, done = hist.lastOrNull()?.date == today) { onOpen(i) }
            }
        }
    }

    if (importDialog) {
        ImportDialog(
            onDismiss = { importDialog = false },
            onImport = { onImport(it); importDialog = false },
        )
    }
}

@Composable
fun ExerciseCard(ex: Exercise, hist: List<LogEntry>, done: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                if (done) Icon(Icons.Default.Check, "Bugün yapıldı", tint = Up)
            }
            Text(ex.label, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            val last = hist.lastOrNull()
            if (last == null) {
                Text("Henüz kayıt yok", color = Muted, fontSize = 14.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${shortDate(last.date)}:  ${setsText(last.sets)}",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    ChangeBadge(change(last, hist.getOrNull(hist.size - 2)))
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

@Composable
fun ExerciseScreen(
    ex: Exercise,
    hist: List<LogEntry>,
    onBack: () -> Unit,
    onSave: (List<SetEntry>) -> Unit,
    onDelete: (LogEntry) -> Unit,
) {
    val context = LocalContext.current
    val last = hist.lastOrNull()
    // Son kayıttaki değerlerle doldurulur, değiştirip kaydetmek yeterli.
    val kg = remember(ex.name) {
        mutableStateListOf<String>().apply {
            repeat(ex.sets) { i -> add(last?.sets?.getOrNull(i)?.kg?.let(::fmt) ?: "") }
        }
    }
    val reps = remember(ex.name) {
        mutableStateListOf<String>().apply {
            repeat(ex.sets) { i -> add(last?.sets?.getOrNull(i)?.reps?.toString() ?: "") }
        }
    }
    var toDelete by remember { mutableStateOf<LogEntry?>(null) }
    val rev = hist.asReversed()

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(ex.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            item { Text(ex.label, color = Muted, fontSize = 14.sp) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bugün", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        for (i in 0 until ex.sets) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("${i + 1}. set", color = Muted, modifier = Modifier.width(48.dp))
                                OutlinedTextField(
                                    value = kg[i],
                                    onValueChange = { v -> kg[i] = v.filter { it.isDigit() || it == ',' || it == '.' }.take(6) },
                                    label = { Text("kg") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = reps[i],
                                    onValueChange = { v -> reps[i] = v.filter { it.isDigit() }.take(3) },
                                    label = { Text("tekrar") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val sets = (0 until ex.sets).mapNotNull { i ->
                                    val k = kg[i].replace(',', '.').toDoubleOrNull()
                                    val r = reps[i].toIntOrNull()
                                    if (k != null && r != null) SetEntry(k, r) else null
                                }
                                if (sets.isEmpty()) {
                                    Toast.makeText(context, "En az bir set gir", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSave(sets)
                                    Toast.makeText(context, "Kaydedildi", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Kaydet") }
                    }
                }
            }
            if (hist.size >= 2) item { ProgressChart(hist) }
            item {
                Text(
                    if (hist.isEmpty()) "Henüz kayıt yok" else "Geçmiş",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            itemsIndexed(rev, key = { _, e -> e.id }) { idx, e ->
                Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
                    Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(longDate(e.date), color = Muted, fontSize = 13.sp)
                            Text(setsText(e.sets), fontSize = 15.sp)
                        }
                        ChangeBadge(change(e, rev.getOrNull(idx + 1)))
                        IconButton(onClick = { toDelete = e }) {
                            Icon(Icons.Default.Delete, "Sil", tint = Muted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("${longDate(e.date)}: ${setsText(e.sets)}") },
            confirmButton = { TextButton(onClick = { onDelete(e); toDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
fun ProgressChart(hist: List<LogEntry>) {
    val pts = hist.takeLast(20).map { it.best().kg }
    val color = MaterialTheme.colorScheme.primary
    Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text("En iyi set (son ${pts.size} kayıt)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${fmt(pts.min())} – ${fmt(pts.max())} kg", color = Muted, fontSize = 13.sp)
            }
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 12.dp, start = 6.dp, end = 6.dp)
            ) {
                val min = pts.min()
                val range = (pts.max() - min).takeIf { it > 0 } ?: 1.0
                val stepX = size.width / (pts.size - 1)
                val path = Path()
                pts.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = size.height - ((v - min) / range * size.height * 0.85 + size.height * 0.075).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    drawCircle(color, 4.dp.toPx(), Offset(x, y))
                }
                drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            }
        }
    }
}

@Composable
fun ImportDialog(onDismiss: () -> Unit, onImport: (List<LogEntry>) -> Unit) {
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
                        Toast.makeText(context, "${it.size} kayıt yüklendi", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { Toast.makeText(context, "Yedek okunamadı", Toast.LENGTH_SHORT).show() }
            }) { Text("Yükle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

private fun shareBackup(context: Context, logs: List<LogEntry>) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, Store.toJson(logs))
    }
    context.startActivity(Intent.createChooser(send, "Yedeği paylaş"))
}

private val TR = Locale("tr")
private fun shortDate(d: String) = LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMM", TR))
private fun longDate(d: String) = LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR))
