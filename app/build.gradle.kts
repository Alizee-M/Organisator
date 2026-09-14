import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val versionProps = Properties().apply {
    val f = rootProject.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val appVersionCode = (versionProps.getProperty("versionCode") ?: "1").trim().toInt()
val appVersionName = (versionProps.getProperty("versionName") ?: "1.0.0").trim()

android {
    namespace = "com.organisator.print3d"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.organisator.print3d"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("sideload") {
            // Clé de signature de sideload, versionnée volontairement : elle n'ouvre
            // aucun accès et garantit que les mises à jour s'installent par-dessus la
            // version précédente. Surchargeable par des secrets CI (voir .github/workflows).
            //
            // Une variable absente et une variable vide doivent être traitées pareil :
            // GitHub Actions transmet un secret non défini comme une chaîne vide, ce qui
            // produirait un alias et un mot de passe vides plutôt que les valeurs par défaut.
            fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

            storeFile = env("ORGANISATOR_KEYSTORE")?.let { file(it) }
                ?: rootProject.file("keystore/organisator-sideload.jks")
            storePassword = env("ORGANISATOR_KEYSTORE_PASSWORD") ?: "organisator"
            keyAlias = env("ORGANISATOR_KEY_ALIAS") ?: "organisator"
            keyPassword = env("ORGANISATOR_KEY_PASSWORD") ?: "organisator"
        }
    }

    buildTypes {
        release {
            // Pas de minification : l'APK est distribué en sideload, la taille importe
            // peu et on évite tout risque de régression R8 sur Room/Compose.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("sideload")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    debugImplementation(libs.androidx.ui.tooling)
}
