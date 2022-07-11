import org.openrndr.Fullscreen
import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.*
import org.openrndr.math.Vector2

const val cols: Int = 16 * 10
const val rows: Int = 9 * 10

const val noisePower: Double = 0.05
const val noiseSpeed: Double = 1.0
const val noiseUniformity: Double = 2000.0
const val snapCoef: Double = 0.0005

val randomSeed: Long = System.currentTimeMillis()

fun main() = application {
    configure {
        width = 1980
        height = 1080
        fullscreen = Fullscreen.SET_DISPLAY_MODE
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        backgroundColor = ColorRGBa.WHITE

        Random.seed = randomSeed.toString()

        val initPoints: List<Vector2> = run {
            val colW = 1.0 * width / cols
            val rowH = 1.0 * height / rows
            val hidColHalfCount = (width / 2.0 / colW).toInt()
            val hidRowHalfCount = (height / 2.0 / rowH).toInt()
            val fromX = -hidColHalfCount * colW
            val fromY = -hidRowHalfCount * rowH

            List(rows + hidRowHalfCount * 2) { i ->
                List(cols + hidColHalfCount * 2) { j ->
                    Vector2(fromX + j * colW, fromY + i * rowH)
                }
            }.flatten()
        }

        val points = initPoints.toMutableList()
        val velos = points.map { Vector2.ZERO }.toMutableList()

        extend {
            for (i in points.indices) {
                val p = points[i]
                val noiseUniformCoef = noiseUniformity
                val noise: Vector2 = gradient3D(
                    simplex3D,
                    randomSeed.hashCode(),
                    p.x / noiseUniformCoef,
                    p.y / noiseUniformCoef,
                    seconds * noiseSpeed
                ).xy
                velos[i] += noise * noisePower
                velos[i] += (initPoints[i] - p) * snapCoef

                // bounce off border
//                val outsideMargin = 20.0
//                if (p.x !in -outsideMargin..(drawer.bounds.width + outsideMargin)) velos[i] =
//                    Vector2(-velos[i].x, velos[i].y)
//                if (p.y !in -outsideMargin..(drawer.bounds.height + outsideMargin)) velos[i] =
//                    Vector2(velos[i].x, -velos[i].y)

                velos[i] = velos[i] * 0.99

                points[i] += velos[i]
            }

            drawer.fill = ColorRGBa.BLACK
            drawer.stroke = ColorRGBa.BLACK
            drawer.circles(points, 0.5)
        }
    }
}