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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
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
            .size(planet.radius * 2 * animatedScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotationAngles = orbitingPlanets.map {
            infiniteTransition.animateFloat(
                initialValue = it.angleOffset,
                targetValue = it.angleOffset + 360f * it.speed,
                animationSpec = infiniteRepeatable(
                    animation = tween((10000 / it.speed).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            drawPlanet(planet)

            orbitingPlanets.forEachIndexed { index, orbitingPlanet ->
                val angle = rotationAngles[index].value
                val orbitRadiusPx = orbitingPlanet.orbitRadius.toPx()
                val centerOffset = Offset(
                    orbitRadiusPx * cos(Math.toRadians(angle.toDouble())).toFloat(),
                    orbitRadiusPx * sin(Math.toRadians(angle.toDouble())).toFloat()
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = orbitRadiusPx,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                this.center.let {
                    val planetCenter = it + centerOffset
                    drawCircle(
                        color = orbitingPlanet.data.mainColor,
                        radius = orbitingPlanet.data.radius.toPx(),
                        center = planetCenter
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPlanet(planet: PlanetData) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = planet.accentColors,
            center = center,
            radius = planet.radius.toPx()
        ),
        radius = planet.radius.toPx()
    )
    drawCircle(
        color = planet.mainColor,
        radius = planet.radius.toPx(),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
}

fun planetForCategory(category: String): PlanetData {
    return when (category) {
        "Fiction" -> PlanetData("Fiction", Color(0xFFC70039), listOf(Color(0xFFC70039), Color(0xFF900C3F)), 20.dp)
        "History", "History of the Americas" -> PlanetData("History", Color(0xFF00A8CC), listOf(Color(0xFF00A8CC), Color(0xFF007A99)), 18.dp)
        "Science" -> PlanetData("Science", Color(0xFF9B59B6), listOf(Color(0xFF9B59B6), Color(0xFF8E44AD)), 22.dp)
        "Romance" -> PlanetData("Romance", Color(0xFFE91E63), listOf(Color(0xFFE91E63), Color(0xFFC2185B)), 19.dp)
        "Fantasy" -> PlanetData("Fantasy", Color(0xFF2ECC71), listOf(Color(0xFF2ECC71), Color(0xFF27AE60)), 25.dp)
        "Mystery" -> PlanetData("Mystery", Color(0xFF34495E), listOf(Color(0xFF34495E), Color(0xFF2C3E50)), 21.dp)
        "Biography" -> PlanetData("Biography", Color(0xFFF1C40F), listOf(Color(0xFFF1C40F), Color(0xFFF39C12)), 18.dp)
        "Adventure" -> PlanetData("Adventure", Color(0xFFE67E22), listOf(Color(0xFFE67E22), Color(0xFFD35400)), 24.dp)
        else -> PlanetData("Default", Color(0xFF7F8C8D), listOf(Color(0xFF7F8C8D), Color(0xFF95A5A6)), 15.dp)
    }
}
