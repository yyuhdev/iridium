plugins {
    `java-library`
    id("com.gradleup.shadow")
}

group = "de.yyuh"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("http://mvn.int.revived.club/releases") {
    isAllowInsecureProtocol = true
  }
}

dependencies() {
  implementation("org.jspecify:jspecify:1.0.0")

  api("de.yyuh.libs:celery:1.0.4-SNAPSHOT")
  api("de.yyuh.libs.celery-platform:celery-mongodb:1.0.4-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
