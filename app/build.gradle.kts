import java.util.Properties
plugins {
    id("deft.android.application")
    id("deft.android.compose")
    id("deft.android.hilt")
}
val deftVersion = DeftVersion.parse(providers.gradleProperty("deft.version").get())
val signing =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

fun signingValue(
    property: String,
    variable: String,
): String? = providers.environmentVariable(variable).orNull ?: signing.getProperty(property)
android {
    defaultConfig {
        applicationId = "com.kmab.deft"
        versionName = deftVersion.name
        versionCode = deftVersion.code
    }
    signingConfigs {
        create("release") {
            storeFile = signingValue("storeFile", "DEFT_KEYSTORE_FILE")?.let(::file)
            storePassword = signingValue("storePassword", "DEFT_KEYSTORE_PASSWORD")
            keyAlias = signingValue("keyAlias", "DEFT_KEY_ALIAS")
            keyPassword = signingValue("keyPassword", "DEFT_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
