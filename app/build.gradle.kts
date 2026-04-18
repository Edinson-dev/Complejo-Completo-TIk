plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.tikdownloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tikdownloader"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.2"
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
}

tasks.register("updateWeb") {
    group = "publishing"
    description = "Prepara el APK, JSON e Index para la Landing Page"
    dependsOn("assembleDebug")
    
    doLast {
        val vName = android.defaultConfig.versionName ?: "1.0"
        val vCode = android.defaultConfig.versionCode ?: 1
        
        // 1. Copiar y renombrar APK a la raíz del proyecto
        val apkOrigin = File(layout.buildDirectory.get().asFile, "outputs/apk/debug/app-debug.apk")
        val apkDest = File(project.rootDir, "tikdownloader.apk")
        
        if (apkOrigin.exists()) {
            apkOrigin.copyTo(apkDest, true)
            println("✅ APK LISTO: ${apkDest.name}")
        } else {
            throw GradleException("❌ ERROR: No se encontró el APK en ${apkOrigin.absolutePath}")
        }
        
        // 2. Actualizar version.json
        val jsonFile = File(project.rootDir, "version.json")
        val jsonContent = """
        {
          "latestVersionName": "$vName",
          "latestVersionCode": $vCode,
          "updateUrl": "https://tik-downloader-five.vercel.app/"
        }
        """.trimIndent()
        jsonFile.writeText(jsonContent)
        println("✅ version.json actualizado a v$vName")
        
        // 3. Actualizar index.html (el label de versión)
        val htmlFile = File(project.rootDir, "index.html")
        if (htmlFile.exists()) {
            var htmlContent = htmlFile.readText()
            // Reemplaza el contenido de heroVersionLabel
            htmlContent = htmlContent.replace(Regex("""<span id="heroVersionLabel">V.*?</span>"""), "<span id=\"heroVersionLabel\">V$vName</span>")
            htmlFile.writeText(htmlContent)
            println("✅ index.html actualizado con la versión V$vName")
        }
        
        println("🚀 ¡Todo listo! Ahora sube tikdownloader.apk, version.json e index.html a Vercel.")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    
    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}