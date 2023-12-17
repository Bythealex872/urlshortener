import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    id("urlshortener.spring-library-conventions")
    kotlin("plugin.spring")
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
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-integration")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("commons-validator:commons-validator:${Version.COMMONS_VALIDATOR}")
    implementation("com.google.guava:guava:${Version.GUAVA}")
    implementation("com.blueconic:browscap-java:1.4.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${Version.MOCKITO}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    

}

tasks.withType<Test> {
    addTestOutputListener{ _, outputEvent ->
        logger.lifecycle(outputEvent.message)
    }
}
