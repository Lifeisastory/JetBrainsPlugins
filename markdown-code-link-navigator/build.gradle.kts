import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.12.0"
}

group = "com.melot"
version = "0.1.8"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        androidStudio("2025.3.2.4")
        bundledPlugin("org.intellij.plugins.markdown")
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Markdown Code Link Navigator"
        description = """
            Lightweight Android Studio plugin for navigating from Markdown links to local code files,
            including support for line and optional column markers.
        """.trimIndent()

        ideaVersion {
            sinceBuild = "253"
        }
    }
}
