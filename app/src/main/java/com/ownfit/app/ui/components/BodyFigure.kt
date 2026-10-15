package com.ownfit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.ownfit.app.data.Gender
import com.ownfit.app.data.Muscle

private data class Build(val sh: Float, val waist: Float, val hip: Float, val arm: Float, val thigh: Float)

/**
 * Vector athlete figure (front view) with the trained muscles highlighted.
 * Drawn in code so it's crisp, theme-aware and adds ~0 KB to the app.
 */
@Composable
fun BodyFigure(gender: Gender, highlight: Collection<Muscle>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val base = cs.surfaceVariant
    val muscle = lerp(cs.surfaceVariant, cs.onSurfaceVariant, 0.22f)
    val hot = cs.primary
    val hair = lerp(cs.onSurfaceVariant, cs.surface, 0.25f)
    val b = when (gender) {
        Gender.MALE -> Build(sh = 29f, waist = 15f, hip = 17f, arm = 11f, thigh = 17f)
        Gender.FEMALE -> Build(sh = 23f, waist = 12.5f, hip = 19.5f, arm = 8.5f, thigh = 16.5f)
        Gender.OTHER -> Build(sh = 26f, waist = 14f, hip = 18f, arm = 10f, thigh = 16.5f)
    }
    val hl = highlight.toSet()
    val cardio = Muscle.CARDIO in hl

    Canvas(modifier.aspectRatio(0.5f)) {
        val u = size.width / 100f
        fun p(x: Float, y: Float) = Offset(x * u, y * u)
        fun c(m: Muscle) = if (m in hl) hot else muscle
        fun limb(x1: Float, y1: Float, x2: Float, y2: Float, w: Float, col: Color) =
            drawLine(col, p(x1, y1), p(x2, y2), strokeWidth = w * u, cap = StrokeCap.Round)
        fun mirror(block: (s: Float) -> Unit) { block(-1f); block(1f) }

        val bodyCol = if (cardio) lerp(base, hot, 0.25f) else base

        // ---- Hair behind head (female) ----
        if (gender == Gender.FEMALE) {
            drawOval(hair, p(39f, 3f), Size(22f * u, 25f * u))
            drawCircle(hair, 6f * u, p(50f, 3.5f))
        }
        // ---- Silhouette ----
        drawOval(bodyCol, p(41.5f, 5f), Size(17f * u, 20f * u)) // head
        if (gender == Gender.MALE) drawArc(hair, 180f, 180f, true, p(41.5f, 4.5f), Size(17f * u, 12f * u))
        drawRect(bodyCol, p(45f, 22f), Size(10f * u, 11f * u)) // neck
        val torso = Path().apply {
            moveTo(50f - b.sh + 5, 31f * 1); quadraticTo(50f, 27f, 50f + b.sh - 5, 31f)
            lineTo(50f + b.sh, 37f)
            quadraticTo(50f + b.sh - 1, 60f, 50f + b.waist, 84f)
            lineTo(50f + b.hip, 100f)
            lineTo(50f - b.hip, 100f)
            lineTo(50f - b.waist, 84f)
            quadraticTo(50f - b.sh + 1, 60f, 50f - b.sh, 37f)
            close()
        }
        drawScaled(torso, bodyCol, u)
        mirror { s ->
            val ax = 50f + s * (b.sh + 1)
            limb(ax, 40f, ax + s * 4, 70f, b.arm, bodyCol) // upper arm
            limb(ax + s * 4, 70f, ax + s * 6.5f, 100f, b.arm - 2.5f, bodyCol) // forearm
            drawCircle(bodyCol, 4.2f * u, p(ax + s * 7f, 105f)) // hand
            val lx = 50f + s * (b.hip - b.thigh / 2f)
            limb(lx, 100f, 50f + s * 10.5f, 145f, b.thigh, bodyCol) // thigh
            limb(50f + s * 10.5f, 145f, 50f + s * 11f, 187f, b.thigh - 6.5f, bodyCol) // shin
            drawOval(bodyCol, p(50f + s * 11f - 5f + s * 2f, 186f), Size(10f * u, 6f * u)) // foot
        }

        // ---- Muscles ----
        // Lats peek out under the arms
        mirror { s ->
            val lat = Path().apply {
                moveTo(50f + s * (b.sh - 4), 44f)
                lineTo(50f + s * (b.waist + 1.5f), 76f)
                lineTo(50f + s * (b.waist - 3f), 62f)
                close()
            }
            drawScaled(lat, c(Muscle.BACK), u)
        }
        // Chest
        mirror { s ->
            val w = if (gender == Gender.FEMALE) b.sh - 9f else b.sh - 7f
            val x = if (s < 0) 50f - w - 0.8f else 50.8f
            drawRoundRect(c(Muscle.CHEST), p(x, 38f), Size(w * u, 15f * u), CornerRadius(6f * u))
        }
        // Delts
        mirror { s -> drawCircle(c(Muscle.SHOULDERS), (b.arm / 2f + 1.8f) * u, p(50f + s * (b.sh - 0.5f), 39f)) }
        // Biceps + forearms
        mirror { s ->
            val ax = 50f + s * (b.sh + 1)
            limb(ax + s * 0.7f, 47f, ax + s * 3.3f, 66f, b.arm - 3.5f, c(Muscle.ARMS))
            limb(ax + s * 4.5f, 75f, ax + s * 6f, 94f, b.arm - 5f, c(Muscle.ARMS))
        }
        // Abs
        val absW = if (gender == Gender.FEMALE) 4.8f else 5.6f
        for (row in 0 until 3) mirror { s ->
            val x = if (s < 0) 50f - absW - 0.6f else 50.6f
            drawRoundRect(c(Muscle.CORE), p(x, 57f + row * 9.2f), Size(absW * u, 8f * u), CornerRadius(2f * u))
        }
        // Hips / glutes (front view: glute medius)
        mirror { s -> drawOval(c(Muscle.GLUTES), p(50f + s * (b.hip - 3f) - 3.5f, 92f), Size(7f * u, 10f * u)) }
        // Quads + calves
        mirror { s ->
            val lx = 50f + s * (b.hip - b.thigh / 2f)
            limb(lx + s * 0.3f, 106f, 50f + s * 10.6f, 138f, b.thigh - 5f, c(Muscle.LEGS))
            limb(50f + s * 10.7f, 153f, 50f + s * 10.9f, 172f, b.thigh - 10f, c(Muscle.LEGS))
        }
        // Heart for cardio
        if (cardio) drawHeart(hot, p(57f, 43f), 5f * u)
    }
}

private fun DrawScope.drawScaled(path: Path, color: Color, u: Float) {
    val scaled = Path().apply { addPath(path) }
    scaled.transform(androidx.compose.ui.graphics.Matrix().apply { scale(u, u) })
    drawPath(scaled, color)
}

private fun DrawScope.drawHeart(color: Color, center: Offset, r: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y + r)
        cubicTo(center.x - r * 1.8f, center.y - r * 0.2f, center.x - r * 0.6f, center.y - r * 1.4f, center.x, center.y - r * 0.4f)
        cubicTo(center.x + r * 0.6f, center.y - r * 1.4f, center.x + r * 1.8f, center.y - r * 0.2f, center.x, center.y + r)
        close()
    }
    drawPath(path, Color.White.copy(alpha = 0.9f))
    drawPath(path, color, alpha = 0.9f)
}
