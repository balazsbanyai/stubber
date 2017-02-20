import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import javassist.ClassPool

public class Stubber implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.ext.stub = { dep ->
            def name = dep.tokenize('.|:')
                    .collect({ it -> it.capitalize() })
                    .join()

            def dependency = project.dependencies.create(dep);
            def stubConfiguration = project.configurations.detachedConfiguration(dependency)

            Copy extractorTask = project.tasks.create(name: "unpack${name}", type: Copy)
            extractorTask.from project.zipTree(stubConfiguration.singleFile)
            extractorTask.into project.file("${project.buildDir}/unpacked/$name")
            // TODO handle AAR!
//    if (file.name.endsWith(".aar")) {
//        classesJar = project.zipTree(file)
//                .matching { include "classes.jar" }
//                .first()
//    }


            CreateStubTask createStubTask = project.tasks.create(name: "createStubFor${name}", type: CreateStubTask)

            createStubTask.sourcePath = extractorTask.outputs.files
            FileTree tree = extractorTask.outputs.files.asFileTree.matching {
                include '**/*.class'
            }

            createStubTask.from tree
            createStubTask.into "${project.buildDir}/stubbed/$name"

            def classPath = null

            createStubTask.doFirst {
                classPath = ClassPool.default.insertClassPath(stubConfiguration.singleFile.getAbsolutePath());
            }
            createStubTask.doLast {
                ClassPool.default.removeClassPath(classPath);
            }

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