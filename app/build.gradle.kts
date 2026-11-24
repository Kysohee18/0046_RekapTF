import com.android.build.api.variant.BuildConfigField
import java.io.Serializable
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val apiBaseUrl = providers.gradleProperty("rekapApiBaseUrl").map { it.trim().trimEnd('/') }
val debugLocalApiBaseUrl = providers
    .fileContents(rootProject.layout.projectDirectory.file("rekap-debug.local.properties"))
    .asText
    .map { text ->
        val properties = Properties().apply { load(text.reader()) }
        properties.getProperty("rekapDebugApiBaseUrl", "").trim().trimEnd('/')
    }
    .orElse("")
    .map { it.ifBlank { "http://10.0.2.2:8080" } }
val localOcr = providers.gradleProperty("rekapLocalOcr").map { it.toBooleanStrict() }

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\""

android {
    namespace = "id.ac.rekaptf"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.ac.rekaptf"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "REKAP_LOCAL_OCR", localOcr.orElse(true).get().toString())
        }
        release {
            buildConfigField("String", "API_BASE_URL", apiBaseUrl.orElse("").get().asBuildConfigString())
            buildConfigField("boolean", "REKAP_LOCAL_OCR", localOcr.orElse(false).get().toString())
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val endpoint = apiBaseUrl.orElse(debugLocalApiBaseUrl)
        requireNotNull(variant.buildConfigFields).put("API_BASE_URL", endpoint.map { value ->
            BuildConfigField<Serializable>("String", value.asBuildConfigString(), null)
        })
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.mlkit.text)
}
