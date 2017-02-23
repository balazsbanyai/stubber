import javassist.ClassPath
import javassist.ClassPool
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public class LoadClassesTask extends DefaultTask {

    @Input
    Configuration configuration

    Set<ClassPath> classPaths = []

    @OutputFile
    File classPathDefs = project.file("${project.buildDir}/${name}.txt")

    @TaskAction
    void putJarsOnClasspath() {
        new FileWriter(classPathDefs, false).close()
        configuration.each { dependencyFile ->
            def path = dependencyFile.getAbsolutePath()
            ClassPath cp = ClassPool.default.insertClassPath(path)
            classPaths += cp
            classPathDefs.append("$path\n")
        }
    }
}
