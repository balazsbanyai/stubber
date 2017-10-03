package com.banyaibalazs.stubber

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip


public class Stubber implements Plugin<Project> {

    Set<StubberModel> cache = new HashSet()

    class StubberModel {
        String name
        FileCollection output
    }

    @Override
    void apply(Project project) {

        project.ext.stub = { dep ->

            def dependency = project.dependencies.create(dep);
            def name = BaseNameResolver.resolveName(dependency)
            def model = cache
                    .stream()
                    .filter({m -> m.name.equals(name) })
                    .findFirst()
                    .orElseGet({
                        def newStubber = createStubber(dependency, project)
                        cache.add(newStubber)
                        return newStubber
                    })

            return model.output
        }

    }

    def createStubber(Dependency dependency, Project project) {
        def name = BaseNameResolver.resolveName(dependency)

        def stubConfiguration = project.configurations.detachedConfiguration(dependency)

        def directDependency = stubConfiguration.first();

        Copy extractorTask = project.tasks.create(name: "unpack${name}", type: Copy, {
            into project.file("${project.buildDir}/unpacked/$name")
        })

        def jarFiles = []
        if (directDependency.name.endsWith(".aar")) {
            def classesJar = project.zipTree(directDependency)
                    .matching { include "classes.jar" }
                    .first()

            def libs = project.zipTree(directDependency)
                    .matching { include "libs/**" }

            libs.files.each {
                def libJar = project.zipTree(it)
                extractorTask.from libJar
                jarFiles.add it
            }

            def jarFile = project.zipTree(classesJar)
            extractorTask.from jarFile
            jarFiles.add classesJar
        } else if (directDependency.name.endsWith(".jar")) {
            def jarFile = project.zipTree(directDependency)
            extractorTask.from jarFile
            jarFiles.add directDependency
        } else {
            throw new GradleException("Failed to stub unknown extension $directDependency.name")
        }

        jarFiles += injectAdditionalJarsForCompilation(project)

        def jarFileCollection = project.files(jarFiles)

        FileTree tree = extractorTask.outputs.files.asFileTree.matching {
            include '**/*.class'
        }

        CreateStubTask createStubTask = project.tasks.create(
                name: CreateStubTask.resolveName(name),
                type: CreateStubTask,
                {
                    sourcePath = extractorTask.outputs.files
                    from tree
                    into "${project.buildDir}/stubbed/$name"
                })

        Task load = project.tasks.create(name: "loadClassesForStubbing${name}", type: LoadClassesTask, {
            classesJars = jarFileCollection
        })
        createStubTask.dependsOn load

        Task unload = project.tasks.create(name: "unloadClassesForStubbing${name}", type: UnloadClassesTask, {
            classPathDefs = load.outputs.files
            loadTask = load
            onlyIf { createStubTask.dependsOnTaskDidWork() }
        })
        createStubTask.finalizedBy unload


        Zip jarTask = project.tasks.create(name: "jarStubFor${name}", type: Zip)
        jarTask.from createStubTask.outputs.files
        jarTask.archiveName "stubFor${name}.jar"
        jarTask.destinationDir project.file("${project.buildDir}/repacked")

        FileCollection collection = project.files(jarTask.outputs.files[0])
        collection.builtBy jarTask
        collection

        StubberModel model = new StubberModel(name: name, output: collection)
        return model
    }

    static def injectAdditionalJarsForCompilation(def project) {
        def additionalJars = []
        additionalJars.add injectAndroidBootClasspath(project)
        return additionalJars
    }

    private static injectAndroidBootClasspath(def project) {
        def additionalJars = []
        project.plugins.all { plugin ->
            if (plugin.class.name.matches('com\\.android\\.build\\.gradle\\.[aA-zZ]*Plugin')) {
                project.android.getBootClasspath().each { runtimeJar ->
                    additionalJars.add runtimeJar
                }
            }
        }
        return additionalJars;
    }
}