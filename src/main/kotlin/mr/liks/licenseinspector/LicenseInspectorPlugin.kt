package mr.liks.licenseinspector

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.util.Locale

/** Регистрирует таску, которая проверяет лицензии зависимостей проекта и останавливает сборку при нахождении
 * ограничительных лицензий. Имя таски состоит из префикса `inspectLicensesFor` и имени варианта сборки с заглавной
 * буквы, например: inspectLicensesForDebug. Для настройки плагин создает расширение со следующими параметрами:
 * * `allowedLicenses` список разрешенных лицензий, значение по-умолчанию [allowedLicensesDefaultList]
 * * `ignoredDependencies` список исключаемых из проверки зависимостей
 */
class LicenseInspectorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(PLUGIN_EXT_NAME, LicenseInspectorExtension::class.java).apply {
            allowedLicenses.set(allowedLicensesDefaultList)
            ignoredDependencies.set(emptyList())
        }

        project.pluginManager.withPlugin(ANDROID_APP_PLUGIN_ID) {
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val variantNameCapitalized = variant.name.replaceFirstChar(::makeCharTitlecase)
                val taskName = "$TASK_NAME_PREFIX$variantNameCapitalized"

                val checkLicensesTask = project.tasks.register(taskName, InspectLicensesTask::class.java) {
                    checkExtensionParams(extension)
                    allowedLicenses.set(extension.allowedLicenses)
                    ignoredDependencies.set(extension.ignoredDependencies)

                    val runtimeConfigName = "${variant.name}$RUNTIME_CLASSPATH"
                    val runtimeConfiguration = project.configurations.getByName(runtimeConfigName)

                    val pomFilesProvider = project.provider {
                        val componentIds = runtimeConfiguration.incoming.resolutionResult.allComponents
                            .map { it.id }
                            .filterIsInstance<ModuleComponentIdentifier>()

                        val artifactResult = project.dependencies.createArtifactResolutionQuery()
                            .forComponents(componentIds)
                            .withArtifacts(
                                MavenModule::class.java,
                                MavenPomArtifact::class.java
                            )
                            .execute()

                        artifactResult.resolvedComponents.flatMap { component ->
                            component.getArtifacts(MavenPomArtifact::class.java)
                                .filterIsInstance<ResolvedArtifactResult>()
                                .map { it.file }
                        }
                    }

                    pomFiles.setFrom(pomFilesProvider)
                    reportFile.set(project.layout.buildDirectory.file(runtimeConfigName.createReportFilePathFromName()))
                }

                project.tasks.configureEach {
                    if (name.isCompileTask(variantNameCapitalized)) {
                        dependsOn(checkLicensesTask)
                    }
                }
            }
        }
    }

    private fun checkExtensionParams(extension: LicenseInspectorExtension) {
        extension.allowedLicenses.get().forEach(::validateString)
        extension.ignoredDependencies.get().forEach(::validateString)
    }

    private fun validateString(value: String) {
        if (value.isBlank()) {
            throw IllegalArgumentException("A list item cannot be empty or contain only whitespaces.")
        }
    }

    private fun String.isCompileTask(variantNameCapitalized: String): Boolean =
        this == "$COMPILE$variantNameCapitalized$JAVA_WITH_JAVAC" || this == "$COMPILE$variantNameCapitalized$KOTLIN"

    private fun String.createReportFilePathFromName(): String = "$REPORT_FILE_PATH$this$REPORT_FILE_EXT"

    private fun makeCharTitlecase(char: Char): CharSequence =
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()

    private companion object {
        const val PLUGIN_EXT_NAME = "licenseInspector"
        const val ANDROID_APP_PLUGIN_ID = "com.android.application"
        const val TASK_NAME_PREFIX = "inspectLicensesFor"
        const val REPORT_FILE_PATH = "reports/license-inspector/"
        const val REPORT_FILE_EXT = ".txt"
        const val COMPILE = "compile"
        const val RUNTIME_CLASSPATH = "RuntimeClasspath"
        const val JAVA_WITH_JAVAC = "JavaWithJavac"
        const val KOTLIN = "Kotlin"
        val allowedLicensesDefaultList = listOf("mit", "apache", "bsd", "public domain")
    }
}