package com.akema.kilotakip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

val FireColor = Color(0xFFFFA94D)
private val ConfettiColors = listOf(
    Color(0xFF7C9BFF), Color(0xFF3DD68C), Color(0xFFFFA94D), Color(0xFFFF6B6B),
    Color(0xFFFFE066), Color(0xFFC084FC), Color.White,
)

private class Particle(
    val cx: Float, val cy: Float, val angle: Float, val speed: Float,
    val color: Color, val radius: Float, val delay: Float,
)

/**
 * Antrenman bitince patlayan konfeti. weekly = haftanın 4. antrenmanı: art arda havai fişek patlamaları ve kupa.
 * Dokununca ya da animasyon bitince kapanır.
 */
@Composable
fun Celebration(weekly: Boolean, title: String, subtitle: String, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 250f)) }
        progress.animateTo(1f, tween(if (weekly) 3800 else 2300, easing = LinearEasing))
        onDone()
    }
    val particles = remember {
        val rnd = Random(System.nanoTime())
        val bursts = if (weekly) {
            listOf(0.5f to 0.3f, 0.22f to 0.2f, 0.78f to 0.24f, 0.3f to 0.55f, 0.7f to 0.5f, 0.5f to 0.68f)
        } else {
            listOf(0.5f to 0.42f)
        }
        bursts.flatMapIndexed { i, (cx, cy) ->
            List(if (weekly) 60 else 110) {
                Particle(
                    cx, cy,
                    angle = rnd.nextFloat() * 2f * PI.toFloat(),
                    speed = 0.15f + rnd.nextFloat() * 0.5f,
                    color = ConfettiColors[rnd.nextInt(ConfettiColors.size)],
                    radius = 3f + rnd.nextFloat() * 5f,
                    delay = i * 0.1f,
                )
            }
        }
    }
    val t0 = progress.value
    val fade = if (t0 > 0.85f) (1f - t0) / 0.15f else 1f

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * fade))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val t = ((t0 - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (t <= 0f || t >= 1f) return@forEach
                val dist = p.speed * size.minDimension * (1f - (1f - t).pow(3))
                val x = p.cx * size.width + cos(p.angle) * dist
                val y = p.cy * size.height + sin(p.angle) * dist + t * t * size.height * 0.2f
                val alpha = (1f - t).coerceIn(0f, 1f)
                if (weekly) {
                    // Havai fişek izi: merkeze doğru sönen kuyruk.
                    val tail = 0.82f
                    drawLine(
                        p.color.copy(alpha = alpha * 0.5f),
                        Offset(p.cx * size.width + cos(p.angle) * dist * tail, p.cy * size.height + sin(p.angle) * dist * tail + t * t * size.height * 0.2f),
                        Offset(x, y),
                        strokeWidth = p.radius.dp.toPx() * 0.5f,
                    )
                }
                drawCircle(p.color.copy(alpha = alpha), p.radius.dp.toPx() * (1f - t * 0.5f), Offset(x, y))
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = fade
            },
        ) {
            Text(if (weekly) "🏆" else "💪", fontSize = if (weekly) 96.sp else 80.sp)
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Seri kartındaki titreyen alev ve yükselen kıvılcımlar; seri yoksa sönük durur. */
@Composable
fun Flame(active: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "flame")
    val scale by inf.animateFloat(0.9f, 1.14f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "scale")
    val tilt by inf.animateFloat(-7f, 7f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "tilt")
    val rise by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = LinearEasing)), label = "rise")
    Box(modifier.size(72.dp), contentAlignment = Alignment.Center) {
        if (active) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(listOf(FireColor.copy(alpha = 0.45f * scale), Color.Transparent)),
                    radius = size.minDimension / 2,
                    center = center,
                )
                repeat(8) { i ->
                    val p = (rise + i / 8f) % 1f
                    val x = size.width * (0.3f + 0.4f * ((i * 37) % 10) / 10f) + sin(p * 2f * PI.toFloat() + i) * 4.dp.toPx()
                    val y = size.height * (0.85f - p * 0.85f)
                    drawCircle(
                        if (i % 2 == 0) FireColor.copy(alpha = 1f - p) else Color(0xFFFFE066).copy(alpha = 1f - p),
                        (2.5f * (1f - p) + 1f).dp.toPx(),
                        Offset(x, y),
                    )
                }
            }
        }
        Text(
            "🔥",
            fontSize = 38.sp,
            modifier = Modifier.graphicsLayer {
                if (active) {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt
                } else {
                    alpha = 0.35f
                }
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        )
    }
}

/** Başlangıçtan hedefe ne kadar gidildiği (0..1); hedeften uzaklaşıldıysa 0. */
fun goalProgress(start: Double, current: Double, target: Double): Float {
    if (abs(start - target) < 0.001) return if (abs(current - target) < 0.05) 1f else 0f
    return ((start - current) / (start - target)).toFloat().coerceIn(0f, 1f)
}

/**
 * Hedef çubuğu: açılışta dolarak gelir, hedefe yaklaştıkça yeşile döner ve nabız gibi atar,
 * hedefe ulaşınca 🎯 zıplar.
 */
@Composable
fun GoalBar(label: String, unit: String, start: Double?, current: Double?, target: Double, track: Color, base: Color, done: Color, muted: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                (current?.let { "${fmt(it)} → " } ?: "") + "hedef ${fmt(target)} $unit",
                color = muted,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        if (current == null) {
            Text("Ölçü girince hedef takibi başlar", color = muted, fontSize = 13.sp)
            return@Column
        }
        val prog = goalProgress(start ?: current, current, target)
        val fill = remember { Animatable(0f) }
        LaunchedEffect(prog) { fill.animateTo(prog, tween(1400, easing = FastOutSlowInEasing)) }
        val reached = prog >= 1f
        val close = prog >= 0.75f
        val inf = rememberInfiniteTransition(label = "goal")
        val pulse by inf.animateFloat(0.45f, 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse")
        val bounce by inf.animateFloat(0f, -8f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "bounce")
        val color = lerp(base, done, fill.value)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(color.copy(alpha = if (close && !reached) pulse else 1f))
                )
            }
            if (reached) {
                Text("🎯", fontSize = 22.sp, modifier = Modifier.graphicsLayer { translationY = bounce.dp.toPx() })
            }
        }
        Spacer(Modifier.height(6.dp))
        val left = abs(current - target)
        Text(
            when {
                reached -> "Hedefe ulaştın! 🎯"
                close -> "Hedefe ${fmt(left)} $unit kaldı, az kaldı! 🔥"
                else -> "%${(prog * 100).toInt()} · ${fmt(left)} $unit kaldı"
            },
            color = if (close) done else muted,
            fontSize = 13.sp,
            fontWeight = if (close) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
