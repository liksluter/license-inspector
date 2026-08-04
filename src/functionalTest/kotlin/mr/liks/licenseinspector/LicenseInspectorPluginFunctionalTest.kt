@file:Suppress(
    "ktlint:standard:no-trailing-spaces",
    "ktlint:standard:string-template-indent",
    "ktlint:standard:indent",
    "ktlint:standard:multiline-if-else",
    "ktlint:standard:if-else-wrapping",
    "ktlint:standard:if-else-bracing",
)

package mr.liks.licenseinspector

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists

/** Тесты на [LicenseInspectorPlugin] */
class LicenseInspectorPluginFunctionalTest : FunctionalTestBase() {
    private lateinit var androidTestProject: AndroidTestProject

    @BeforeEach
    fun setUp() {
        androidTestProject = AndroidTestProject(
            projectDir = projectPath,
            agpVersion = AGP_VERSION,
            pluginId = PLUGIN_ID,
            pluginVersion = PLUGIN_VERSION,
            dependenciesBlock = createDependencyBlock(),
            repositoriesBlock = localMavenRepositoryBlock(),
        )
    }

    @Test
    fun `build success when dependency has allowed license`() {
        val apachePom = createPom(
            "The Apache Software License, Version 2.0",
            "http://www.apache.org/licenses/LICENSE-2.0.txt"
        )
        
        writeLocalMavenArtifact(apachePom)

        runGradle("inspectLicensesForDebug", "--stacktrace", "--info")
    }

    @Test
    fun `build fails when dependency has empty license and report is generated`() {
        val emptyLicensePom = createPom()

        writeLocalMavenArtifact(emptyLicensePom)

        val result = runGradle("inspectLicensesForDebug", "--stacktrace", "--info", expectFailure = true)

        assertTrue(
            result.output.contains("Dependencies found with unallowable license types:")
        )

        assertTrue(
            resolveReportPath().exists()
        )
    }

    @Test
    fun `build success when dependency has empty license in ignoredDependencies`() {
        val emptyLicensePom = createPom()
        val pluginExtBlock = createPluginExtensionBlock(ignoredDependencies = listOf("$DEPENDENCY_ARTIFACT-$DEPENDENCY_VERSION"))

        writeLocalMavenArtifact(emptyLicensePom)
        androidTestProject.updateBuildFile(pluginExtBlock)

        runGradle("inspectLicensesForDebug", "--stacktrace", "--info")
    }

    @Test
    fun `build fails when dependency has disallowed license and report is generated`() {
        val gnuPom = createPom(
            "GNU GENERAL PUBLIC LICENSE, Version 3",
            "https://www.gnu.org/licenses/gpl-3.0.en.html"
        )

        writeLocalMavenArtifact(gnuPom)

        val result = runGradle("inspectLicensesForDebug", "--stacktrace", "--info", expectFailure = true)

        assertTrue(
            result.output.contains("Dependencies found with unallowable license types:")
        )

        assertTrue(
            resolveReportPath().exists()
        )
    }

    @Test
    fun `build success when dependency has disallowed license in allowedLicenses`() {
        val gnuPom = createPom(
            "GNU GENERAL PUBLIC LICENSE, Version 3",
            "https://www.gnu.org/licenses/gpl-3.0.en.html"
        )
        val pluginExtBlock = createPluginExtensionBlock(allowedLicenses = listOf("gnu"))

        writeLocalMavenArtifact(gnuPom)
        androidTestProject.updateBuildFile(pluginExtBlock)

        runGradle("inspectLicensesForDebug", "--stacktrace", "--info")
    }

    private fun createPluginExtensionBlock(allowedLicenses: List<String>? = null, ignoredDependencies: List<String>? = null): String {
        val allowedList = allowedLicenses.toPluginExtList("allowedLicenses.addAll(listOf(", "))")
        val ignoredList = ignoredDependencies.toPluginExtList("ignoredDependencies.set(listOf(", "))")

        return if (allowedList.isNotEmpty() || ignoredList.isNotEmpty()) {
            """
            licenseInspector {
                $allowedList
                $ignoredList
            }
        """.trimIndent()
        } else ""
    }

    private fun List<String>?.toPluginExtList(prefix: String, postfix: String) = this?.run {
        """$prefix${this.joinToString(separator = ",", prefix = "\"", postfix = "\"")}$postfix"""
    } ?: ""

    private fun resolveReportPath(): Path = projectPath
        .resolve("build")
        .resolve("reports")
        .resolve("license-inspector")
        .resolve("debugRuntimeClasspath.txt")

    private fun writeLocalMavenArtifact(pomContent: String) =
        writeLocalMavenArtifact(DEPENDENCY_GROUP, DEPENDENCY_ARTIFACT, DEPENDENCY_VERSION, pomContent)

    private fun createDependencyBlock(
        group: String = DEPENDENCY_GROUP,
        artifact: String = DEPENDENCY_ARTIFACT,
        version: String = DEPENDENCY_VERSION,
    ) = """implementation("$group:$artifact:$version")"""

    private fun createPom(
        licenseName: String? = null,
        licenseUrl: String? = null,
    ): String {
        val licensesBlock = if (licenseName != null || licenseUrl != null) {
            """
                <licenses>
                        <license>
                            <name>$licenseName</name>
                            <url>$licenseUrl</url>
                        </license>
                    </licenses>
            """.trimIndent()
        } else ""

        return """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.test</groupId>
                    <artifactId>dependency</artifactId>
                    <version>1.0.0</version>
                    <name>dependency</name>
        """.trimIndent() + licensesBlock + """
            </project>
        """.trimIndent()
    }

    private companion object {
        const val AGP_VERSION = "9.3.0"
        const val PLUGIN_ID = "mr.liks.license-inspector"
        const val PLUGIN_VERSION = "1.0.0"
        const val DEPENDENCY_GROUP = "com.test"
        const val DEPENDENCY_ARTIFACT = "dependency"
        const val DEPENDENCY_VERSION = "1.0.0"
    }
}