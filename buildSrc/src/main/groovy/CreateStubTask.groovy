import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles

import java.nio.file.Paths

public class CreateStubTask extends Copy {

    private static final String EMPTY_BODY = "{}"
    private static final String THROWING_BODY = "{throw new RuntimeException(\"stub!\");}"
    private static final Set EMPTY_METHODS = [ 'finalize' ]

    @InputFiles
    FileCollection sourcePath

    CreateStubTask() {
        eachFile { FileCopyDetails fileCopyDetails ->
            performOnEachFile(fileCopyDetails)
        }
    }

    public void performOnEachFile(FileCopyDetails fileCopyDetails) {
        def path = sourcePath.asPath+"/"+fileCopyDetails.path
        fileCopyDetails.exclude()

        InputStream fis = new FileInputStream(path);
        byte[] classBytes = new byte[fis.available()];
        fis.read(classBytes);

        ClassPool pool = ClassPool.getDefault();
        def className = fileCopyDetails.path.replace(".class", "").replace("/", ".")

        byte[] modifiedBytes = new byte[0];
        try {

            CtClass cc = pool.get(className);

            if (cc.isInterface()) {
                logger.info "skipping ${className} (interface)"
                return
            }

            cc.defrost()

            logger.info "stubbing ${cc.name}"

            cc.declaredConstructors.each { ctor ->
                logger.debug "\t${ctor.name}"
                ctor.setBody(EMPTY_BODY)
            }

            cc.declaredMethods.each { method ->
                logger.debug "\t${method.name}"

                if (EMPTY_METHODS.contains(method.name)) {
                    method.setBody(EMPTY_BODY)
                } else {
                    method.setBody(THROWING_BODY)
                }
            }


            modifiedBytes = cc.toBytecode();
        } catch (Exception e) {
            throw new RuntimeException(e)
        }

        if (modifiedBytes.length > 0) {
            def dest = "$destinationDir/${fileCopyDetails.path}"
            logger.debug "writing $dest"
            try {
                File target = project.file(dest)
                def parent = Paths.get(target.path).parent
                parent.toFile().mkdirs()

                FileOutputStream fos = new FileOutputStream(dest);
                fos.write(modifiedBytes);
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }

    }
}