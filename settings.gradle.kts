pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RideGuard"

include(
    ":app",
    ":core:model",
    ":core:calculator",
    ":core:settings",
    ":detection:accessibility",
    ":detection:ocr",
    ":platforms:uber",
    ":platforms:cabify",
    ":platforms:didi",
    ":overlay",
)
