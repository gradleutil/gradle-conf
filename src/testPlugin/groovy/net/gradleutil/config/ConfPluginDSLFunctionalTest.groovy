package net.gradleutil.config

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * A simple functional test for the plugin.
 */
class ConfPluginDSLFunctionalTest extends Specification {

    def "generate config bean"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        println new File('./testPlugin/resources/json/veggies.schema.json').absolutePath
        new File(projectDir, "config.schema.json").text = new File('src/testPlugin/resources/json/veggies.schema.json').text
        new File(projectDir, "configConf.conf").text = new File('src/testPlugin/resources/json/veggies.json').text
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.conf')
            }
            confConfig{
                conf = file('configConf.conf')
                generateBean.set true
                schemaFile.set file('config.schema.json')
            }
        """
        new File(projectDir, "build.gradle") << """
        //plugins { id 'config.config' }
        //import config.Config
        afterEvaluate{
            println config.class.name
            println config.fruits.dump()
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
        json.fruits.first() == 'apple'
    }


}
