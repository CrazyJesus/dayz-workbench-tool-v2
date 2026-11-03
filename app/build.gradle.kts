import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(25)
}

group = "com.github.demandss"
version = rootProject.version

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":layout-editor"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")

    testImplementation(kotlin("test"))
    // Tests of the windows: they are drawn without a screen and clicked by the tests
    testImplementation(libs.compose.uitest)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-XX:ErrorFile=logs/hs_err_pid%p.log",
        "-XX:ReplayDataFile=logs/replay_pid%p.log",
        "--enable-native-access=ALL-UNNAMED",
    )
}

compose.desktop {
    application {
        mainClass = "com.github.demandss.dayz_workbench_tool.MainKt"

        // ProGuard optimization corrupts stack map frames in LayoutCanvas's draw lambda.
        buildTypes.release.proguard { optimize.set(false) }

        // Crash dumps go to logs/ (ignored by git) instead of the working directory
        jvmArgs +=
            listOf(
                "-XX:ErrorFile=logs/hs_err_pid%p.log",
                "-XX:ReplayDataFile=logs/replay_pid%p.log",
                "--enable-native-access=ALL-UNNAMED",
            )

        javaHome =
            javaToolchains
                .launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }
                .get()
                .metadata
                .installationPath
                .asFile
                .absolutePath

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Dayz Workbench Tool"
            packageVersion = version.toString()
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.management",
                "java.naming",
                "java.net.http",
                "jdk.unsupported",
            )
            windows {
                iconFile.set(project.file("src/main/resources/assets/logo/logo.ico"))
                upgradeUuid = "1A25F3F9-6694-4C1F-B9EA-B0C0A69FD313"
                perUserInstall = true
                menuGroup = "Dayz Workbench Tool"
                menu = true
                dirChooser = true
                shortcut = true
            }
        }
    }
}

tasks.register<Zip>("packagePortable") {
    group = "compose desktop"
    description = "Packages the release distributable into a portable zip"
    dependsOn("createReleaseDistributable")
    archiveFileName.set("DayzWorkbenchTool.zip")
    destinationDirectory.set(layout.buildDirectory.dir("release"))
    from(layout.buildDirectory.dir("compose/binaries/main-release/app"))
}
