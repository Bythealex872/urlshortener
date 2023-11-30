plugins {
    `java-library`
    id("urlshortener.kotlin-common-conventions")
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}
