package com.ownfit.app.anim

import com.ownfit.app.data.Gender
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A tiny 2D skeleton animator.
 *
 * Angles are absolute, in degrees: 0 = pointing down, 90 = forward (screen right),
 * 180 = up, -90 = backward. In [View.SIDE] the figure faces right; limb "1" is the near
 * side and "2" the far side. In [View.FRONT] limb "1" is on screen-right and "2" on
 * screen-left, so symmetric poses use mirrored (negated) angles for side 2.
 */
enum class View { SIDE, FRONT }

data class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
}

data class Pose(
    val x: Float = 50f,
    val lift: Float = 0f,
    val torso: Float = 180f,
    val head: Float = 180f,
    val ua1: Float = 0f, val fa1: Float = 0f,
    val ua2: Float = 0f, val fa2: Float = 0f,
    val th1: Float = 0f, val sh1: Float = 0f,
    val th2: Float = 0f, val sh2: Float = 0f,
) {
    /** Swap near/far (side view) or right/left (front view, mirrored). */
    fun swapSides(front: Boolean = false): Pose = if (!front) copy(
        ua1 = ua2, fa1 = fa2, ua2 = ua1, fa2 = fa1, th1 = th2, sh1 = sh2, th2 = th1, sh2 = sh1,
    ) else copy(
        x = 100f - x, torso = 360f - torso, head = 360f - head,
        ua1 = -ua2, fa1 = -fa2, ua2 = -ua1, fa2 = -fa1, th1 = -th2, sh1 = -sh2, th2 = -th1, sh2 = -sh1,
    )

    fun arms(ua: Float, fa: Float = ua) = copy(ua1 = ua, fa1 = fa, ua2 = ua, fa2 = fa)
    fun legs(th: Float, sh: Float = th) = copy(th1 = th, sh1 = sh, th2 = th, sh2 = sh)
    /** Front view: mirrored arms. */
    fun symArms(ua: Float, fa: Float = ua) = copy(ua1 = ua, fa1 = fa, ua2 = -ua, fa2 = -fa)
    /** Front view: mirrored legs. */
    fun symLegs(th: Float, sh: Float = th) = copy(th1 = th, sh1 = sh, th2 = -th, sh2 = -sh)

    companion object {
        fun lerp(a: Pose, b: Pose, t: Float): Pose {
            fun l(x: Float, y: Float) = x + (y - x) * t
            return Pose(
                l(a.x, b.x), l(a.lift, b.lift), l(a.torso, b.torso), l(a.head, b.head),
                l(a.ua1, b.ua1), l(a.fa1, b.fa1), l(a.ua2, b.ua2), l(a.fa2, b.fa2),
                l(a.th1, b.th1), l(a.sh1, b.sh1), l(a.th2, b.th2), l(a.sh2, b.sh2),
            )
        }
    }
}

data class Bench(val x: Float, val w: Float, val h: Float)

data class Motion(
    val frames: List<Pose>,
    val view: View = View.SIDE,
    /** Milliseconds per transition between consecutive frames. */
    val ms: Int = 900,
    val bench: Bench? = null,
    /** Pull-up bar: the near hand is pinned here (ground is y = 0, up is negative). */
    val bar: Pt? = null,
    /** Pin the near hand to this point instead of standing on the ground. */
    val handAnchor: Pt? = null,
    val wall: Float? = null,
    /** Resistance band runs from the hands to this point (else to the feet). */
    val bandTo: Pt? = null,
    /** Two-handed single weight (goblet, front raise...). */
    val twoHandWeight: Boolean = false,
) {
    val duration: Int get() = frames.size * ms
}

class Skeleton(
    val view: View,
    val hip: Pt, val hip1: Pt, val hip2: Pt,
    val neck: Pt, val sh1: Pt, val sh2: Pt,
    val head: Pt, val headR: Float,
    val el1: Pt, val ha1: Pt, val el2: Pt, val ha2: Pt,
    val kn1: Pt, val an1: Pt, val kn2: Pt, val an2: Pt,
    val torsoDir: Pt, val headDir: Pt,
) {
    val points get() = listOf(hip1, hip2, neck, sh1, sh2, el1, ha1, el2, ha2, kn1, an1, kn2, an2)

    fun shift(d: Pt) = Skeleton(
        view, hip + d, hip1 + d, hip2 + d, neck + d, sh1 + d, sh2 + d, head + d, headR,
        el1 + d, ha1 + d, el2 + d, ha2 + d, kn1 + d, an1 + d, kn2 + d, an2 + d, torsoDir, headDir,
    )

    /** Lowest y (largest, since y points down) including head and foot thickness. */
    val bottom: Float
        get() = maxOf(points.maxOf { it.y } + 2.5f, head.y + headR, hip.y + 5f, neck.y + 5f)
}

