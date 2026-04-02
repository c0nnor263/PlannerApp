import com.github.benmanes.gradle.versions.VersionsPlugin
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import java.util.Locale
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.benMames) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.daggerHilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
}


allprojects {
    plugins.withId("org.jetbrains.kotlin.plugin.compose") {
        configure<ComposeCompilerGradlePluginExtension> {
            enableStrongSkippingMode = true

            reportsDestination = layout.buildDirectory.dir("compose_compiler")
            stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
        }
    }

    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply<VersionsPlugin>()

    // Ktlint configuration
    configure<KtlintExtension> {
        android = true // to use the Android Studio KtLint plugin style
        ignoreFailures = true

        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
        filter {
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }
    afterEvaluate {
        tasks.findByName("preBuild")?.dependsOn("ktlintFormat")
        tasks.findByName("checkKotlinGradlePluginConfigurationErrors")?.dependsOn("ktlintFormat")
    }

    // Never mind about this, it's just a helper function to check newest versions of dependencies
    fun isNonStable(version: String): Boolean {
        val stableKeyword =
            listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            val version = candidate.version
            val stableKeyword =
                listOf("RELEASE", "FINAL", "GA").any {
                    version.uppercase(Locale.getDefault())
                        .contains(it)
                }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(version)
            isStable.not()
        }
    }
}