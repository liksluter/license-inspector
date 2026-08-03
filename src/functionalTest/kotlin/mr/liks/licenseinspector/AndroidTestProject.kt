package mr.liks.licenseinspector

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/** Создает тестовый Android Gradle проект */
class AndroidTestProject(
    private val projectDir: Path,
    private val agpVersion: String,
    private val pluginId: String,
    private val pluginVersion: String,
    private val packageName: String = "com.test.app",
    private val dependenciesBlock: String,
    private val repositoriesBlock: String = "",
    private var pluginExtBlock: String = ""
) {
    init {
        writeBuildFile()
        writeSettings()
        writeGradleProperties()
        writeFakeSdk()
        writeAndroidManifest()
    }

    fun updateBuildFile(pluginExtBlock: String) {
        this.pluginExtBlock = pluginExtBlock
        writeBuildFile()
    }

    private fun writeBuildFile() {
        projectDir
            .resolve("build.gradle.kts")
            .writeText(
                """
                plugins {
                    id("com.android.application") version "$agpVersion"
                    id("$pluginId") version "$pluginVersion"
                }

                android {
                    namespace = "com.test.app"
                    compileSdk = 34

                    defaultConfig {
                        applicationId = "com.test.app"
                        minSdk = 24
                        targetSdk = 34
                        versionCode = 1
                        versionName = "1.0"
                    }
                }

                repositories {
                    google()
                    mavenCentral()
                    mavenLocal()
                    $repositoriesBlock
                }
                
                $pluginExtBlock

                dependencies {
                    $dependenciesBlock
                }
                """.trimIndent()
            )
    }

    private fun writeSettings() {
        projectDir
            .resolve("settings.gradle.kts")
            .writeText(
                """
                pluginManagement {
                    repositories {
                        mavenLocal()
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }

                rootProject.name = "license-inspector-functional-test"
                """.trimIndent()
            )
    }

    private fun writeGradleProperties() {
        projectDir
            .resolve("gradle.properties")
            .writeText(
                """
                org.gradle.unsafe.configuration-resolution=warn
                """.trimIndent()
            )
    }

    private fun writeFakeSdk() {
        val fakeSdkDir = projectDir
            .resolve("fake-android-sdk")
            .createDirectories()

        val sdkPath = fakeSdkDir.toFile()
            .absolutePath
            .replace("\\", "\\\\")

        projectDir
            .resolve("local.properties")
            .writeText("sdk.dir=$sdkPath")
    }

    private fun writeAndroidManifest() {
        val manifestPath = projectDir
            .resolve("src")
            .resolve("main")
            .resolve("AndroidManifest.xml")

        manifestPath.parent.createDirectories()
        manifestPath.writeText(
            """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$packageName">
            </manifest>
            """.trimIndent()
        )
    }
}