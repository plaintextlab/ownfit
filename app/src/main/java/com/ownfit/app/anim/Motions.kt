package com.ownfit.app.anim

import com.ownfit.app.data.Category
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Muscle

/**
 * Keyframed motions for every built-in exercise. Custom exercises fall back to a motion
 * picked from their category and main muscle.
 */
object Motions {
    // ---------- Base poses (side view, facing right) ----------
    private val STAND = Pose()
    private val SQUAT = Pose(x = 38f, torso = 145f, head = 165f).arms(90f).legs(80f, -25f)
    private val HALF_SQUAT = Pose(x = 44f, torso = 160f, head = 172f).legs(45f, -20f)
    private val HINGE = Pose(x = 46f, torso = 115f, head = 125f).legs(15f, -10f)
    private val FOLD = Pose(x = 50f, torso = 62f, head = 35f).arms(0f).legs(0f)
    private val PUSH_UP = Pose(x = 46f, torso = 114f, head = 112f).arms(0f).legs(-66f)
    private val PUSH_DOWN = Pose(x = 46f, torso = 100f, head = 98f).arms(-80f, 45f).legs(-80f)
    private val PLANK = Pose(x = 46f, torso = 102f, head = 100f).arms(0f, 90f).legs(-78f)
    private val ALL_FOURS = Pose(x = 44f, torso = 90f, head = 100f).arms(0f).legs(0f, -90f)
    private val LYING = Pose(x = 56f, torso = -90f, head = -90f).arms(85f, 90f).legs(135f, -10f)
    private val LYING_FLAT = Pose(x = 56f, torso = -90f, head = -90f).arms(80f, 88f).legs(90f)
    private val PRONE = Pose(x = 46f, torso = 92f, head = 95f).arms(-88f).legs(-90f)
    private val SEATED = Pose(x = 50f, torso = 180f, head = 180f).arms(40f, 70f).legs(90f)
    private val BRIDGE_UP = Pose(x = 56f, torso = -110f, head = -100f).arms(75f, 85f).legs(100f, -20f)

    // ---------- Front view ----------
    private val F_STAND = Pose().symArms(8f).symLegs(3f)
    private val F_HANDS_HIPS = Pose().symArms(35f, -55f).symLegs(4f)

    private fun hold(p: Pose, view: View = View.SIDE, extra: (Motion) -> Motion = { it }) = extra(
        Motion(listOf(p, p.copy(torso = p.torso + 1.5f, head = p.head + 3f, ua1 = p.ua1 + 2f, ua2 = p.ua2 - 2f)), view, ms = 1800),
    )

    /** Alternate sides, passing through a neutral frame so both legs never bend at once. */
    private fun alt(p: Pose, ms: Int, view: View = View.SIDE, mid: Pose? = null): Motion {
        val other = p.swapSides(view == View.FRONT)
        val m = mid ?: Pose.lerp(p, other, 0.5f).copy(th1 = 0f, sh1 = 0f, th2 = 0f, sh2 = 0f, lift = 0f)
        return Motion(listOf(p, m, other, m), view, ms / 2)
    }

