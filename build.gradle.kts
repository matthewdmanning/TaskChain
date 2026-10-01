plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

project(":cyberpunkandroid") {
    plugins.withId("com.android.library") {
        dependencies.add("implementation", "androidx.compose.material:material-icons-extended")
    }
    afterEvaluate {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 36
        }
    }
}
