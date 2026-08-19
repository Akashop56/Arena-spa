// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// Auto-accept Android SDK licenses if running on CI / GitHub Actions
val sdkDirs = listOfNotNull(
    System.getenv("ANDROID_HOME"),
    System.getenv("ANDROID_SDK_ROOT"),
    "/usr/local/lib/android/sdk",
    "${System.getProperty("user.home")}/Android/Sdk"
).map { java.io.File(it) }.filter { it.exists() }

for (sdkDir in sdkDirs) {
    try {
        val licensesDir = java.io.File(sdkDir, "licenses")
        licensesDir.mkdirs()
        
        val sdkLicense = java.io.File(licensesDir, "android-sdk-license")
        if (!sdkLicense.exists() || !sdkLicense.readText().contains("24333f8a63718c303d008f2230294d7973b07370")) {
            sdkLicense.writeText(
                """
                8933017284140101bC1573917812d4d8b37f4951
                24333f8a63718c303d008f2230294d7973b07370
                84831b9409646a7d8f22a21b22498e470f98b7f7
                d56f518747945140a32263b12a1086a9d9fc9268
                """.trimIndent()
            )
            println("Accepted Android SDK licenses in ${sdkLicense.absolutePath}")
        }
    } catch (e: Exception) {
        println("Could not write SDK licenses to $sdkDir: ${e.message}")
    }
}

gradle.buildFinished {
    val failure = this.failure
    if (failure != null) {
        val rootCause = (failure.cause?.message ?: failure.message ?: "Unknown build failure")
            .replace("\n", " ").replace("\r", " ")
        val stack = failure.stackTraceToString().replace("\n", " ").replace("\r", " ").take(1000)
        println("::error file=build.gradle.kts,line=1::BUILD_FAILURE: $rootCause --- STACK: $stack")
    }
}
