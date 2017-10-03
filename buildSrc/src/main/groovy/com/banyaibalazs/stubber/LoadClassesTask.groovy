package com.banyaibalazs.stubber

import javassist.ClassPath
import javassist.ClassPool
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class LoadClassesTask extends DefaultTask {

    @InputFiles
    FileCollection classesJars

    Set<ClassPath> classPaths = []

    @OutputFile
    File classPathDefs = project.file("${project.buildDir}/${name}.txt")

    @TaskAction
    void putJarsOnClasspath() {
        new FileWriter(classPathDefs, false).close()
        classesJars.files.each { classesJar ->
            def path = classesJar.getAbsolutePath()
            ClassPath cp = ClassPool.default.insertClassPath(path)
            classPaths += cp
            classPathDefs.append("$path\n")
        }
    }
}
