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
                
                rotate(degrees = angle, pivot = center) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = orbitRadiusPx,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }

                this.center.let {
                    val planetCenter = it + centerOffset
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = orbitingPlanet.data.accentColors,
                                center = planetCenter
                            ),
                            radius = orbitingPlanet.data.radius.toPx(),
                            center = planetCenter
                        )
                        restoreToCount(checkPoint)
                    }
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
