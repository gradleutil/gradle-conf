package net.gradleutil.config

import groovy.json.JsonSlurper
import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for the plugin.
 */
class ConfPluginFunctionalTest extends Specification {

    def "no config or schema"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()
        def jsonText = result.output.drop(result.output.indexOf('{')).replaceAll('(.*}).*','$1')
        def json = new JsonSlurper().parseText(jsonText)

        then:
        json == [:]
    }

    def "settings"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
            def configFile = new File(projectDir, "someConfig.conf") << "someOption=someValue"
        new File(projectDir, "build.gradle") << ""
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig{
                conf = file('${configFile.name}')
            }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        def json = new JsonSlurper().parseText(result.output.drop(result.output.indexOf('{\n')))
        json.someOption == 'someValue'
    }

    def "someOption"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def configFile = new File(projectDir, "someConfig.conf") << "someOption=someValue"
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
        plugins{ 
            id('java') 
            id('net.gradleutil.gradle-conf')
        }
        confConfig {
            conf.set file('${configFile.name}')
        }
        println config
//        assert wee =='goo'
        if(config.someOption !='someValue'){
        println('noooooo')
        } else {
        println 'yaasssssssss'
        }
        assert config.someOption == 'someValue'
        tasks.withType(AbstractPublishToMaven) { publishTask ->
            doLast{
                if(publishTask instanceof PublishToMavenRepository){
                    logger.lifecycle("Published \${project.group}.\${project.name}:\${version} to \${publishTask.repository.url}")
                } else {
                    logger.lifecycle("Published \${project.group}.\${project.name}:\${version}")
                }
            }
        }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        def json = new JsonSlurper().parseText(result.output.drop(result.output.indexOf('{\n')))
        json.someOption == 'someValue'

    }

}
