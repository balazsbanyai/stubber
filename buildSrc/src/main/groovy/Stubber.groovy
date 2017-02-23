import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

public class Stubber implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.ext.stub = { dep ->

            def dependency = project.dependencies.create(dep);
            def stubConfiguration = project.configurations.detachedConfiguration(dependency)

            def name = [dependency.group, dependency.name, dependency.version]
                    .collect({ it -> it.tokenize('.').collect({it2 -> it2.capitalize()}).join() })
                    .collect({ it -> it.capitalize() })
                    .join()

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
                    name: "createStubFor${name}",
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
        }
    }
}