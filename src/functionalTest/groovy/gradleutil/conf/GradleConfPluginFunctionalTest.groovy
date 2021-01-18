package gradleutil.conf

import groovy.json.JsonSlurper
import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for the plugin.
 */
class GradleConfPluginFunctionalTest extends Specification {

    def "no config or schema"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        new File(projectDir, "settings.gradle") << ""
        new File(projectDir, "build.gradle") << """
            plugins {
                id('gradleutil.conf')
            }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-S")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        result.output.contains("does not exist")
    }

    def "settings"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def configFile = new File(projectDir, "config.conf") << "someOption=someValue"
        new File(projectDir, "build.gradle") << ""
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('gradleutil.settings-conf')
            }
            config{
                conf = file('${configFile.path}')
            }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-Sq")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        def json = new JsonSlurper().parseText(result.output)
        json.someOption == 'someValue'
    }

}
