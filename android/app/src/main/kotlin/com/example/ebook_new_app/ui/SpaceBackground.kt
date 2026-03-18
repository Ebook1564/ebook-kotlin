package com.example.ebook_new_app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────
//  Colour palette
// ─────────────────────────────────────────────
object SpaceGlowColors {
    val DeepSpace    = Color(0xFF03040F)
    val NebulaPurple = Color(0xFF6C29C0)
    val NebulaBlue   = Color(0xFF1A3BCC)
    val NebulaPink   = Color(0xFFC0298A)
    val GlowCyan     = Color(0xFF00E5FF)
    val GlowViolet   = Color(0xFFB84FFF)
    val PlanetAmber  = Color(0xFFFF9D00)
    val StarWhite    = Color(0xFFE8ECFF)
}

// ─────────────────────────────────────────────
//  Data classes
// ─────────────────────────────────────────────
private data class StarData(
    val x: Float,           // 0..1 (fraction of screen width)
    val y: Float,           // 0..1 (fraction of screen height)
    val radius: Float,      // dp-independent pixel radius
    val alpha: Float,       // base opacity
    val color: Color
)

private data class ParticleConfig(
    val xFraction: Float,   // 0..1 horizontal start
    val durationMs: Int,    // full loop duration
    val startOffsetMs: Int, // animation phase offset
    val color: Color,
    val radius: Float
)

private data class NebulaBlobConfig(
    val cx: Float,          // 0..1
    val cy: Float,          // 0..1
    val radiusFraction: Float,  // fraction of min(W,H)
    val color: Color,
    val pulseDelay: Int
)

// ─────────────────────────────────────────────
//  Factories – built once and remembered
// ─────────────────────────────────────────────
private fun generateStars(count: Int, seed: Long = 42L): List<StarData> {
    val rng = Random(seed)
    val accentColors = listOf(
        SpaceGlowColors.GlowCyan,
        SpaceGlowColors.GlowViolet,
        SpaceGlowColors.PlanetAmber,
        SpaceGlowColors.StarWhite
    )
    return List(count) {
        StarData(
            x      = rng.nextFloat(),
            y      = rng.nextFloat(),
            radius = rng.nextFloat() * 1.8f + 0.6f,
            alpha  = rng.nextFloat() * 0.6f + 0.3f,
            color  = if (rng.nextFloat() > 0.75f)
                         accentColors[rng.nextInt(accentColors.size)]
                     else SpaceGlowColors.StarWhite
        )
    }
}

private fun buildParticleConfigs(): List<ParticleConfig> = listOf(
    ParticleConfig(0.10f, 8_000,      0, SpaceGlowColors.GlowCyan,    2.2f),
    ParticleConfig(0.25f, 12_000,  2_000, SpaceGlowColors.GlowViolet,  1.8f),
    ParticleConfig(0.45f, 10_000,  4_000, SpaceGlowColors.GlowCyan,    2.0f),
    ParticleConfig(0.62f, 9_000,   1_500, SpaceGlowColors.PlanetAmber, 2.5f),
    ParticleConfig(0.80f, 14_000,  6_000, SpaceGlowColors.StarWhite,   1.6f),
    ParticleConfig(0.35f, 11_000,  3_000, SpaceGlowColors.GlowViolet,  1.8f),
    ParticleConfig(0.90f, 7_500,     500, SpaceGlowColors.GlowCyan,    2.0f),
)

private val nebulaBlobs = listOf(
    NebulaBlobConfig(0.15f, 0.12f, 0.48f, SpaceGlowColors.NebulaPurple, 0),
    NebulaBlobConfig(0.88f, 0.28f, 0.40f, SpaceGlowColors.NebulaBlue,   2_000),
    NebulaBlobConfig(0.50f, 0.82f, 0.35f, SpaceGlowColors.NebulaPink,   4_000),
)

// ─────────────────────────────────────────────
//  Main composable
// ─────────────────────────────────────────────

