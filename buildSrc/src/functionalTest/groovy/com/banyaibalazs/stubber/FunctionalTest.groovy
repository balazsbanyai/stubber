package com.banyaibalazs.stubber

import org.gradle.api.artifacts.Dependency
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class FunctionalTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "plugin applied, dependency stub requested, stub creator task added"() {

        String guavaDependencyName = 'com.google.guava:guava:19.0'

        given:
            buildFile << """
                plugins {
                    id 'com.banyaibalazs.stubber'
                }

                apply plugin: 'java'

                repositories {
                    jcenter()
                }

                dependencies {
                    compile stub('$guavaDependencyName')
                }
            """

        when:
            def result = GradleRunner.create()
                    .withProjectDir(testProjectDir.root)
                    .withArguments("compileJava")
                    .withPluginClasspath()
                    .build()

        then:
            Dependency dependency = new ProjectBuilder()
                    .build()
                    .dependencies
                    .create(guavaDependencyName)

            def taskName = CreateStubTask.resolveName(BaseNameResolver.resolveName(dependency))

            result.task(":$taskName") != null
            result.task(":$taskName").outcome == SUCCESS
    }
}