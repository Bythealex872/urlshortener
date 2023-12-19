import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    id("urlshortener.kotlin-common-conventions")
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
}

kover {
    excludeJavaCode()
}

koverReport {
    defaults {
        log {
            groupBy = GroupingEntityType.CLASS
        }
    }
}

dependencies {
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.opencsv:opencsv:5.5")
    implementation("com.google.apis:google-api-services-safebrowsing:v4-rev44-1.23.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.40.1") //Deprecated check after it works
    implementation("com.google.api-client:google-api-client:1.31.5")
    implementation("com.blueconic:browscap-java:1.4.1")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")

    testImplementation("org.mockito.kotlin:mockito-kotlin:${Version.MOCKITO}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

}

tasks.withType<Test> {
    addTestOutputListener{ _, outputEvent ->
        logger.lifecycle(outputEvent.message)
    }
}
