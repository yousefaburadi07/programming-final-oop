plugins {
    id("java")
//    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.mongodb:mongodb-driver-sync:5.3.0")
    // This tells the MongoDB driver to stay quiet
    implementation("org.slf4j:slf4j-nop:2.0.9")
}

//application {
//    // This must match the package and class name in your Main.java
//    mainClass.set("org.example.Main")
//}

tasks.test {
    useJUnitPlatform()
}
