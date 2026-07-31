plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "com.devscope"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)

    // Integrations: the host app provides the real dependency; compileOnly keeps
    // DevScope from forcing OkHttp/Room/Navigation on apps that don't use them.
    compileOnly(libs.okhttp)
    compileOnly(libs.androidx.room.runtime)
    compileOnly(libs.androidx.navigation.compose)
    api(libs.timber)

    testImplementation(libs.junit)
}

// JitPack publishing: consumers add `com.github.<user>:DevScope:<tag>`.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.afik"
            artifactId = "devscope"
            version = "1.0.0"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