    // ---------- Patterns ----------
    private val march = alt(STAND.copy(th1 = 70f, sh1 = 0f, ua1 = -30f, fa1 = 20f, ua2 = 35f, fa2 = 80f), 550)
    private val highKnees = alt(STAND.copy(lift = 2f, th1 = 95f, sh1 = 5f, ua1 = -40f, fa1 = 60f, ua2 = 50f, fa2 = 120f), 300)
    private val buttKicks = alt(STAND.copy(lift = 2f, th1 = -5f, sh1 = -160f, ua1 = 30f, fa1 = 110f, ua2 = -20f, fa2 = 60f), 300)
    private val fastFeet = alt(Pose(x = 46f, torso = 165f, head = 170f).arms(30f, 100f).copy(th1 = 30f, sh1 = -25f, th2 = 12f, sh2 = -12f, lift = 1f), 180,
        mid = Pose(x = 46f, torso = 165f, head = 170f).arms(30f, 100f).legs(20f, -18f))
    private val legSwings = Motion(listOf(STAND.copy(th1 = 55f, sh1 = 50f, ua2 = 80f, fa2 = 85f), STAND.copy(th1 = -35f, sh1 = -40f, ua2 = 80f, fa2 = 85f)), ms = 600)
    private val inchworm = Motion(listOf(STAND, FOLD, PUSH_UP.copy(x = 38f), FOLD), ms = 1000)
    private val squat = Motion(listOf(STAND, SQUAT), ms = 900)
    private val gobletSquat = Motion(listOf(STAND.arms(25f, 165f), SQUAT.arms(25f, 165f)), ms = 900, twoHandWeight = true)
    private val chairSquat = Motion(listOf(STAND.arms(90f), SQUAT), ms = 1000, bench = Bench(28f, 20f, 18f))
    private val thruster = Motion(listOf(SQUAT.arms(25f, 165f), STAND.arms(25f, 165f), STAND.arms(180f)), ms = 600)
    private val wallSit = hold(Pose(x = 36f, torso = 180f, head = 180f).arms(20f, 80f).legs(90f, 0f)) { it.copy(wall = 30f) }
    private val lunge = Motion(listOf(STAND, STAND.copy(th1 = 85f, sh1 = 0f, th2 = -30f, sh2 = -100f)), ms = 900)
    private val bulgarian = Motion(
        listOf(STAND.copy(th1 = 15f, sh1 = -5f, th2 = -45f, sh2 = -95f), STAND.copy(torso = 172f, th1 = 75f, sh1 = -5f, th2 = -5f, sh2 = -75f)),
        ms = 900, bench = Bench(14f, 20f, 16f),
    )
    private val stepUp = Motion(
        listOf(STAND.copy(x = 44f), STAND.copy(x = 44f, th1 = 70f, sh1 = -10f), STAND.copy(x = 56f, lift = 14f, th2 = -10f, sh2 = -30f)),
        ms = 700, bench = Bench(52f, 30f, 14f),
    )
    private val calfRaise = Motion(listOf(STAND, STAND.copy(lift = 4f)), ms = 700)
    private val rdl = Motion(listOf(STAND, Pose(x = 42f, torso = 100f, head = 110f).arms(0f).legs(12f, -6f)), ms = 1000)
    private val kbSwing = Motion(
        listOf(Pose(x = 44f, torso = 120f, head = 130f).arms(-15f, -20f).legs(25f, -18f), STAND.arms(95f)),
        ms = 600,
    )
    private val bridge = Motion(listOf(LYING, BRIDGE_UP), ms = 900)
    private val singleBridge = Motion(listOf(LYING.copy(th1 = 115f, sh1 = 115f), BRIDGE_UP.copy(th1 = 95f, sh1 = 95f)), ms = 900)
    private val bridgeHold = hold(BRIDGE_UP)

