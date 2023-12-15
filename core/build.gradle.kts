plugins {
    id("urlshortener.kotlin-common-conventions")
}
dependencies {
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.opencsv:opencsv:5.5")
    implementation("com.google.apis:google-api-services-safebrowsing:v4-rev44-1.23.0")
    implementation("com.google.http-client:google-http-client-jackson2:1.40.1") //Deprecated check after it works
    implementation("com.google.api-client:google-api-client:1.31.5")
    implementation("com.blueconic:browscap-java:1.4.1")

}