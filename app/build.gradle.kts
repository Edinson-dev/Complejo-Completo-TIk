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
        versionCode = 22
        versionName = "2.2"
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    
    // Asegura que el build termine antes de copiar
    dependsOn("assembleDebug")
    
    doLast {
        val vName = android.defaultConfig.versionName ?: "1.0"
        val vCode = android.defaultConfig.versionCode ?: 1
        
        // 1. Localizar el APK (ruta absoluta robusta)
        val apkOrigin = File(layout.buildDirectory.get().asFile, "outputs/apk/debug/app-debug.apk")
        val apkDest = File(project.rootDir, "tikdownloader.apk")
        
        if (apkOrigin.exists()) {
            // Usamos copy de Gradle para garantizar que el archivo se cierre correctamente
            copy {
                from(apkOrigin)
                into(project.rootDir)
                rename { "tikdownloader.apk" }
            }
            println("✅ APK ACTUALIZADO EXITOSAMENTE: ${apkDest.absolutePath} (${apkDest.length() / 1024} KB)")
        } else {
            throw GradleException("❌ ERROR: No se encontró el APK. Verifica que la compilación terminó en: ${apkOrigin.absolutePath}")
        }
        
        // 2. Actualizar version.json con URL completa al APK
        val jsonFile = File(project.rootDir, "version.json")
        jsonFile.writeText("""
{
  "latestVersionName": "$vName",
  "latestVersionCode": $vCode,
  "updateUrl": "https://tik-downloader-five.vercel.app/tikdownloader.apk"
}
        """.trimIndent())
        println("✅ version.json sincronizado a v$vName")
        
        // 3. Actualizar index.html (label de versión)
        val htmlFile = File(project.rootDir, "index.html")
        if (htmlFile.exists()) {
            var htmlContent = htmlFile.readText()
            val versionRegex = Regex("""(<span id="heroVersionLabel">)(.*?)(</span>)""")
            if (versionRegex.containsMatchIn(htmlContent)) {
                htmlContent = versionRegex.replace(htmlContent) { it.groupValues[1] + "V$vName" + it.groupValues[3] }
                htmlFile.writeText(htmlContent)
                println("✅ index.html actualizado a V$vName")
            }
        }
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