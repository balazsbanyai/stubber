package com.banyaibalazs.stubber

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

        Copy extractorTask = project.tasks.create(name: "unpack${name}", type: Copy)

        def directDependency = stubConfiguration.first();
        extractorTask.from project.zipTree(directDependency)
        extractorTask.into project.file("${project.buildDir}/unpacked/$name")
        // TODO handle AAR!
//    if (file.name.endsWith(".aar")) {
//        classesJar = project.zipTree(file)
//                .matching { include "classes.jar" }
//                .first()
//    }

        FileTree tree = extractorTask.outputs.files.asFileTree.matching {
            include '**/*.class'
        }

        CreateStubTask createStubTask = project.tasks.create(
                name: CreateStubTask.resolveName(name),
                type: CreateStubTask,
                {
                    configuration = stubConfiguration
                    sourcePath = extractorTask.outputs.files
                    from tree
                    into "${project.buildDir}/stubbed/$name"
                })

        Task load = project.tasks.create(name: "loadClassesForStubbing${name}", type: LoadClassesTask, {
            configuration = stubConfiguration
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
}