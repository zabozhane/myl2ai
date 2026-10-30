plugins {
    kotlin("jvm") version "1.9.25"
}

val acisSources = providers.gradleProperty("acisSources").get()
val autobotsSources = providers.gradleProperty("autobotsSources").get()

repositories { mavenCentral() }

dependencies {
    compileOnly(fileTree("$acisSources/aCis_gameserver/lib") { include("*.jar") })
    // jackson-module-kotlin pulls an old kotlin-reflect; it must match the stdlib version.
    implementation(kotlin("reflect"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.5")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.10.5")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.reflections:reflections:0.9.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
java { targetCompatibility = JavaVersion.VERSION_11 }

sourceSets.main {
    kotlin.srcDir("$autobotsSources/src/main/kotlin")
    // Dashboard HTML templates (views/*.htv) and the bot name wordlist are loaded from the classpath.
    resources.srcDir("$autobotsSources/src/main/resources")
    // aCis Java sources are only read for symbol resolution; javac never compiles them here.
    kotlin.srcDir("$acisSources/aCis_gameserver/java")
    java.setSrcDirs(emptyList<String>())
}

// The aCis ant build picks up every jar in aCis_gameserver/lib, so the module and its runtime deps go there.
tasks.register<Copy>("installIntoAcisLib") {
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into("$acisSources/aCis_gameserver/lib")
}
