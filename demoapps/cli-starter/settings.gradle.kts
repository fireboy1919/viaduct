// tag::dependency-resolution[30] How to setup settings.gradle.kts on a new viaduct application

val viaductVersion: String by settings

pluginManagement {
    if (gradle.parent != null) {
        includeBuild("../../gradle-plugins")
    } else {
        repositories {
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            version("viaduct", viaductVersion)
        }
    }
}