    private val pushup = Motion(listOf(PUSH_UP, PUSH_DOWN), ms = 800)
    private val kneePushup = Motion(
        listOf(Pose(x = 40f, torso = 125f, head = 122f).arms(0f).legs(-55f, -150f), Pose(x = 40f, torso = 104f, head = 100f).arms(-80f, 45f).legs(-76f, -150f)),
        ms = 800,
    )
    private val inclinePushup = Motion(
        listOf(Pose(x = 40f, torso = 128f, head = 125f).arms(0f).legs(-52f), Pose(x = 40f, torso = 112f, head = 110f).arms(-75f, 40f).legs(-68f)),
        ms = 800, bench = Bench(50f, 28f, 15f),
    )
    private val wallPushup = Motion(
        listOf(Pose(x = 50f, torso = 160f, head = 165f).arms(98f).legs(-18f), Pose(x = 50f, torso = 148f, head = 150f).arms(30f, 100f).legs(-30f)),
        ms = 800, wall = 96f,
    )
    private val pikePushup = Motion(
        listOf(Pose(torso = 50f, head = 30f).arms(50f).legs(-25f), Pose(torso = 38f, head = 15f).arms(-5f, 55f).legs(-25f)),
        ms = 900,
    )
    private val plank = hold(PLANK)
    private val kneePlank = hold(Pose(x = 42f, torso = 107f, head = 104f).arms(0f, 90f).legs(-73f, -160f))
    private val sidePlank = hold(Pose(x = 46f, torso = 110f, head = 112f, ua1 = 0f, fa1 = 0f, ua2 = 180f, fa2 = 180f).legs(-70f))
    private val shoulderTap = Motion(listOf(PUSH_UP, PUSH_UP.copy(ua1 = 40f, fa1 = 150f), PUSH_UP, PUSH_UP.copy(ua2 = 40f, fa2 = 150f)), ms = 450)
    private val climbers = alt(PUSH_UP.copy(th1 = 25f, sh1 = -60f), 330, mid = PUSH_UP)
    private val burpee = Motion(
        listOf(
            STAND,
            Pose(x = 42f, torso = 110f, head = 120f).arms(0f).legs(88f, -35f),
            PUSH_UP,
            Pose(x = 42f, torso = 110f, head = 120f).arms(0f).legs(88f, -35f),
            STAND.copy(lift = 12f).arms(180f),
        ),
        ms = 450,
    )
    private val lowBurpee = Motion(
        listOf(STAND, Pose(x = 42f, torso = 110f, head = 120f).arms(0f).legs(88f, -35f), PUSH_UP, Pose(x = 42f, torso = 110f, head = 120f).arms(0f).legs(88f, -35f)),
        ms = 700,
    )
    private val squatJump = Motion(listOf(SQUAT, STAND.copy(lift = 12f).arms(170f)), ms = 500)
    private val boxing = Motion(
        listOf(
            Pose(torso = 172f, head = 178f).arms(30f, 160f).copy(th1 = 15f, sh1 = 0f, th2 = -15f, sh2 = 0f),
            Pose(torso = 168f, head = 175f).arms(30f, 160f).copy(ua1 = 92f, fa1 = 90f, th1 = 15f, sh1 = 0f, th2 = -15f, sh2 = 0f),
            Pose(torso = 172f, head = 178f).arms(30f, 160f).copy(th1 = 15f, sh1 = 0f, th2 = -15f, sh2 = 0f),
            Pose(torso = 168f, head = 175f).arms(30f, 160f).copy(ua2 = 92f, fa2 = 90f, th1 = 15f, sh1 = 0f, th2 = -15f, sh2 = 0f),
        ),
        ms = 260,
    )