@Composable
fun SpaceBackground(modifier: Modifier = Modifier) {

    // ── stable data ──────────────────────────
    val stars     = remember { generateStars(90) }
    val particles = remember { buildParticleConfigs() }

    // ── infinite transition (one shared clock) ──
    val inf = rememberInfiniteTransition(label = "space")

    // Nebula pulse: 0 → 1 → 0 (sine-like via Reverse)
    val nebulaPulse by inf.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nebulaPulse"
    )

    // Per-particle linear progress values (7 particles)
    val p0 by particleAnim(inf, particles[0])
    val p1 by particleAnim(inf, particles[1])
    val p2 by particleAnim(inf, particles[2])
    val p3 by particleAnim(inf, particles[3])
    val p4 by particleAnim(inf, particles[4])
    val p5 by particleAnim(inf, particles[5])
    val p6 by particleAnim(inf, particles[6])
    val particleProgress = listOf(p0, p1, p2, p3, p4, p5, p6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceGlowColors.DeepSpace)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // 1. Deep nebula glow blobs
            drawNebulaBlobs(nebulaPulse)

            // 2. Static star field
            drawStars(stars)

            // 3. Floating particles
            drawParticles(particles, particleProgress)
        }
    }
}

// ─────────────────────────────────────────────
//  Helpers: animation
// ─────────────────────────────────────────────

@Composable
private fun particleAnim(
    inf: InfiniteTransition,
    cfg: ParticleConfig
): androidx.compose.runtime.State<Float> =
    inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation     = tween(cfg.durationMs, easing = LinearEasing),
            repeatMode    = RepeatMode.Restart,
            initialStartOffset = StartOffset(cfg.startOffsetMs)
        ),
        label = "particle_${cfg.xFraction}"
    )

// ─────────────────────────────────────────────
//  DrawScope extensions
// ─────────────────────────────────────────────

private fun DrawScope.drawNebulaBlobs(pulse: Float) {
    nebulaBlobs.forEach { blob ->
        // Individual blob has its own phase offset baked into colour alpha modulation
        val blobPulse = (pulse + blob.pulseDelay / 6_000f).mod(1f)
        // Sine wave: smooth 0.15 → 0.26 oscillation
        val alphaMod = 0.15f + 0.11f * sin(blobPulse * PI.toFloat())
        val scaleMod = 1.0f  + 0.08f * sin(blobPulse * PI.toFloat())

        val cx     = blob.cx * size.width
        val cy     = blob.cy * size.height
        val radius = blob.radiusFraction * minOf(size.width, size.height) * scaleMod

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                asFrameworkPaint().apply {
                    // Soft radial glow via BlurMaskFilter
                    maskFilter = android.graphics.BlurMaskFilter(
                        radius * 0.85f,
                        android.graphics.BlurMaskFilter.Blur.NORMAL
                    )
                    color = blob.color.copy(alpha = alphaMod).toArgb()
                    style = android.graphics.Paint.Style.FILL
                }
            }
            canvas.nativeCanvas.drawCircle(cx, cy, radius, paint.asFrameworkPaint())
        }
    }
}

private fun DrawScope.drawStars(stars: List<StarData>) {
    stars.forEach { star ->
        drawCircle(
            color  = star.color.copy(alpha = star.alpha),
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height)
        )
        // Subtle glow ring for brighter stars
        if (star.radius > 1.6f) {
            drawCircle(
                color  = star.color.copy(alpha = star.alpha * 0.25f),
                radius = star.radius * 2.8f,
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}

private fun DrawScope.drawParticles(
    configs: List<ParticleConfig>,
    progresses: List<Float>
) {
    configs.forEachIndexed { i, cfg ->
        val progress = progresses[i]          // 0..1 linear
        val x = cfg.xFraction * size.width

        // Particle travels from bottom (above nav bar) to off-screen top
        val startY = size.height - 90f        // just above bottom nav
        val endY   = -20f
        val y = startY + (endY - startY) * progress

        // Fade in first 20%, hold, fade out last 20%
        val alpha = when {
            progress < 0.20f -> progress / 0.20f
            progress > 0.80f -> (1f - progress) / 0.20f
            else             -> 1f
        } * 0.7f

        // Slight horizontal sine drift
        val driftX = x + sin(progress * 2 * PI.toFloat()) * 12f

        // Core dot
        drawCircle(
            color  = cfg.color.copy(alpha = alpha),
            radius = cfg.radius,
            center = Offset(driftX, y)
        )
        // Glow halo
        drawCircle(
            color  = cfg.color.copy(alpha = alpha * 0.3f),
            radius = cfg.radius * 4f,
            center = Offset(driftX, y)
        )
    }
}
