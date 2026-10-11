plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Su un Mac con Scrivania e Documenti in iCloud, iCloud duplica i file generati
// ("splash_empty 2.xml") e la build fallisce. I prodotti della build vanno fuori,
// come la DerivedData dell'app iOS (vedi build.sh).
val home: String = System.getProperty("user.home")
if (listOf("$home/Desktop/", "$home/Documents/").any { rootDir.absolutePath.startsWith(it) }) {
    allprojects {
        layout.buildDirectory.set(File("$home/.gradle-builds/Receipty/${project.name}"))
    }
}
