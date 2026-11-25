import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProps = Properties().apply {
    val propsFile = rootProject.file("local.properties")
    if (propsFile.isFile) {
        propsFile.inputStream().use { load(it) }
    }
}
val rawGoogleClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").orNull
    ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
    ?: ""
val googleWebClientId = rawGoogleClientId.trim()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.pocketkit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.pocketkit"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }

        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
