import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.elsayyad.control"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.elsayyad.control"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

val downloadGestureModel by tasks.registering {
    val modelFile = layout.projectDirectory.file("src/main/assets/gesture_recognizer.task")
    outputs.file(modelFile)
    doLast {
        val target = modelFile.asFile
        if (!target.exists()) {
            target.parentFile.mkdirs()
            val connection = URL("https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task").openConnection()
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.getInputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        check(target.length() > 1_000_000L) { "MediaPipe gesture model download is incomplete" }
    }
}
tasks.named("preBuild").configure { dependsOn(downloadGestureModel) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    debugImplementation("androidx.compose.ui:ui-tooling")
}