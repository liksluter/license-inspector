package mr.liks.licenseinspector

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/** Базовый класс для функциональных тестов */
abstract class FunctionalTestBase {
    @TempDir
    lateinit var projectPath: Path

    /** Создаёт локальный Maven‑репозиторий в `<projectPath>/maven-repo` с артефактом `group:artifact:version` и заданным POM. */
    protected fun writeLocalMavenArtifact(
        group: String,
        artifact: String,
        version: String,
        pomContent: String,
    ) {
        val artifactDir = projectPath
            .resolve("maven-repo")
            .resolve(group.replace('.', '/'))
            .resolve(artifact)
            .resolve(version)

        artifactDir.createDirectories()

        artifactDir
            .resolve("$artifact-$version.pom")
            .writeText(pomContent)

        Files.write(artifactDir.resolve("$artifact-$version.jar"), ByteArray(0))
    }

    /** @return текст блока `repositories` для использования локального maven-repo. */
    protected fun localMavenRepositoryBlock(): String =
        """
        maven {
            url = uri(projectDir.resolve("maven-repo"))
        }
        """.trimIndent()

    /** Запускает Gradle с заданными аргументами. */
    protected fun runGradle(
        vararg args: String,
        expectFailure: Boolean = false,
    ): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectPath.toFile())
            .withArguments(*args)
            .forwardOutput()
            .run {
                if (expectFailure) buildAndFail() else build()
            }
}