    // Upper body
    private val curl = Motion(listOf(STAND, STAND.arms(8f, 150f)), ms = 850)
    private val concCurl = Motion(
        listOf(SEATED.copy(torso = 150f, head = 160f, ua1 = 12f, fa1 = 10f, ua2 = 60f, fa2 = 90f).legs(90f, 0f), SEATED.copy(torso = 150f, head = 160f, ua1 = 12f, fa1 = 150f, ua2 = 60f, fa2 = 90f).legs(90f, 0f)),
        ms = 850, bench = Bench(42f, 22f, 17f),
    )
    private val frontRaise = Motion(listOf(STAND, STAND.arms(92f)), ms = 900, twoHandWeight = true)
    private val overheadExt = Motion(listOf(STAND.arms(175f, -20f), STAND.arms(175f, 178f)), ms = 900, twoHandWeight = true)
    private val kickback = Motion(
        listOf(HINGE.copy(ua1 = -75f, fa1 = 0f, ua2 = 35f, fa2 = 20f), HINGE.copy(ua1 = -75f, fa1 = -78f, ua2 = 35f, fa2 = 20f)),
        ms = 800,
    )
    private val row = Motion(listOf(HINGE.arms(0f), HINGE.arms(-55f, 5f)), ms = 850)
    private val oneArmRow = Motion(
        listOf(HINGE.copy(ua1 = 0f, fa1 = 0f, ua2 = 10f, fa2 = 10f), HINGE.copy(ua1 = -55f, fa1 = 5f, ua2 = 10f, fa2 = 10f)),
        ms = 850, bench = Bench(58f, 26f, 26f),
    )
    private val reverseFly = Motion(listOf(HINGE.arms(5f), HINGE.arms(-45f, -50f)), ms = 900)
    private val bandRow = Motion(listOf(STAND.arms(90f), STAND.arms(-35f, 85f)), ms = 850, bandTo = Pt(92f, -55f))
    private val bandPress = Motion(listOf(STAND.arms(-40f, 85f), STAND.arms(90f)), ms = 850, bandTo = Pt(18f, -55f))
    private val floorPress = Motion(listOf(LYING.arms(95f, 180f), LYING.arms(180f)), ms = 850)
    private val fly = Motion(listOf(LYING.arms(180f), LYING.arms(140f, 150f)), ms = 1000)
    private val superman = Motion(listOf(PRONE.copy(torso = 90f, head = 95f).arms(92f), PRONE.copy(torso = 104f, head = 112f).arms(110f).legs(-104f)), ms = 1000)
    private val snowAngel = Motion(listOf(PRONE.copy(torso = 96f, head = 100f).arms(-95f), PRONE.copy(torso = 96f, head = 100f).arms(-255f)), ms = 1100)
    private val proneY = Motion(listOf(PRONE.copy(torso = 94f).arms(98f), PRONE.copy(torso = 98f, head = 105f).arms(118f)), ms = 900)
    private val PULL_HANG = Pose(torso = 180f, head = 180f).arms(180f).legs(5f, -25f)
    private val PULL_TOP = Pose(torso = 180f, head = 175f).arms(20f, 165f).legs(5f, -25f)
    private val pullup = Motion(listOf(PULL_HANG, PULL_TOP), ms = 1000, bar = Pt(50f, -96f))
    private val negative = Motion(listOf(PULL_TOP, PULL_HANG), ms = 1500, bar = Pt(50f, -96f))
    private val deadHang = hold(PULL_HANG) { it.copy(bar = Pt(50f, -96f)) }
    private val hangingKnee = Motion(listOf(PULL_HANG, PULL_HANG.legs(100f, 5f)), ms = 900, bar = Pt(50f, -96f))
    private val dip = Motion(
        listOf(Pose(torso = 180f, head = 180f).arms(-15f).legs(85f, 0f), Pose(torso = 180f, head = 180f).arms(-90f, 0f).legs(95f, 65f)),
        ms = 900, handAnchor = Pt(34f, -20f), bench = Bench(14f, 22f, 20f),
    )

    // Front-view arm work
    private val press = Motion(listOf(F_STAND.symArms(80f, 175f), F_STAND.symArms(168f, 178f)), View.FRONT, 850)
    private val singlePress = Motion(
        listOf(F_STAND.copy(ua1 = 80f, fa1 = 175f), F_STAND.copy(ua1 = 168f, fa1 = 178f)),
        View.FRONT, 850,
    )
    private val lateralRaise = Motion(listOf(F_STAND.symArms(12f, 10f), F_STAND.symArms(85f, 82f)), View.FRONT, 900)
    private val wallAngels = Motion(listOf(F_STAND.symArms(80f, 170f), F_STAND.symArms(150f, 165f)), View.FRONT, 1000)
    private val pullApart = Motion(listOf(F_STAND.symArms(45f, 70f), F_STAND.symArms(90f)), View.FRONT, 800)
    private val woodchop = Motion(
        listOf(F_STAND.copy(torso = 176f, ua1 = -40f, fa1 = -35f, ua2 = -20f, fa2 = -40f).symLegs(12f), F_STAND.copy(torso = 184f, ua1 = 150f, fa1 = 150f, ua2 = 140f, fa2 = 145f).symLegs(12f)),
        View.FRONT, 700, twoHandWeight = true,
    )

