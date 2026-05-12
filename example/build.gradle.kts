plugins {
  application
  id("iridium.common-convention")
}

application {
  mainClass.set("de.yyuh.iridium.example.Main")
}

dependencies() {
  implementation(project(":iridium"))
  implementation(project(":shared"))
  implementation(project(":iridium-auth"))
}
