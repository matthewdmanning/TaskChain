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

rootProject.name = "TaskChain"
include(":app")
include(":cyberpunkandroid")
project(":cyberpunkandroid").projectDir = file("third_party/cyberpunkAndroid/cyberpunkandroid")
