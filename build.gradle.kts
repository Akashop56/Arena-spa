// Top-level build file. Plugin versions are declared in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

gradle.buildFinished {
    val failure = this.failure
    if (failure != null) {
        val rootCause = failure.cause?.message ?: failure.message ?: "Unknown build failure"
        val stack = failure.stackTraceToString().take(1500)
        println("BUILD FAILURE REASON: $rootCause")
        println("BUILD FAILURE STACKTRACE:\n$stack")
        
        val ghToken = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
        val ghRepo = System.getenv("GITHUB_REPOSITORY") ?: "Akashop56/Arena-spa"
        val ghSha = System.getenv("GITHUB_SHA")
        if (!ghToken.isNullOrEmpty() && !ghSha.isNullOrEmpty()) {
            try {
                val url = java.net.URL("https://api.github.com/repos/$ghRepo/statuses/$ghSha")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $ghToken")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "Gradle-Diagnostic")
                conn.doOutput = true
                val desc = (rootCause + " | " + stack).replace("\n", " ").replace("\r", " ").replace("\"", "'").take(130)
                val json = """{"state": "failure", "description": "$desc", "context": "gradle-diagnostic"}"""
                conn.outputStream.write(json.toByteArray(Charsets.UTF_8))
                println("Posted diagnostic status: ${conn.responseCode}")
            } catch (e: Exception) {
                println("Failed to post diagnostic status: ${e.message}")
            }
        }
    }
}