    // Legs (front view)
    private val jumpingJack = Motion(listOf(F_STAND, Pose(lift = 3f).symArms(160f, 170f).symLegs(20f, 6f)), View.FRONT, 380)
    private val stepJack = Motion(
        listOf(F_STAND, F_STAND.symArms(140f, 150f).copy(th1 = 25f, sh1 = 8f), F_STAND, F_STAND.symArms(140f, 150f).copy(th2 = -25f, sh2 = -8f)),
        View.FRONT, 420,
    )
    private val sumoSquat = Motion(
        listOf(Pose().symLegs(18f, 4f).copy(ua1 = -14f, fa1 = -6f, ua2 = 14f, fa2 = 6f), Pose().symLegs(68f, -6f).copy(ua1 = -14f, fa1 = -6f, ua2 = 14f, fa2 = 6f)),
        View.FRONT, 900, twoHandWeight = true,
    )
    private val sideLunge = Motion(
        listOf(F_STAND, Pose(x = 44f, torso = 172f, head = 176f).symArms(-10f).copy(th1 = 58f, sh1 = -6f, th2 = -48f, sh2 = -48f), F_STAND, Pose(x = 56f, torso = 188f, head = 184f).symArms(-10f).copy(th2 = -58f, sh2 = 6f, th1 = 48f, sh1 = 48f)),
        View.FRONT, 650,
    )
    private val lateralWalk = Motion(listOf(F_HANDS_HIPS.copy(x = 48f).symLegs(10f, 0f), F_HANDS_HIPS.copy(x = 52f).symLegs(20f, -4f)), View.FRONT, 450, bandTo = null)
    private val skater = Motion(
        listOf(
            Pose(x = 40f, torso = 172f, head = 176f, ua1 = -45f, fa1 = -50f, ua2 = -60f, fa2 = -70f, th1 = 22f, sh1 = -4f, th2 = 28f, sh2 = 70f),
            Pose(x = 50f, torso = 180f, head = 180f, lift = 4f).symArms(15f).symLegs(8f, -4f),
            Pose(x = 60f, torso = 188f, head = 184f, ua1 = 60f, fa1 = 70f, ua2 = 45f, fa2 = 50f, th1 = -28f, sh1 = -70f, th2 = -22f, sh2 = 4f),
            Pose(x = 50f, torso = 180f, head = 180f, lift = 4f).symArms(-15f).symLegs(8f, -4f),
        ),
        View.FRONT, 300,
    )
    private val armCircles = Motion(
        listOf(F_STAND.symArms(80f), F_STAND.symArms(92f), F_STAND.symArms(100f), F_STAND.symArms(88f)),
        View.FRONT, 230,
    )
    private val hipCircles = Motion(
        listOf(F_HANDS_HIPS.copy(x = 47f, torso = 176f), F_HANDS_HIPS.copy(x = 50f, torso = 180f, lift = 1f), F_HANDS_HIPS.copy(x = 53f, torso = 184f), F_HANDS_HIPS.copy(x = 50f, torso = 180f)),
        View.FRONT, 450,
    )
    private val torsoTwist = Motion(
        listOf(F_STAND.copy(ua1 = 70f, fa1 = 70f, ua2 = 60f, fa2 = 110f), F_STAND.copy(ua1 = -60f, fa1 = -110f, ua2 = -70f, fa2 = -70f)),
        View.FRONT, 600,
    )

