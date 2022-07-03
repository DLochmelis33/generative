import org.openrndr.application
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.orml.u2net.U2Net

fun main() = application {
    configure {
        width = 800
        height = 600
    }
    program {
        val vp = loadVideoDevice()
        vp.play()
        extend {
            vp.draw(drawer)
        }
    }
}
