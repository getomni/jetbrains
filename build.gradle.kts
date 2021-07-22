import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.5.20"
  id("org.jetbrains.intellij") version "1.1.4"
  id("org.jetbrains.changelog") version "1.1.2"
  id("io.gitlab.arturbosch.detekt") version "1.16.0"
  id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()
  jcenter()
}
dependencies {
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.16.0")
}

intellij {
  pluginName = properties("pluginName")
  version = properties("platformVersion")
  type = properties("platformType")
  downloadSources = properties("platformDownloadSources").toBoolean()
  updateSinceUntilBuild = true

  setPlugins(*properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty).toTypedArray())
}

changelog {
  version = properties("pluginVersion")
  groups = emptyList()
}

detekt {
  config = files("./detekt-config.yml")
  buildUponDefaultConfig = true

  reports {
    html.enabled = false
    xml.enabled = false
    txt.enabled = false
  }
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }
  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  withType<Detekt> {
    jvmTarget = "1.8"
  }

  patchPluginXml {
    version(properties("pluginVersion"))
    sinceBuild(properties("pluginSinceBuild"))
    untilBuild(properties("pluginUntilBuild"))

    pluginDescription(
      File("./README.md").readText().lines().run {
        val start = "<!-- Plugin description -->"
        val end = "<!-- Plugin description end -->"

        if (!containsAll(listOf(start, end))) {
          throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
        }
        subList(indexOf(start) + 1, indexOf(end))
      }.joinToString("\n").run { markdownToHTML(this) }
    )

    changeNotes(changelog.getUnreleased().toHTML())
  }

  runPluginVerifier {
    ideVersions(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token(System.getenv("PUBLISH_TOKEN"))
    channels(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
  }
}