    // Core (lying)
    private val crunch = Motion(listOf(LYING.arms(150f, 60f), LYING.copy(torso = -122f, head = -135f).arms(150f, 60f)), ms = 800)
    private val bicycle = Motion(
        listOf(
            LYING.copy(torso = -115f, head = -125f, ua1 = 200f, fa1 = 120f, ua2 = 200f, fa2 = 120f, th1 = 150f, sh1 = 80f, th2 = 100f, sh2 = 100f),
            LYING.copy(torso = -115f, head = -125f, ua1 = 200f, fa1 = 120f, ua2 = 200f, fa2 = 120f, th2 = 150f, sh2 = 80f, th1 = 100f, sh1 = 100f),
        ),
        ms = 450,
    )
    private val legRaise = Motion(listOf(LYING_FLAT.legs(95f), LYING_FLAT.legs(178f)), ms = 1000)
    private val deadBug = Motion(
        listOf(
            LYING.arms(180f).legs(180f, 90f),
            LYING.arms(180f).legs(180f, 90f).copy(ua1 = 265f, fa1 = 268f, th2 = 100f, sh2 = 100f),
            LYING.arms(180f).legs(180f, 90f),
            LYING.arms(180f).legs(180f, 90f).copy(ua2 = 265f, fa2 = 268f, th1 = 100f, sh1 = 100f),
        ),
        ms = 650,
    )
    private val hollow = hold(Pose(x = 54f, torso = -106f, head = -112f).arms(250f).legs(106f))
    private val russianTwist = Motion(
        listOf(
            Pose(torso = 210f, head = 195f).legs(130f, 60f).copy(ua1 = 140f, fa1 = 120f, ua2 = 140f, fa2 = 120f),
            Pose(torso = 210f, head = 195f).legs(130f, 60f).copy(ua1 = 90f, fa1 = 70f, ua2 = 90f, fa2 = 70f),
        ),
        ms = 500,
    )
    private val heelTaps = LYING.copy(torso = -115f, head = -125f, ua1 = 75f, fa1 = 78f, ua2 = 95f, fa2 = 98f)
        .let { alt(it, 400, mid = Pose.lerp(it, it.swapSides(), 0.5f)) }
    private val birdDog = Motion(
        listOf(ALL_FOURS, ALL_FOURS.copy(ua1 = 95f, fa1 = 95f, th2 = -90f, sh2 = -90f), ALL_FOURS, ALL_FOURS.copy(ua2 = 95f, fa2 = 95f, th1 = -90f, sh1 = -90f)),
        ms = 800,
    )
    private val donkeyKick = Motion(listOf(ALL_FOURS, ALL_FOURS.copy(th1 = -118f, sh1 = 175f)), ms = 700)
    private val fireHydrant = Motion(listOf(ALL_FOURS, ALL_FOURS.copy(th1 = -45f, sh1 = -100f, torso = 92f)), ms = 700)
    private val catCow = Motion(listOf(ALL_FOURS.copy(torso = 97f, head = 35f), ALL_FOURS.copy(torso = 85f, head = 135f)), ms = 1400)
    private val sideLegRaise = Motion(
        listOf(Pose(x = 56f, torso = -90f, head = -105f, ua1 = 80f, fa1 = 90f, ua2 = 200f, fa2 = 150f).legs(90f), Pose(x = 56f, torso = -90f, head = -105f, ua1 = 80f, fa1 = 90f, ua2 = 200f, fa2 = 150f, th1 = 125f, sh1 = 125f, th2 = 90f, sh2 = 90f)),
        ms = 800,
    )

