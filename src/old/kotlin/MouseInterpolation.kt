import org.apache.commons.math.analysis.interpolation.SplineInterpolator
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import java.util.*
import kotlin.math.abs

const val persist = 3.0
const val circleSize = 30.0
const val interpolationWindow = 30

infix fun Double.closeTo(d: Double) = abs(this - d) < 1e-8

data class TimedPosition(
    val pos: Vector2,
    val t: Double,
)

fun interpolateLastPart(posList: List<TimedPosition>, timeStep: Double = 0.0002): List<TimedPosition> {
    if (posList.size < 3) return posList

    val interpolator = SplineInterpolator()

    val ts = posList.map { it.t }.toDoubleArray()
    val xs = posList.map { it.pos.x }.toDoubleArray()
    val ys = posList.map { it.pos.y }.toDoubleArray()

    val xf = interpolator.interpolate(ts, xs)
    val yf = interpolator.interpolate(ts, ys)
    fun getPos(t: Double) = Vector2(xf.value(t), yf.value(t))

    val result = mutableListOf<TimedPosition>()
    val p1 = posList[posList.size - 2]
    val p2 = posList[posList.size - 1]
    var t = p1.t
    while (t < p2.t) {
        result.add(TimedPosition(getPos(t), t))
        t += timeStep
    }
    return result
}

fun main() {
    application {
        configure {
            width = 1200
            height = 700
        }
        program {
            val interpolatedPositions = mutableListOf<TimedPosition>()
            val mousePositions = LinkedList<TimedPosition?>() as Queue<TimedPosition?>

            fun mouseListener(it: MouseEvent) {
                mousePositions.add(TimedPosition(it.position, seconds))
                if (it.type == MouseEventType.BUTTON_UP) {
                    mousePositions.add(null)
                }
                if (mousePositions.size > interpolationWindow) {
                    mousePositions.poll()
                }
            }
            mouse.dragged.listen(::mouseListener)
            mouse.buttonDown.listen(::mouseListener)
            mouse.buttonUp.listen(::mouseListener)

            extend {
                val posListS = mutableListOf<MutableList<TimedPosition>>()
                var tmpList = mutableListOf<TimedPosition>()
                for (mp in mousePositions) {
                    if (mp == null) {
                        posListS.add(tmpList)
                        tmpList = mutableListOf()
                    } else {
                        tmpList.add(mp)
                    }
                }
                if (tmpList.isNotEmpty()) posListS.add(tmpList)

                for (posList in posListS) {
                    val repeatedTs = posList.asSequence()
                        .zipWithNext()
                        .filter { (p1, p2) -> p1.t closeTo p2.t }
                        .flatMap { listOf(it.first, it.second) }
                        .map { it.t }
                        .toSet()
                    posList.removeAll { it.t in repeatedTs }
                    interpolatedPositions.addAll(interpolateLastPart(posList))
                }

                interpolatedPositions.removeAll { seconds - it.t > persist }

                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = null
                drawer.circles {
                    for ((pos, t) in interpolatedPositions) {
                        fill = ColorRGBa.PINK.shade(1 - seconds + t)
                        circle(pos, circleSize)
                    }
                }
            }
        }
    }
}