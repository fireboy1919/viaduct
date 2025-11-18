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

include(":modules:filmography")
include(":common")
include(":modules:universe")
