package mr.liks.licenseinspector

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Таска проверяющая лицензии в POM файлах */
@Suppress("TooGenericExceptionCaught", "")
abstract class InspectLicensesTask : DefaultTask() {
    /** Коллекция POM файлов */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pomFiles: ConfigurableFileCollection

    /** Список разрешенных лицензий */
    @get:Input
    abstract val allowedLicenses: ListProperty<String>

    /** Список игнорируемых при проверке зависимостей */
    @get:Input
    @get:Optional
    abstract val ignoredDependencies: ListProperty<String>

    /** Файл с отчетом */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun inspectLicenses() {
        val violations = mutableListOf<String>()
        val reportBuilder = StringBuilder()
        val allowedListNormalized = allowedLicenses.get().map { it.lowercase() }
        val ignoredPatterns = ignoredDependencies.get().map { Regex(it) }

        println(String.format(Locale.getDefault(), POM_FILES_COUNT, pomFiles.files.size))

        pomFiles.files.forEach { pomFile ->
            val dependencyName = pomFile.name.removeSuffix(POM_FILE_EXT)
            val isDependencyIgnored = ignoredPatterns.any { pattern -> pattern.containsMatchIn(dependencyName) }

            if (isDependencyIgnored) {
                reportBuilder.appendLine(String.format(SKIPPED, dependencyName))
                return@forEach
            }

            val licenseTokens = extractLicenseTokensFromPom(pomFile)

            reportBuilder.appendLine("$dependencyName -> $licenseTokens")

            if (licenseTokens.isEmpty()) {
                violations.add(String.format(LICENSE_NOT_FOUND, dependencyName))
                return@forEach
            }

            val hasValidLicense = licenseTokens.any { license ->
                allowedListNormalized.any { allowedKeyword -> license.lowercase().contains(allowedKeyword) }
            }

            if (!hasValidLicense) {
                violations.add("$dependencyName - ${licenseTokens.first()}")
            }
        }

        reportFile.get().asFile.writeText(reportBuilder.toString())

        if (violations.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine(TITLE)
                appendLine(UNALLOWABLE_FOUND)
                violations.forEach { appendLine("   * $it") }
                appendLine(String.format(ALLOWED_LICENSES, allowedLicenses.get().joinToString(", ")))
                appendLine(IGNORED_DEPS_HINT)
                appendLine(FOOTER)
            }

            throw GradleException(errorMessage)
        }
    }

    @Suppress("NestedBlockDepth")
    private fun extractLicenseTokensFromPom(pomFile: File): List<String> {
        val licenseTokens = mutableListOf<String>()

        try {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            try {
                documentBuilderFactory.setFeature(DISALLOW_DOCTYPE_DECL_FEATURE, true)
            } catch (ex: ParserConfigurationException) {
                logger.error("$TAG $ex")
            }

            val documentBuilder = documentBuilderFactory.newDocumentBuilder()
            val document = documentBuilder.parse(pomFile)

            document.documentElement.normalize()

            val licenseNodes = document.getElementsByTagName(LICENSE_TAG)

            for (i in 0 until licenseNodes.length) {
                val node = licenseNodes.item(i)
                val childNodes = node.childNodes

                for (j in 0 until childNodes.length) {
                    val childNode = childNodes.item(j)

                    if (childNode.nodeName == NAME_NODE_NAME || childNode.nodeName == URL_NODE_NAME) {
                        val childNodeContent = childNode.textContent.trim()

                        if (childNodeContent.isNotEmpty()) {
                            licenseTokens.add(childNodeContent)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            licenseTokens.add(String.format(ERROR_READING_POM, pomFile.name, e.localizedMessage))
        }

        return licenseTokens
    }

    private companion object {
        const val TAG = "InspectLicensesTask"
        const val POM_FILE_EXT = ".pom"
        const val POM_FILES_COUNT = "[License inspector] POM files count: %d"
        const val LICENSE_NOT_FOUND = "%s - The license is not specified in the POM file"
        const val TITLE = "[License inspector] ============================================================"
        const val UNALLOWABLE_FOUND = "Dependencies found with unallowable license types:"
        const val ALLOWED_LICENSES = "Allowed licenses: %s."
        const val IGNORED_DEPS_HINT = "To exclude a dependency from checking, add it to 'ignoredDependencies'."
        const val FOOTER = "================================================================================"
        const val SKIPPED = "%s -> Skipped (from ignoredDependencies)"
        const val LICENSE_TAG = "license"
        const val NAME_NODE_NAME = "name"
        const val URL_NODE_NAME = "url"
        const val DISALLOW_DOCTYPE_DECL_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl"
        const val ERROR_READING_POM = "Error reading POM %s : %s"
    }
}