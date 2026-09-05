import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(ktorLibs.plugins.ktor)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ktlint)
  id("application")
}

group = "one.nfolio"
version = "1.0.0-SNAPSHOT"

application {
  mainClass = "io.ktor.server.cio.EngineMain"

  applicationDefaultJvmArgs = listOf("-Dconfig.file=application-dev.yaml")
}

ktlint {
  version.set("1.8.0")

  reporters {
    reporter(ReporterType.PLAIN)
    reporter(ReporterType.HTML)
  }
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
  implementation(ktorLibs.server.auth)
  implementation(ktorLibs.server.sessions)
  implementation(ktorLibs.server.callLogging)
  implementation(ktorLibs.client.core)
  implementation(ktorLibs.client.cio)
  implementation(ktorLibs.client.contentNegotiation)
  implementation(ktorLibs.client.websockets)
  implementation(libs.logback.classic)
  implementation(libs.loki4j)

  testImplementation(kotlin("test"))
  testImplementation(ktorLibs.server.testHost)
}
