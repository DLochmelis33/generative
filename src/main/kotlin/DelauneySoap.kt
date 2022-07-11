import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment
import kotlin.math.pow

const val optimalDistance = 200.0
const val forceCoef = 0.01

fun main() = application {
    configure {
        width = 1080
        height = 720
//        fullscreen = Fullscreen.SET_DISPLAY_MODE
    }
    program {
        Random.seed = System.currentTimeMillis().toString()
        val points = List(500) { drawer.bounds.center + drawer.bounds.center * Random.vector2() * 3.0 }.toMutableList()
        var userOverrideIndex: Int? = null

        mouse.buttonDown.listen { event ->
            userOverrideIndex = points.indexOfFirst { (it - event.position).length < 10 }.takeUnless { it == -1 }
        }
        mouse.buttonUp.listen {
            userOverrideIndex = null
        }

//        val edgesIdxPairs = points
//            .flatMap { p -> List(3) { Segment(p, points.random()) } }
//            .filter { it.length > 0.0001 }
//            .map { Pair(points.indexOf(it.start), points.indexOf(it.end)) }
        val edgesIdxPairs = Delaunay.from(points)
            .triangles()
            .flatMap { it.contour.segments }
            .map { Pair(points.indexOf(it.start), points.indexOf(it.end)) }

        extend {
//            val triangles = Delaunay.from(points).triangles()
//            val edges = triangles.flatMap { it.contour.segments }
            val edges: List<Segment> = edgesIdxPairs.map { Segment(points[it.first], points[it.second]) } +
                    points.flatMap { p -> points.filter { (it - p).length < optimalDistance && it != p }.map { Segment(it, p) } }

            val forceSums = points.map { Vector2.ZERO }.toMutableList()
            for (e in edges) {
                val si = points.indexOf(e.start)
                val ei = points.indexOf(e.end)

                var f = e.end - e.start
                val opt = f.normalized * optimalDistance
//                val dist = (f - opt).length
                f -= opt // force towards optimal distance

                forceSums[si] += f
                forceSums[ei] -= f
            }

            for ((i, f) in forceSums.withIndex()) {
                if (i == userOverrideIndex) continue
                points[i] += f * forceCoef
            }

            userOverrideIndex?.let { points[it] = mouse.position }

            // artificial nudging
            val ri1 = (seconds / 10.0 % points.size).toInt()
            val ri2 = (seconds / 6.0 % points.size).toInt()
            points[ri1] = points[ri2]

            with(drawer) {
                stroke = ColorRGBa.WHITE
                fill = ColorRGBa.PINK
                strokeWeight = 0.1
                segments(edges)
                circles(points, 3.0)
            }
        }
    }
}
