plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("org.jetbrains.kotlin.plugin.serialization")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.lulema.app"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.lulema.app"
  minSdk = 26
  targetSdk = 35
  versionCode = 2
  versionName = "0.2.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 val signingPath = providers.environmentVariable("KEYSTORE_FILE").orNull
 if (signingPath != null) signingConfigs.create("release") {
  storeFile = file(signingPath)
  storePassword = System.getenv("KEYSTORE_PASSWORD")
  keyAlias = System.getenv("KEY_ALIAS")
  keyPassword = System.getenv("KEY_PASSWORD")
 }
 buildTypes { release {
  isMinifyEnabled = true
  proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  if (signingPath != null) signingConfig = signingConfigs.getByName("release")
 } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true; buildConfig = true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
 testOptions { unitTests.isReturnDefaultValues = true }
 sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
 implementation("androidx.navigation:navigation-compose:2.9.0")
 implementation("androidx.room:room-runtime:2.7.1")
 implementation("androidx.room:room-ktx:2.7.1")
 ksp("androidx.room:room-compiler:2.7.1")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 implementation("androidx.core:core-splashscreen:1.0.1")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.room:room-testing:2.7.1")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
