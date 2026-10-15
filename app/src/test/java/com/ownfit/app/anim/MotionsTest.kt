package com.ownfit.app.anim

import com.ownfit.app.data.ExerciseLibrary
import com.ownfit.app.data.Gender
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MotionsTest {

    @Test fun everyBuiltInExerciseHasItsOwnMotion() {
        val missing = ExerciseLibrary.all.map { it.id }.filter { it !in Motions.all }
        assertTrue("No motion for: $missing", missing.isEmpty())
    }

    @Test fun motionsStayInBounds() {
        val b = Kinematics.build(Gender.MALE)
        Motions.all.forEach { (id, m) ->
            val bb = Kinematics.bounds(m, b)
            assertTrue("$id too wide: ${bb.maxX - bb.minX}", bb.maxX - bb.minX < 160f)
            assertTrue("$id too tall: ${bb.maxY - bb.minY}", bb.maxY - bb.minY < 140f)
        }
    }

    /** Dumps 4 phases per motion as JSON (for scripts/render_motions.py visual review). */
    @Test fun dumpFrames() {
        val out = File("build/anim").apply { mkdirs() }
        val sb = StringBuilder("[")
        ExerciseLibrary.all.forEachIndexed { i, e ->
            val gender = if (i % 2 == 0) Gender.MALE else Gender.FEMALE
            val b = Kinematics.build(gender)
            val m = Motions.all.getValue(e.id)
            val bb = Kinematics.bounds(m, b)
            if (i > 0) sb.append(",")
            sb.append("{\"id\":\"${e.id}\",\"front\":${m.view == View.FRONT},")
            sb.append("\"b\":[${bb.minX},${bb.maxX},${bb.minY},${bb.maxY}],")
            sb.append("\"w\":[${b.upperArm},${b.forearm},${b.thigh},${b.shin},${b.torso}],")
            m.bench?.let { sb.append("\"bench\":[${it.x},${it.w},${it.h}],") }
            m.bar?.let { sb.append("\"bar\":[${it.x},${it.y}],") }
            m.wall?.let { sb.append("\"wall\":$it,") }
            sb.append("\"frames\":[")
            for (ph in 0 until 4) {
                val s = Kinematics.place(Kinematics.poseAt(m, (m.duration * ph / 4).toLong()), m, b)
                if (ph > 0) sb.append(",")
                val pts = listOf(s.hip, s.hip1, s.hip2, s.neck, s.sh1, s.sh2, s.head, s.el1, s.ha1, s.el2, s.ha2, s.kn1, s.an1, s.kn2, s.an2)
                sb.append(pts.joinToString(",", "[", "]") { "[${it.x},${it.y}]" })
            }
            sb.append("]}")
        }
        sb.append("]")
        File(out, "frames.json").writeText(sb.toString())
    }
}
