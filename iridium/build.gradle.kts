plugins {
  id("java")
  id("iridium.common-convention")
}

dependencies() {
  implementation(project(":shared"))
  api("com.fasterxml.jackson.core:jackson-databind:2.21.3")
  api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.3")
}