    // Stretch & yoga
    private val hamstring = hold(SEATED.copy(torso = 128f, head = 112f).arms(88f, 84f))
    private val quad = hold(STAND.copy(th1 = 0f, sh1 = -168f, ua1 = -25f, fa1 = -10f, ua2 = 95f, fa2 = 95f))
    private val chestStretch = hold(STAND.copy(ua1 = -100f, fa1 = -100f)) { it.copy(wall = 20f) }
    private val crossBody = hold(F_STAND.copy(ua1 = -72f, fa1 = -78f, ua2 = 20f, fa2 = 95f), View.FRONT)
    private val figureFour = hold(LYING.copy(th1 = 150f, sh1 = 70f, th2 = 135f, sh2 = -10f).arms(130f, 120f))
    private val neck = Motion(listOf(F_STAND.copy(head = 158f), F_STAND.copy(head = 202f)), View.FRONT, 1800)
    private val tricepsStretch = hold(F_STAND.copy(ua1 = 168f, fa1 = -12f, ua2 = 150f, fa2 = 115f), View.FRONT)
    private val mountain = hold(F_STAND.symArms(12f), View.FRONT)
    private val seated = hold(Pose(torso = 180f, head = 180f).symArms(22f, 62f).symLegs(78f, -78f), View.FRONT)
    private val sunSal = Motion(
        listOf(STAND.arms(180f), FOLD, Pose(x = 50f, torso = 100f, head = 100f).arms(0f).legs(0f), FOLD, STAND.arms(180f), STAND),
        ms = 1300,
    )
    private val dog = hold(Pose(torso = 50f, head = 30f).arms(50f).legs(-25f))
    private val cobra = hold(Pose(x = 46f, torso = 140f, head = 155f).arms(-40f, 20f).legs(-90f))
    private val child = hold(Pose(x = 40f, torso = 70f, head = 78f).arms(85f, 90f).legs(55f, -90f))
    private val warrior2 = hold(Pose(ua1 = 90f, fa1 = 90f, ua2 = -90f, fa2 = -90f, th1 = 52f, sh1 = 0f, th2 = -42f, sh2 = -42f), View.FRONT)
    private val triangle = hold(Pose(torso = 130f, head = 125f, ua1 = 8f, fa1 = 8f, ua2 = 188f, fa2 = 188f, th1 = 34f, sh1 = 34f, th2 = -34f, sh2 = -34f), View.FRONT)
    private val tree = hold(Pose(ua1 = 160f, fa1 = -165f, ua2 = -160f, fa2 = 165f, th1 = 70f, sh1 = -62f, th2 = 0f, sh2 = 0f), View.FRONT)
    private val lowLunge = hold(Pose(torso = 182f, head = 185f).arms(180f).copy(th1 = 95f, sh1 = 0f, th2 = -40f, sh2 = -95f))
    private val seatedTwist = hold(SEATED.copy(ua1 = -35f, fa1 = -20f, ua2 = 65f, fa2 = 75f, th1 = 140f, sh1 = -10f))
    private val pigeon = hold(Pose(torso = 150f, head = 160f).arms(35f, 15f).copy(th1 = 80f, sh1 = -100f, th2 = -95f, sh2 = -95f))
    private val happyBaby = hold(Pose(x = 56f, torso = -90f, head = -90f).arms(140f, 160f).legs(150f, 180f))
    private val legsUpWall = hold(Pose(x = 56f, torso = -90f, head = -90f).arms(80f, 85f).legs(180f))
    private val savasana = hold(LYING_FLAT)

