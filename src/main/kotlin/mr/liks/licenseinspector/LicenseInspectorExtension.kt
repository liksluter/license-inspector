package mr.liks.licenseinspector

import org.gradle.api.provider.ListProperty

/** Расширение для настройки плагина [LicenseInspectorPlugin] */
abstract class LicenseInspectorExtension {
    abstract val allowedLicenses: ListProperty<String>
    abstract val ignoredDependencies: ListProperty<String>
}