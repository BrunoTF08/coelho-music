buildscript {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val patched = when {
                requested.group == "com.google.guava" && requested.name == "guava" -> "33.7.2-jre"
                requested.group == "io.netty" -> "4.1.137.Final"
                requested.group == "org.bouncycastle" -> "1.85"
                requested.group == "org.apache.commons" && requested.name == "commons-compress" -> "1.28.0"
                requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> "0.9.6"
                requested.group == "org.jdom" && requested.name == "jdom2" -> "2.0.6.1"
                else -> null
            }
            if (patched != null) { useVersion(patched); because("Security patches identified by the OSV dependency review; see SECURITY_REVIEW.md") }
        }
    }
}

plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
}
