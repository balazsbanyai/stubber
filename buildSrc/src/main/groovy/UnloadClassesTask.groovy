import javassist.ClassPool
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

public class UnloadClassesTask extends DefaultTask {

    @InputFiles
    FileCollection classPathDefs

    LoadClassesTask loadTask

    @TaskAction
    void removeJarsFromClasspath() {
        loadTask.classPaths.each { classPath ->
            ClassPool.default.removeClassPath(classPath);
        }
    }
}
