import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

/* the name of this project, default is the template version but you are free to change these */
group = "org.openrndr.template"
version = "0.4.0"

val applicationMainClass = "TemplateProgramKt"

/*  Which additional (ORX) libraries should be added to this project. */
val orxFeatures = setOf(
//  "orx-boofcv",
//  "orx-camera",
//  "orx-chataigne",
//  "orx-color",
    "orx-compositor",
//  "orx-dnk3",
//  "orx-easing",
//  "orx-file-watcher",
//  "orx-filter-extension",
    "orx-fx",
//  "orx-glslify",
//  "orx-gradient-descent",
    "orx-gui",
    "orx-image-fit",
//  "orx-integral-image",
//  "orx-interval-tree",
//  "orx-jumpflood",
//  "orx-kdtree",
//  "orx-keyframer",      
//  "orx-kinect-v1",
//  "orx-kotlin-parser",
//  "orx-mesh-generators",
//  "orx-midi",
//  "orx-no-clear",
    "orx-noise",
//  "orx-obj-loader",
    "orx-olive",
//  "orx-osc",
//  "orx-palette",
    "orx-panel",
//  "orx-parameters",
//  "orx-poisson-fill",
//  "orx-rabbit-control",
//  "orx-realsense2",
//  "orx-runway",
    "orx-shade-styles",
//  "orx-shader-phrases",
//  "orx-shapes",
//  "orx-syphon",
//  "orx-temporal-blur",
//  "orx-tensorflow",    
//  "orx-time-operators",
//  "orx-timer",
//  "orx-triangulation",
//  "orx-video-profiles",
    null
).filterNotNull()

val ormlFeatures = setOf<String>(
//    "orml-blazepose",
//    "orml-dbface",
//    "orml-facemesh",
//    "orml-image-classifier",
//    "orml-psenet",
//    "orml-ssd",
//    "orml-style-transfer",
//    "orml-super-resolution",
//    "orml-u2net"
)

/* Which OPENRNDR libraries should be added to this project? */
val openrndrFeatures = setOf(
    "video"
)

/*  What type of logging should this project use? */
enum class Logging { NONE, SIMPLE, FULL }

val applicationLogging = Logging.FULL

// ------------------------------------------------------------------------------------------------------------------ //

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
}

repositories {
    mavenCentral()
}

dependencies {

//    implementation(libs.jsoup)
//    implementation(libs.gson)
//    implementation(libs.csv)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)

    when (applicationLogging) {
        Logging.NONE -> {
            runtimeOnly(libs.slf4j.nop)
        }
        Logging.SIMPLE -> {
            runtimeOnly(libs.slf4j.simple)
        }
        Logging.FULL -> {
            runtimeOnly(libs.log4j.slf4j)
            runtimeOnly(libs.jackson.databind)
            runtimeOnly(libs.jackson.json)
        }
    }
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.junit)
}

// ------------------------------------------------------------------------------------------------------------------ //

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

// ------------------------------------------------------------------------------------------------------------------ //

project.setProperty("mainClassName", applicationMainClass)
tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = applicationMainClass
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
        }
    }
    named<org.beryx.runtime.JPackageTask>("jpackage") {
        doLast {
            when (OperatingSystem.current()) {
                OperatingSystem.WINDOWS, OperatingSystem.LINUX -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application/data")
                    }
                }
                OperatingSystem.MAC_OS -> {
                    copy {
                        from("data") {
                            include("**/*")
                        }
                        into("build/jpackage/openrndr-application.app/data")
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks.register<Zip>("jpackageZip") {
    archiveFileName.set("openrndr-application.zip")
    from("$buildDir/jpackage") {
        include("**/*")
    }
}
tasks.findByName("jpackageZip")?.dependsOn("jpackage")

// ------------------------------------------------------------------------------------------------------------------ //

runtime {
    jpackage {
        imageName = "openrndr-application"
        skipInstaller = true
        if (OperatingSystem.current() == OperatingSystem.MAC_OS) {
            jvmArgs.add("-XstartOnFirstThread")
        }
    }
    options.empty()
    options.add("--strip-debug")
    options.add("--compress")
    options.add("1")
    options.add("--no-header-files")
    options.add("--no-man-pages")
    modules.empty()
    modules.add("jdk.unsupported")
    modules.add("java.management")
}

// ------------------------------------------------------------------------------------------------------------------ //

class Openrndr {
    val openrndrVersion = libs.versions.openrndr.get()
    val orxVersion = libs.versions.orx.get()
    val ormlVersion = libs.versions.orml.get()

    // choices are "orx-tensorflow-gpu", "orx-tensorflow-mkl", "orx-tensorflow"
    val orxTensorflowBackend = "orx-tensorflow-mkl"

    val os = if (project.hasProperty("targetPlatform")) {
        val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
        val platform: String = project.property("targetPlatform") as String
        if (platform !in supportedPlatforms) {
            throw IllegalArgumentException("target platform not supported: $platform")
        } else {
            platform
        }
    } else when (OperatingSystem.current()) {
        OperatingSystem.WINDOWS -> "windows"
        OperatingSystem.MAC_OS -> "macos"
        OperatingSystem.LINUX -> when (val h = DefaultNativePlatform("current").architecture.name) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $h")
        }
        else -> throw IllegalArgumentException("os not supported")
    }

    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
    fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
    fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
    fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orxVersion"

    init {
        repositories {
            listOf(openrndrVersion, orxVersion, ormlVersion).any { it.contains("SNAPSHOT") }.ifTrue { mavenLocal() }
            maven(url = "https://maven.openrndr.org")
        }
        dependencies {
            runtimeOnly(openrndr("gl3"))
            runtimeOnly(openrndrNatives("gl3"))
            implementation(openrndr("openal"))
            runtimeOnly(openrndrNatives("openal"))
            implementation(openrndr("application"))
            implementation(openrndr("svg"))
            implementation(openrndr("animatable"))
            implementation(openrndr("extensions"))
            implementation(openrndr("filter"))
            if ("video" in openrndrFeatures) {
                implementation(openrndr("ffmpeg"))
                runtimeOnly(openrndrNatives("ffmpeg"))
            }
            for (feature in orxFeatures) {
                implementation(orx(feature))
            }
            for (feature in ormlFeatures) {
                implementation(orml(feature))
            }
            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
        }
    }
}

val openrndr = Openrndr()
