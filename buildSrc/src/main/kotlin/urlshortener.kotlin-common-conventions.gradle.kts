import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Detekt>("detekt") {
    exclude("**/generated-sources/**")
}

tasks.named("check") {
    dependsOn("detekt")
}
