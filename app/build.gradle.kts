plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.tiborlaszlo.keywave"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.tiborlaszlo.keywave"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"

    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)

  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.material3)
  implementation(libs.datastore.preferences)
  implementation(libs.coroutines.android)
  implementation(libs.lifecycle.runtime.compose)
}
