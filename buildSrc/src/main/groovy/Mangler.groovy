import javassist.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*

import java.util.jar.JarEntry
import java.util.jar.JarFile

public class Mangler extends DefaultTask {


    private static final boolean LOG_CLASSES = true
    private static final boolean LOG_METHODS = false

    private static final String EMPTY_BODY = "{}"
    private static final String THROWING_BODY = "{throw new RuntimeException(\"stub!\");}"
    private static final Set EMPTY_METHODS = [ 'finalize' ]

    @Input
    FileCollection src


    @TaskAction
    void mangle() {

        src.each { file ->
            mangle(file)
        }

    }

    void mangle(File file) {

        def classesJar = file;

        if (file.name.endsWith(".aar")) {
            classesJar = project.zipTree(file)
                    .matching { include "classes.jar" }
                    .first()
        }

        JarFile jarFile = new JarFile(classesJar);

        Enumeration allEntries = jarFile.entries();
        def errors = 0;
        while (allEntries.hasMoreElements()) {
            JarEntry entry = (JarEntry) allEntries.nextElement();
            try {
                mangleClass(classesJar, jarFile, entry)
            } catch (Exception e) {
                errors++
            }
        }

        if (errors > 0) {
            println "There were $errors errors during stubbing."
        }

    }

    void mangleClass(File originalJarFile, JarFile jarFile, JarEntry entry) {

        if (entry.name.endsWith(".class")) {
            println "opening ${entry.name}"
        } else {
            println "skipping ${entry.name} (not a class file)"
            return
        }

        def className = entry.name.replace(".class", "").replace("/", ".")

        InputStream fis = jarFile.getInputStream(entry);
        byte[] classBytes = new byte[fis.available()];
        fis.read(classBytes);

        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(new ClassClassPath(this.getClass()));
        ClassPath cp1 = null;
        //ClassPath cp2 = null;

        // add the JAR file to the classpath
        try {
            cp1 = cp.insertClassPath(originalJarFile.getAbsolutePath());
        } catch (NotFoundException e1) {
            e1.printStackTrace();
        }
        // add the class file we are going to modify to the classpath
        //cp2 = cp.appendClassPath(new ByteArrayClassPath(className, classBytes));

        byte[] modifiedBytes = new byte[0];
        try {
            CtClass cc = cp.get(className);

            if (cc.isInterface()) {
                println "skipping ${entry.name} (interface)"
                return
            }
            // skip instrumentation if the class is frozen and therefore
            // can't be modified
            cc.defrost()
            if (!cc.isFrozen()) {

                if (LOG_CLASSES) {
                    println("stubbing " + cc.getName())
                }

                cc.getDeclaredConstructors().each { ctor ->
                    if (LOG_METHODS) {
                        println "\t${ctor.name}"
                    }

                    cc.removeConstructor(ctor)

                    CtConstructor c = new CtConstructor(ctor.parameterTypes, cc)
                    c.setBody(EMPTY_BODY)
                    cc.addConstructor(c);
                }

                cc.getDeclaredMethods().each { method ->

                    if (LOG_METHODS) {
                        println "\t${method.name}"
                    }

                    cc.removeMethod(method)

                    CtMethod m = new CtMethod(method.returnType, method.name, method.parameterTypes, cc)
                    if (EMPTY_METHODS.contains(m.name)) {
                        m.setBody(EMPTY_BODY)
                    } else {
                        m.setBody(THROWING_BODY)
                    }

                    cc.addMethod(m)
                }

            }
            modifiedBytes = cc.toBytecode();
        } catch (Exception e) {
            throw new RuntimeException(e)
        } finally {
            cp.removeClassPath(cp1);
        }

        if (modifiedBytes.length > 0) {
            try {
                FileOutputStream fos = new FileOutputStream("SessionFeatures.class");
                fos.write(modifiedBytes);
            } catch (Exception e) {
                throw new RuntimeException(e)
            }
        }
    }

}