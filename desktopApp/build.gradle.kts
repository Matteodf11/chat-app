import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.matteo.chatapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Chimera"
            packageVersion = "1.1.2"
            description = "Chimera - Mensajes, amigos y comunidades"
            vendor = "Chimera"
            modules("java.instrument", "java.management", "java.net.http", "java.prefs", "jdk.crypto.ec", "jdk.unsupported")
            windows {
                iconFile.set(project.file("src/main/resources/chimera.ico"))
                perUserInstall = true
                dirChooser = true
                menuGroup = "Chimera"
                shortcut = true
                upgradeUuid = "5d2a42cd-b2b4-4cd9-8c39-46a8dde93937"
            }
        }
    }
}
