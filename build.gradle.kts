import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

plugins {
  id("java")
  id("org.jetbrains.intellij.platform") version "2.19.0"
  id("org.jetbrains.changelog") version "2.5.0"
}

fun properties(key: String) = providers.gradleProperty(key)

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    create(properties("platformType"), properties("platformVersion"))
  }
}

intellijPlatform {
  buildSearchableOptions = false

  pluginConfiguration {
    name = properties("pluginName")
    version = properties("pluginVersion")

    description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
      val start = "<!-- Plugin description -->"
      val end = "<!-- Plugin description end -->"

      with(it.lines()) {
        if (!containsAll(listOf(start, end))) {
          throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
        }
        subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
      }
    }

    changeNotes = provider {
      with(changelog) {
        renderItem(
          (getOrNull(properties("pluginVersion").get()) ?: getUnreleased())
            .withHeader(false)
            .withEmptySections(false),
          Changelog.OutputType.HTML,
        )
      }
    }

    ideaVersion {
      sinceBuild = properties("pluginSinceBuild")
      untilBuild = provider { null }
    }
  }

  publishing {
    token = providers.environmentVariable("PUBLISH_TOKEN")
    channels = properties("pluginVersion").map {
      listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
    }
  }

  pluginVerification {
    ides {
      recommended()
    }
  }
}

changelog {
  version = properties("pluginVersion")
  groups.empty()
  repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
  wrapper {
    gradleVersion = properties("gradleVersion").get()
  }

  publishPlugin {
    dependsOn(patchChangelog)
  }
}
