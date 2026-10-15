package com.ownfit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ownfit.app.anim.Kinematics
import com.ownfit.app.anim.Motions
import com.ownfit.app.anim.Pt
import com.ownfit.app.anim.Skeleton
import com.ownfit.app.anim.View
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Gender
import com.ownfit.app.data.Kit
import kotlin.math.hypot
import kotlin.math.min

/**
 * Looping, theme-aware animation of an exercise. When [playing] is false it shows a still
 * frame at [stillAt] (0..1 of the loop) — cheap enough for list thumbnails.
 */
@Composable
fun ExerciseAnimation(
    exercise: Exercise?,
    gender: Gender,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    stillAt: Float = 0.5f,
    motionId: String? = null,
) {
    val motion = remember(exercise?.id, motionId) { motionId?.let { Motions.all[it] } ?: Motions.of(exercise) }
    val build = remember(gender) { Kinematics.build(gender) }
    val bounds = remember(motion, build) { Kinematics.bounds(motion, build) }
    var time by remember(motion) { mutableLongStateOf((motion.duration * stillAt).toLong()) }
    LaunchedEffect(motion, playing) {
        if (!playing) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) withFrameMillis { now -> time += now - last; last = now }
    }

    val cs = MaterialTheme.colorScheme
    val near = cs.primary
    val far = if (motion.view == View.SIDE) lerp(cs.primary, cs.surface, 0.5f) else cs.primary
    val hair = lerp(cs.primary, cs.onSurface, 0.45f)
    val gear = cs.onSurface
    val prop = cs.outlineVariant
    val band = cs.tertiary
    val kit = exercise?.kit ?: Kit.NONE

    Canvas(modifier.semantics { contentDescription = "${exercise?.name ?: "Exercise"} animation" }) {
        val s = Kinematics.place(Kinematics.poseAt(motion, time), motion, build)
        val bw = bounds.maxX - bounds.minX
        val bh = bounds.maxY - bounds.minY
        val sc = min(size.width * 0.9f / bw, size.height * 0.86f / bh)
        val gx = size.width / 2 - sc * (bounds.minX + bounds.maxX) / 2
        val gy = size.height * 0.94f - sc * bounds.maxY
        fun o(p: Pt) = Offset(gx + p.x * sc, gy + p.y * sc)
        fun seg(a: Pt, b: Pt, w: Float, c: Color) = drawLine(c, o(a), o(b), strokeWidth = w * sc, cap = StrokeCap.Round)

        // Floor and props
        drawLine(prop, Offset(size.width * 0.06f, gy + 1.5f * sc), Offset(size.width * 0.94f, gy + 1.5f * sc), strokeWidth = 1.2f * sc, cap = StrokeCap.Round)
        motion.bench?.let { b ->
            drawRoundRect(prop, o(Pt(b.x, -b.h)), Size(b.w * sc, b.h * sc), CornerRadius(1.5f * sc))
        }
        motion.wall?.let { x -> seg(Pt(x, 0f), Pt(x, -90f), 2f, prop) }
        motion.bar?.let { p -> seg(Pt(p.x - 14f, p.y), Pt(p.x + 14f, p.y), 2.2f, gear) }

        // Band runs behind the body
        if (kit == Kit.BAND) {
            val to = motion.bandTo ?: s.an1
            drawLine(band, o(s.ha1), o(to), strokeWidth = 1.4f * sc)
            drawLine(band, o(s.ha2), o(motion.bandTo ?: s.an2), strokeWidth = 1.4f * sc)
        }

        // Far side
        seg(s.sh2, s.el2, build.upperArm, far); seg(s.el2, s.ha2, build.forearm, far)
        seg(s.hip2, s.kn2, build.thigh, far); seg(s.kn2, s.an2, build.shin, far)
        foot(s, far, sc, second = true, o = ::o)

        // Torso with a hint of build
        seg(s.hip, s.neck, build.torso, near)
        val d = s.torsoDir
        when (gender) {
            Gender.MALE -> seg(s.hip + d * 17f, s.neck - d * 2f, build.torso * 1.28f, near)
            Gender.FEMALE -> seg(s.hip, s.hip + d * 7f, build.torso * 1.18f, near)
            Gender.OTHER -> {}
        }
        // Head (+ hair for female)
        val hc = o(s.head)
        val r = s.headR * sc
        if (gender == Gender.FEMALE) {
            if (motion.view == View.SIDE) {
                val back = Pt(s.headDir.y, -s.headDir.x)
                drawCircle(hair, r * 0.62f, o(s.head + back * (s.headR * 1.05f) - s.headDir * 0.6f))
                drawCircle(hair, r * 0.42f, o(s.head + back * (s.headR * 1.6f) - s.headDir * 3f))
            } else drawCircle(hair, r * 1.18f, hc + Offset(0f, -r * 0.12f))
        }
        drawCircle(near, r, hc)

        // Near side
        seg(s.hip1, s.kn1, build.thigh, near); seg(s.kn1, s.an1, build.shin, near)
        foot(s, near, sc, second = false, o = ::o)
        seg(s.sh1, s.el1, build.upperArm, near); seg(s.el1, s.ha1, build.forearm, near)

        // Weights
        val front = motion.view == View.FRONT
        when (kit) {
            Kit.TWO_DUMBBELLS -> { dumbbell(o(s.ha1), sc, gear, front); dumbbell(o(s.ha2), sc, gear, front) }
            Kit.ONE_DUMBBELL -> dumbbell(if (motion.twoHandWeight) o((s.ha1 + s.ha2) * 0.5f) else o(s.ha1), sc, gear, front)
            Kit.KETTLEBELL -> {
                val h = if (motion.twoHandWeight) (s.ha1 + s.ha2) * 0.5f else s.ha1
                drawCircle(gear, 3.6f * sc, o(h) + Offset(0f, 3.8f * sc))
                drawCircle(gear, 1.6f * sc, o(h), style = androidx.compose.ui.graphics.drawscope.Stroke(0.9f * sc))
            }
            else -> {}
        }
    }
}

private fun DrawScope.foot(s: Skeleton, c: Color, sc: Float, second: Boolean, o: (Pt) -> Offset) {
    val kn = if (second) s.kn2 else s.kn1
    val an = if (second) s.an2 else s.an1
    if (s.view == View.FRONT) {
        drawCircle(c, 2.6f * sc, o(an))
        return
    }
    val vx = an.x - kn.x; val vy = an.y - kn.y
    val len = hypot(vx, vy).coerceAtLeast(0.01f)
    val toe = Pt(an.x + vy / len * 4.5f, an.y - vx / len * 4.5f)
    drawLine(c, o(an), o(toe), strokeWidth = 3.4f * sc, cap = StrokeCap.Round)
}

private fun DrawScope.dumbbell(at: Offset, sc: Float, c: Color, front: Boolean) {
    if (front) {
        drawLine(c, at - Offset(4.5f * sc, 0f), at + Offset(4.5f * sc, 0f), strokeWidth = 1.3f * sc)
        drawRoundRect(c, at - Offset(6.2f * sc, 2.6f * sc), Size(2.4f * sc, 5.2f * sc), CornerRadius(0.8f * sc))
        drawRoundRect(c, at + Offset(3.8f * sc, -2.6f * sc), Size(2.4f * sc, 5.2f * sc), CornerRadius(0.8f * sc))
    } else {
        drawCircle(c, 3.3f * sc, at)
        drawCircle(Color.White.copy(alpha = 0.35f), 1.2f * sc, at)
    }
}
