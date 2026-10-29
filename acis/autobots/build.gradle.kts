plugins {
    kotlin("jvm") version "1.9.25"
}

val acisSources = providers.gradleProperty("acisSources").get()
val autobotsSources = providers.gradleProperty("autobotsSources").get()

repositories { mavenCentral() }

dependencies {
    compileOnly(fileTree("$acisSources/aCis_gameserver/lib") { include("*.jar") })
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
    // aCis Java sources are only read for symbol resolution; javac never compiles them here.
    kotlin.srcDir("$acisSources/aCis_gameserver/java")
    java.setSrcDirs(emptyList<String>())
}
