import org.gradle.kotlin.dsl.testImplementation

plugins {
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "com.github.liksluter"
version = "1.0.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/liksluter/license-inspector")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    compileOnly(libs.android.build.gradle)
    compileOnly(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.android.build.gradle)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.android.build.gradle)
}

gradlePlugin {
    plugins.register("licenseInspector") {
        id = "mr.liks.license-inspector"
        implementationClass = "mr.liks.licenseinspector.LicenseInspectorPlugin"
    }
}

ktlint {
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
}

val functionalTest by sourceSets.creating {
    compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
    runtimeClasspath += output + compileClasspath
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs functional tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(functionalTestTask)
}

tasks.named("functionalTest") {
    dependsOn("publishToMavenLocal")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}