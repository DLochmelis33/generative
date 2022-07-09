import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.Random
import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.math.Vector2

const val optimalDistance = 150.0
const val forceCoef = 0.15

fun main() = application {
    configure {
        width = 1080
        height = 720
//        fullscreen = Fullscreen.SET_DISPLAY_MODE
    }
    program {
        Random.seed = System.currentTimeMillis().toString()
        val points = List(50) { drawer.bounds.center + drawer.bounds.center * Random.vector2() }.toMutableList()
        var userOverrideIndex: Int? = null

        mouse.buttonDown.listen { event ->
            userOverrideIndex = points.indexOfFirst { (it - event.position).length < 10 }.takeUnless { it == -1 }
        }
        mouse.buttonUp.listen {
            userOverrideIndex = null
        }

        extend {
            val triangles = Delaunay.from(points).triangles()
            val edges = triangles.flatMap { it.contour.segments }

            val forceSums = points.map { Vector2.ZERO }.toMutableList()
            for (e in edges) {
                val si = points.indexOf(e.start)
                val ei = points.indexOf(e.end)

                var f = e.end - e.start
                f -= f.normalized * optimalDistance // force towards optimal distance

                forceSums[si] += f
                forceSums[ei] -= f
            }

            for ((i, f) in forceSums.withIndex()) {
                if (i == userOverrideIndex) continue
                points[i] += f * forceCoef
            }

            userOverrideIndex?.let { points[it] = mouse.position }

            with(drawer) {
                stroke = ColorRGBa.WHITE
                fill = ColorRGBa.PINK
                strokeWeight = 0.1
                segments(edges)
                circles(points, 5.0)
            }
        }
    }
}
