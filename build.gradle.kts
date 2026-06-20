plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
}

group = "one.nfolio"
version = "1.0.0-SNAPSHOT"

application {
  mainClass = "io.ktor.server.cio.EngineMain"

  applicationDefaultJvmArgs = listOf("-Dconfig.file=application-dev.yaml")
}

kotlin {
  jvmToolchain(25)
}
dependencies {
  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(ktorLibs.server.cio)
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.csrf)
  implementation(ktorLibs.server.websockets)
  implementation(libs.logback.classic)
  implementation(libs.ktor.client)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.content.negotiation)
  implementation("io.ktor:ktor-server-auth:3.5.0")
  implementation("io.ktor:ktor-server-sessions:3.5.0")

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}
