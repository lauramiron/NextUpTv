plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "io.github.lauramiron.nextuptv"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.lauramiron.nextuptv"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "RAPIDAPI_KEY", "\"YOUR_API_KEY_HERE\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = file("schemas").path
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.junit.ktx)
    kapt("androidx.room:room-compiler:2.8.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    implementation(libs.kotlin.stdlib)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-moshi:3.0.0")
    implementation(libs.androidx.work.runtime.ktx)
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")

    // Room testing
    testImplementation("androidx.room:room-testing:<version>")
    testImplementation("androidx.room:room-ktx:<version>")

    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<version>")

//    // Retrofit/OkHttp
//    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
//    testImplementation("com.squareup.retrofit2:retrofit:<version>")
//    testImplementation("com.squareup.retrofit2:converter-moshi:<version>")
//    testImplementation("com.squareup.moshi:moshi-kotlin:<version>")

    // WorkManager test utils (only if your sync is driven by WorkManager)
    androidTestImplementation(libs.work.testing)
    testImplementation(libs.work.testing)
    testImplementation(kotlin("test"))
    testImplementation(libs.truth)
}