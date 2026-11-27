import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// dev 서버 주소·토큰은 커밋하지 않는 secrets.properties에서 dev 변형에만 넣는다(D-043, D-044)
val secrets =
    Properties().apply {
        val file = rootProject.file("secrets.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun String.quoted() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// versionCode는 커밋 수(RN app.config.ts와 같은 방식). git이 없으면 1
val commitCount: Int =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
        .toIntOrNull() ?: 1

android {
    namespace = "io.github.seyoon.futureetp"
    // Compose 1.13(BOM 2026.11.01)이 37.1 이상을 요구한다(AOS-003)
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "io.github.seyoon.futureetp"
        minSdk = 29
        targetSdk = 37
        versionCode = commitCount
        versionName = "1.0.0"
    }

    flavorDimensions += "variant"
    productFlavors {
        create("dev") {
            dimension = "variant"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "futureETP Dev")
            buildConfigField(
                "String",
                "API_BASE_URL",
                secrets.getProperty("DEV_API_BASE_URL", "http://10.0.2.2:3000").quoted(),
            )
            buildConfigField("String", "DEV_API_TOKEN", secrets.getProperty("DEV_API_TOKEN", "").quoted())
        }
        create("prod") {
            dimension = "variant"
            resValue("string", "app_name", "futureETP")
            buildConfigField("String", "API_BASE_URL", "https://futureetp-api.duckdns.org".quoted())
            buildConfigField("String", "DEV_API_TOKEN", "\"\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        // deprecated·경고는 오류로 본다(D-128 대응). 버전 알림은 카탈로그에서 직접 올리므로 끈다(AOS-003)
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // 앱 안 언어(per-app language, Android 12 이하 저장 포함, AOS-015)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.tink)
    // 기기 UI 설정(D-073, AOS-014)
    implementation(libs.androidx.datastore.preferences)
    // datastore-tink가 기대는 tink-android 1.20.0을 카탈로그 버전으로 올린다(키셋 생성에 직접 쓴다, AOS-007)
    implementation(libs.tink.android)
    // B-b QR 읽기(AOS-013)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.konsist)
    testImplementation(libs.json.schema.validator)
}
