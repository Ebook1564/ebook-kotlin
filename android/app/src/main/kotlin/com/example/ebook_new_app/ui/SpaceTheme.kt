package com.example.ebook_new_app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class PlanetData(
    val name: String,
    val mainColor: Color,
    val accentColors: List<Color>,
    val radius: Dp,
    val hasRings: Boolean = false,
    val ringColor: Color = Color.White,
    val particleCount: Int = 3
)

data class OrbitingPlanet(
    val data: PlanetData,
    val orbitRadius: Dp,
    val angleOffset: Float,
    val speed: Float
)

data class PlanetSystem(
    val planet: PlanetData,
    val orbitingPlanets: List<OrbitingPlanet>
)

@Composable
fun CelestialBody(
    planet: PlanetData,
    orbitingPlanets: List<OrbitingPlanet>,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.2f else 1f,
        animationSpec = tween(400)
    )

    Box(
        modifier = Modifier
            .size((planet.radius.value * 3 * animatedScale).dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "planet_galaxy")
        
        val globalRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "global_rotation"
        )
        
        val planetPulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "planet_pulse"
        )
        
        val energyWave by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "energy_wave"
        )
        
        val orbitAngles = orbitingPlanets.mapIndexed { index, orb ->
            infiniteTransition.animateFloat(
                initialValue = orb.angleOffset,
                targetValue = orb.angleOffset + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween((8000 / orb.speed).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "orbit_$index"
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            rotate(globalRotation * 0.05f, Offset(centerX, centerY)) {
                drawPlanet(planet, centerX, centerY, planetPulse, energyWave, globalRotation)
            }
            
            orbitingPlanets.forEachIndexed { index, orbitingPlanet ->
                val angle = orbitAngles[index].value
                val orbitRadiusPx = orbitingPlanet.orbitRadius.toPx()
                
                val orbitX = centerX + orbitRadiusPx * cos(Math.toRadians(angle.toDouble())).toFloat()
                val orbitY = centerY + orbitRadiusPx * sin(Math.toRadians(angle.toDouble())).toFloat()
                
                drawCircle(
                    color = orbitingPlanet.data.mainColor.copy(alpha = 0.12f),
                    radius = orbitRadiusPx,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
                
                rotate(globalRotation * 0.1f, Offset(orbitX, orbitY)) {
                    drawOrbitingPlanet(orbitingPlanet.data, orbitX, orbitY, globalRotation + angle)
                }
            }
        }
    }
}

private fun DrawScope.drawPlanet(planet: PlanetData, centerX: Float, centerY: Float, pulse: Float, energyWave: Float, rotation: Float) {
    val planetCenter = Offset(centerX, centerY)
    val radius = planet.radius.toPx() * pulse
    
    for (i in 4 downTo 0) {
        val glowRadius = radius + (i * 10).dp.toPx()
        val alpha = (0.06f - i * 0.01f).coerceAtLeast(0.01f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    planet.mainColor.copy(alpha = alpha),
                    planet.mainColor.copy(alpha = 0f)
                ),
                center = planetCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = planetCenter
        )
    }
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = planet.accentColors,
            center = Offset(centerX - radius * 0.3f, centerY - radius * 0.3f),
            radius = radius * 1.3f
        ),
        radius = radius,
        center = planetCenter
    )
    
    drawCircle(
        color = planet.mainColor,
        radius = radius,
        center = planetCenter,
        style = Stroke(width = 2.dp.toPx())
    )
    
    val craterSeed = planet.name.hashCode()
    val random = Random(craterSeed)
    for (i in 0 until 5) {
        val craterAngle = random.nextFloat() * 360f
        val craterDist = random.nextFloat() * radius * 0.55f
        val craterRadius = radius * (0.06f + random.nextFloat() * 0.08f)
        
        val craterX = centerX + craterDist * cos(Math.toRadians(craterAngle.toDouble())).toFloat()
        val craterY = centerY + craterDist * sin(Math.toRadians(craterAngle.toDouble())).toFloat()
        
        drawCircle(
            color = planet.mainColor.copy(alpha = 0.35f),
            radius = craterRadius,
            center = Offset(craterX, craterY)
        )
    }
    
    if (planet.hasRings) {
        rotate(rotation * 0.15f, planetCenter) {
            for (ring in 0 until 3) {
                val ringRadius = radius * (1.4f + ring * 0.2f)
                val ringAlpha = (0.5f - ring * 0.15f).coerceAtLeast(0.15f)
                drawCircle(
                    color = planet.ringColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = planetCenter,
                    style = Stroke(width = (2.5f - ring * 0.5f).dp.toPx())
                )
            }
        }
    }
    
    val waveRadius = radius * (1.6f + energyWave * 0.6f)
    val waveAlpha = (1f - energyWave) * 0.35f
    drawCircle(
        color = planet.accentColors.first().copy(alpha = waveAlpha),
        radius = waveRadius,
        center = planetCenter,
        style = Stroke(width = 1.5f.dp.toPx())
    )
}

