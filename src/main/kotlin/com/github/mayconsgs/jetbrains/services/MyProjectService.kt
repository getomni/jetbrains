package com.github.mayconsgs.jetbrains.services

import com.github.mayconsgs.jetbrains.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {
  init {
    println(MyBundle.message("projectService", project.name))
  }
}
