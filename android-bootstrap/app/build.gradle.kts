plugins { id("com.android.application") }

android {
    namespace = "dev.thefoolish.aidao"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "dev.thefoolish.aidao"
        minSdk = 26
        targetSdk = 37
        versionCode = 17
        versionName = "0.17.0-alpha"
    }
}