    private val byId: Map<String, Motion> = mapOf(
        "march" to march, "arm_circles" to armCircles, "hip_circles" to hipCircles, "torso_twist" to torsoTwist,
        "leg_swings" to legSwings, "jumping_jacks" to jumpingJack, "step_jacks" to stepJack, "cat_cow_wu" to catCow,
        "inchworm" to inchworm,
        "wall_pushup" to wallPushup, "incline_pushup" to inclinePushup, "knee_pushup" to kneePushup,
        "pushup" to pushup, "wide_pushup" to pushup, "decline_pushup" to pushup, "archer_pushup" to pushup,
        "diamond_pushup" to pushup,
        "db_floor_press" to floorPress, "db_bench_press" to floorPress, "db_squeeze_press" to floorPress,
        "db_fly" to fly, "band_chest_press" to bandPress,
        "superman" to superman, "reverse_snow_angel" to snowAngel, "prone_y_raise" to proneY, "table_row" to row,
        "db_row" to oneArmRow, "db_bent_row" to row, "db_reverse_fly" to reverseFly, "band_pull_apart" to pullApart,
        "band_row" to bandRow, "pullup" to pullup, "chinup_negative" to negative, "dead_hang" to deadHang,
        "kb_swing" to kbSwing,
        "pike_pushup" to pikePushup, "plank_shoulder_tap" to shoulderTap, "db_shoulder_press" to press,
        "db_single_press" to singlePress, "db_lateral_raise" to lateralRaise, "db_front_raise" to frontRaise,
        "db_arnold" to press, "band_overhead_press" to press, "wall_angels" to wallAngels,
        "chair_dip" to dip, "db_curl" to curl, "db_hammer_curl" to curl, "db_conc_curl" to concCurl,
        "db_overhead_ext" to overheadExt, "db_kickback" to kickback, "band_curl" to curl,
        "chair_squat" to chairSquat, "squat" to squat, "wall_sit" to wallSit, "glute_bridge" to bridge,
        "single_leg_bridge" to singleBridge, "reverse_lunge" to lunge, "side_lunge" to sideLunge,
        "calf_raise" to calfRaise, "donkey_kick" to donkeyKick, "fire_hydrant" to fireHydrant,
        "side_leg_raise" to sideLegRaise, "bulgarian_split" to bulgarian, "step_up" to stepUp,
        "db_goblet_squat" to gobletSquat, "db_rdl" to rdl, "db_lunge" to lunge, "db_sumo_squat" to sumoSquat,
        "db_hip_thrust" to bridge, "band_lateral_walk" to lateralWalk,
        "dead_bug" to deadBug, "bird_dog" to birdDog, "plank" to plank, "knee_plank" to kneePlank,
        "side_plank" to sidePlank, "crunch" to crunch, "bicycle_crunch" to bicycle, "leg_raise" to legRaise,
        "hollow_hold" to hollow, "russian_twist" to russianTwist, "heel_taps" to heelTaps, "pallof_press" to bandPress,
        "db_woodchop" to woodchop, "hanging_knee_raise" to hangingKnee,
        "high_knees" to highKnees, "butt_kicks" to buttKicks, "mountain_climbers" to climbers, "burpee" to burpee,
        "squat_jump" to squatJump, "skater" to skater, "shadow_boxing" to boxing, "fast_feet" to fastFeet,
        "low_impact_burpee" to lowBurpee, "db_thruster" to thruster,
        "hamstring_stretch" to hamstring, "quad_stretch" to quad, "chest_stretch" to chestStretch,
        "cross_body_stretch" to crossBody, "figure_four" to figureFour, "neck_release" to neck,
        "triceps_stretch" to tricepsStretch,
        "mountain_pose" to mountain, "sun_salutation" to sunSal, "downward_dog" to dog, "cobra" to cobra,
        "childs_pose" to child, "cat_cow" to catCow, "warrior_2" to warrior2, "triangle" to triangle,
        "tree_pose" to tree, "low_lunge" to lowLunge, "bridge_pose" to bridgeHold, "seated_twist" to seatedTwist,
        "pigeon" to pigeon, "happy_baby" to happyBaby, "legs_up_wall" to legsUpWall, "savasana" to savasana,
        "box_breathing" to seated, "breath_478" to seated, "coherent" to seated, "belly_breathing" to seated,
        "recovery_breath" to seated,
    )

    val all: Map<String, Motion> get() = byId

    fun of(e: Exercise?): Motion {
        if (e == null) return mountain
        byId[e.id]?.let { return it }
        return when (e.category) {
            Category.YOGA -> mountain
            Category.BREATHING -> seated
            Category.STRETCH -> hamstring
            Category.WARMUP -> march
            Category.CARDIO -> highKnees
            Category.CORE -> crunch
            else -> when (e.muscles.firstOrNull()) {
                Muscle.CHEST -> pushup
                Muscle.BACK -> row
                Muscle.SHOULDERS -> press
                Muscle.ARMS -> curl
                Muscle.CORE -> crunch
                Muscle.GLUTES -> bridge
                Muscle.LEGS -> squat
                Muscle.CARDIO -> highKnees
                null -> squat
            }
        }
    }
}