private fun DrawScope.drawOrbitingPlanet(planet: PlanetData, orbitX: Float, orbitY: Float, rotation: Float) {
    val planetCenter = Offset(orbitX, orbitY)
    val radius = planet.radius.toPx()
    
    for (i in 3 downTo 0) {
        val glowRadius = radius + (i * 5).dp.toPx()
        val alpha = (0.1f - i * 0.02f).coerceAtLeast(0.02f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    planet.mainColor.copy(alpha = alpha),
                    planet.mainColor.copy(alpha = 0f)
                ),
                center = planetCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = planetCenter
        )
    }
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = planet.accentColors,
            center = Offset(orbitX - radius * 0.25f, orbitY - radius * 0.25f),
            radius = radius * 1.15f
        ),
        radius = radius,
        center = planetCenter
    )
    
    drawCircle(
        color = planet.mainColor,
        radius = radius,
        center = planetCenter,
        style = Stroke(width = 1.5f.dp.toPx())
    )
    
    if (planet.hasRings) {
        rotate(rotation * 0.2f, planetCenter) {
            drawCircle(
                color = planet.ringColor.copy(alpha = 0.5f),
                radius = radius * 1.5f,
                center = planetCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
    
    for (i in 0 until planet.particleCount) {
        val particleAngle = rotation + (i * (360f / planet.particleCount))
        val particleDist = radius * 2.2f
        
        val particleX = orbitX + particleDist * cos(Math.toRadians(particleAngle.toDouble())).toFloat()
        val particleY = orbitY + particleDist * sin(Math.toRadians(particleAngle.toDouble())).toFloat()
        
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = 2.5f.dp.toPx(),
            center = Offset(particleX, particleY)
        )
        drawCircle(
            color = planet.accentColors.first().copy(alpha = 0.3f),
            radius = 5.dp.toPx(),
            center = Offset(particleX, particleY)
        )
    }
}

fun planetForCategory(category: String): PlanetData {
    return when (category) {
        "Fiction" -> PlanetData("Fiction", Color(0xFFC70039), listOf(Color(0xFFE74C3C), Color(0xFF900C3F)), 20.dp, hasRings = true, ringColor = Color(0xFFFF6B6B).copy(alpha = 0.4f), particleCount = 4)
        "History", "History of the Americas" -> PlanetData("History", Color(0xFF00A8CC), listOf(Color(0xFF3498DB), Color(0xFF007A99)), 18.dp, particleCount = 3)
        "Science" -> PlanetData("Science", Color(0xFF9B59B6), listOf(Color(0xFF8E44AD), Color(0xFF6C3483)), 22.dp, hasRings = true, ringColor = Color(0xFFBB8FCE).copy(alpha = 0.5f), particleCount = 5)
        "Romance" -> PlanetData("Romance", Color(0xFFE91E63), listOf(Color(0xFFFF6B9D), Color(0xFFC2185B)), 19.dp, particleCount = 4)
        "Fantasy" -> PlanetData("Fantasy", Color(0xFF2ECC71), listOf(Color(0xFF58D68D), Color(0xFF27AE60)), 25.dp, hasRings = true, ringColor = Color(0xFFABEBC6).copy(alpha = 0.4f), particleCount = 6)
        "Mystery" -> PlanetData("Mystery", Color(0xFF34495E), listOf(Color(0xFF5D6D7E), Color(0xFF2C3E50)), 21.dp, particleCount = 3)
        "Biography" -> PlanetData("Biography", Color(0xFFF1C40F), listOf(Color(0xFFF4D03F), Color(0xFFF39C12)), 18.dp, particleCount = 4)
        "Adventure" -> PlanetData("Adventure", Color(0xFFE67E22), listOf(Color(0xFFF39C12), Color(0xFFD35400)), 24.dp, hasRings = true, ringColor = Color(0xFFFAD7A0).copy(alpha = 0.4f), particleCount = 5)
        else -> PlanetData("Default", Color(0xFF7F8C8D), listOf(Color(0xFF95A5A6), Color(0xFFBDC3C7)), 15.dp, particleCount = 3)
    }
}
