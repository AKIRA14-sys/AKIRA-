pluginManagement {
    repositories {
        google()
        maven { url = java.net.URI("https://cache-redirector.jetbrains.com/maven-central") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = java.net.URI("https://cache-redirector.jetbrains.com/maven-central") }
    }
}

rootProject.name = "ANKIGPT"
include(":app")