data class Build(val shoulder: Float, val hip: Float, val upperArm: Float, val forearm: Float, val thigh: Float, val shin: Float, val torso: Float)

object Kinematics {
    const val TORSO = 30f
    const val NECK = 7f
    const val HEAD_R = 6.5f
    const val UA = 15f
    const val FA = 14f
    const val TH = 21f
    const val SH = 21f

    /** Limb thickness and frontal widths per body type (world units). */
    fun build(g: Gender) = when (g) {
        Gender.MALE -> Build(shoulder = 8.5f, hip = 4.5f, upperArm = 6.8f, forearm = 5.4f, thigh = 8.8f, shin = 6.6f, torso = 12f)
        Gender.FEMALE -> Build(shoulder = 7f, hip = 5.8f, upperArm = 5.2f, forearm = 4.4f, thigh = 8.4f, shin = 6f, torso = 10f)
        Gender.OTHER -> Build(shoulder = 7.8f, hip = 5f, upperArm = 6f, forearm = 4.9f, thigh = 8.6f, shin = 6.3f, torso = 11f)
    }

    private fun dir(deg: Float): Pt {
        val r = Math.toRadians(deg.toDouble())
        return Pt(sin(r).toFloat(), cos(r).toFloat())
    }

    /** Forward kinematics with the hip at (pose.x, 0). */
    fun solve(p: Pose, view: View, b: Build): Skeleton {
        val hip = Pt(p.x, 0f)
        val d = dir(p.torso)
        val perp = Pt(-d.y, d.x) // screen-right when upright
        val front = view == View.FRONT
        val sw = if (front) b.shoulder else 0f
        val hw = if (front) b.hip else 0f
        val neck = hip + d * TORSO
        val sh1 = neck + perp * sw
        val sh2 = neck - perp * sw
        val hip1 = hip + perp * hw
        val hip2 = hip - perp * hw
        val hd = dir(p.head)
        val head = neck + hd * (NECK + HEAD_R - 3f)
        val el1 = sh1 + dir(p.ua1) * UA
        val ha1 = el1 + dir(p.fa1) * FA
        val el2 = sh2 + dir(p.ua2) * UA
        val ha2 = el2 + dir(p.fa2) * FA
        val kn1 = hip1 + dir(p.th1) * TH
        val an1 = kn1 + dir(p.sh1) * SH
        val kn2 = hip2 + dir(p.th2) * TH
        val an2 = kn2 + dir(p.sh2) * SH
        return Skeleton(view, hip, hip1, hip2, neck, sh1, sh2, head, HEAD_R, el1, ha1, el2, ha2, kn1, an1, kn2, an2, d, hd)
    }

    /** Solve and place in the world: grounded (y = 0 is the floor) or pinned by the hands. */
    fun place(p: Pose, m: Motion, b: Build): Skeleton {
        val s = solve(p, m.view, b)
        val pin = m.bar ?: m.handAnchor
        return if (pin != null) s.shift(pin - s.ha1 + Pt(0f, -p.lift))
        else s.shift(Pt(0f, -s.bottom - p.lift))
    }

    private fun ease(t: Float) = t * t * (3f - 2f * t)

    /** Pose at time [ms] (looping). */
    fun poseAt(m: Motion, ms: Long): Pose {
        val n = m.frames.size
        if (n == 1) return m.frames[0]
        val total = (n * m.ms).toLong()
        val t = (ms % total).toFloat() / m.ms
        val i = t.toInt() % n
        val f = ease(t - t.toInt())
        return Pose.lerp(m.frames[i], m.frames[(i + 1) % n], f)
    }

    data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    /** World-space bounds over the whole loop, used to fit the figure in its box. */
    fun bounds(m: Motion, b: Build): Bounds {
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = 0f; var maxY = 0f
        val steps = 8
        for (i in m.frames.indices) for (k in 0 until steps) {
            val p = Pose.lerp(m.frames[i], m.frames[(i + 1) % m.frames.size], k / steps.toFloat())
            val s = place(p, m, b)
            val pts = s.points + listOf(s.head + Pt(-s.headR, -s.headR), s.head + Pt(s.headR, s.headR))
            pts.forEach {
                minX = min(minX, it.x - 4f); maxX = max(maxX, it.x + 4f)
                minY = min(minY, it.y - 3f); maxY = max(maxY, it.y + 3f)
            }
        }
        m.bench?.let { minX = min(minX, it.x); maxX = max(maxX, it.x + it.w); minY = min(minY, -it.h) }
        m.bar?.let { minY = min(minY, it.y - 3f); minX = min(minX, it.x - 14f); maxX = max(maxX, it.x + 14f) }
        m.wall?.let { minX = min(minX, it - 2f); maxX = max(maxX, it + 2f) }
        return Bounds(minX, maxX, minY, max(maxY, 0f))
    }
}
