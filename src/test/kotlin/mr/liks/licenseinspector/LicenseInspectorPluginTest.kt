package mr.liks.licenseinspector

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Тесты на [LicenseInspectorPlugin] */
class LicenseInspectorPluginTest {
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().build()

        project.plugins.apply(ANDROID_APP_PLUGIN_ID)

        val androidExtension = project.extensions.getByType(ApplicationExtension::class.java)
        androidExtension.compileSdk = 36
        androidExtension.namespace = NAMESPACE

        project.plugins.apply(LicenseInspectorPlugin::class.java)
    }

    @Test
    fun `plugin registers task`() {
        val task = project.getAllTasks(true).values.asSequence()
            .flatMap { it }
            .firstOrNull { it.name.contains(TASK_NAME) }

        assertNotNull(task)
    }

    @Test
    fun `plugin creates extension`() {
        val extension = project.extensions.findByName(PLUGIN_EXT_NAME)

        assertNotNull(extension)
    }

    @Test
    fun `extension properties should contains correct default values`() {
        val extension = project.extensions.getByType(LicenseInspectorExtension::class.java)

        assertEquals(allowedLicensesDefaultList, extension.allowedLicenses.get())
        assertTrue(extension.ignoredDependencies.get().isEmpty())
    }

    @Test
    fun `should allow configuring extension properties`() {
        val allowedList = listOf("allowed1", "allowed2")
        val ignoredList = listOf("ignored1", "ignored2")
        val extension = project.extensions.getByType(LicenseInspectorExtension::class.java)

        extension.allowedLicenses.set(allowedList)
        extension.ignoredDependencies.set(ignoredList)

        val task = project.getAllTasks(true).values.asSequence()
            .flatMap { it }
            .first { it.name.contains(TASK_NAME) } as InspectLicensesTask

        assertEquals(allowedList, task.allowedLicenses.get())
        assertEquals(ignoredList, task.ignoredDependencies.get())
    }

    @Test
    fun `plugin should throw exception when allowedLicenses contains blank strings`() {
        val allowedList = listOf(" ")
        val extension = project.extensions.getByType(LicenseInspectorExtension::class.java)
        extension.allowedLicenses.set(allowedList)

        val exception = assertThrows<Exception> {
            project.getAllTasks(true)
        }

        assertTrue {
            exception.cause is IllegalArgumentException
        }
    }

    @Test
    fun `plugin should throw exception when ignoredDependencies contains blank strings`() {
        val ignoredList = listOf(" ")
        val extension = project.extensions.getByType(LicenseInspectorExtension::class.java)
        extension.ignoredDependencies.set(ignoredList)

        val exception = assertThrows<Exception> {
            project.getAllTasks(true)
        }

        assertTrue {
            exception.cause is IllegalArgumentException
        }
    }

    private companion object {
        const val ANDROID_APP_PLUGIN_ID = "com.android.application"
        const val NAMESPACE = "mr.liks.licenseinspector"
        const val TASK_NAME = "inspectLicensesFor"
        const val PLUGIN_EXT_NAME = "licenseInspector"
        val allowedLicensesDefaultList = listOf("mit", "apache", "bsd", "public domain")
    }
}