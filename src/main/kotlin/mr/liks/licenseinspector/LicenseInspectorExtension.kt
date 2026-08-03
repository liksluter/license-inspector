package mr.liks.licenseinspector

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/** Расширение для настройки плагина [LicenseInspectorPlugin] */
abstract class LicenseInspectorExtension {
    abstract val allowedLicenses: ListProperty<String>
    abstract val ignoredDependencies: ListProperty<String>
}