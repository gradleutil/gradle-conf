package net.gradleutil.config

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

/**
 * A simple functional test for the plugin.*/
class ConfPluginDSLFunctionalTest extends Specification {

    def "generate config bean"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def jsonSchema = ConfPluginDSLFunctionalTest.classLoader.getResource('json/booklist.schema.json').text
        def conf = ConfPluginDSLFunctionalTest.classLoader.getResource('json/booklist.json').text
        new File(projectDir, "config.schema.json").text = jsonSchema
        new File(projectDir, "configConf.conf").text = conf
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig{
                conf.set file('configConf.conf')
                generateBean.set true
                silent.set false
                outputDirectory.set new File(rootDir,'src/dsl')
                rootClassName.set 'Booklist'
                schemaFile.set file('config.schema.json')
            }
        """.stripIndent()
        new File(projectDir, "build.gradle") << """
            //plugins { id 'config.config' }
            //import config.Config
            afterEvaluate{
                println config.books
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("printConfig", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        def json = new JsonSlurper().parseText(result.output.drop(result.output.indexOf('{\n')))
        json.books.first().title == 'Unlocking Android'
    }


    def "generate settings model bean"() {
        given:
        def projectDir = new File("build/functionalTest")
        def jsonSchema = new File('src/testPlugin/resources/json/')
        projectDir.deleteDir()
        projectDir.mkdirs()
        new File(projectDir, "configConf.conf").text = new File('src/testPlugin/resources/json/veggies.json').text
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig{
                conf = file('configConf.conf')
            }
            generate{
                bob {
                    sourceConf = file('configConf.conf')
                }
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

    def "generate model bean"() {
        given:
        def projectDir = new File("build/functionalTest")
        def jsonSchema = new File('src/testPlugin/resources/json/')
        projectDir.deleteDir()
        projectDir.mkdirs()
        new File(projectDir, "configConf.conf").text = new File('src/testPlugin/resources/json/veggies.json').text
        //        new File(projectDir, "config.schema").text = new File('src/testPlugin/resources/json/veggies.schema.json').text
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig{
                conf = file('configConf.conf')
            }
        """
        new File(projectDir, "build.gradle") << """
        //plugins { id 'config.config' }
        //import config.Config
        generate{
            bob {
                sourceConf = file('configConf.conf')
                schemaName = 'config'
                targetSchemaFile = file('config.schema')
            }
        }
        afterEvaluate{
            println config.class.name
            println config.fruits.dump()
        }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("generateModelBob", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        new File(projectDir, 'build/conf-generated').exists()
    }

    def "generate mhf jar"() {
        given:
        def projectDir = new File("build/functionalTest")
        def jsonSchema = new File('src/testPlugin/resources/json/')
        projectDir.deleteDir()
        projectDir.mkdirs()
        new File(projectDir, "configConf.conf").text = new File('src/testPlugin/resources/json/veggies.json').text
        new File(projectDir, "config.schema").text = new File('src/testPlugin/resources/json/veggies.schema.json').text
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
        """
        new File(projectDir, "build.gradle") << """
        //plugins { id 'config.config' }
        //import config.Config
            mhfModel{
                myModel {
                   mhf = file('configConf.conf')
                   packageName = 'org.mo'
                }
            }
        """

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("assemble", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        new File(projectDir, 'build/mhf-content').exists()
    }

    def "generate jsonSchema"() {
        given:
        def projectDir = new File("build/functionalTest")
        def jsonSchemaDir = new File(projectDir,'json')
        [projectDir, jsonSchemaDir].each { it.deleteDir(); it.mkdirs() }
        new File(jsonSchemaDir, "veggies.schema.json").text = new File('src/testPlugin/resources/json/veggies.schema.json').text
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
        """.stripIndent()
        new File(projectDir, "build.gradle") << """
            jsonSchemaModel {
                "net.gradleutil.gradleconf" {
                   schemaDir = file('json/')
                }
            }
        """.stripIndent()

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("assemble", "-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        new File(projectDir, 'build/jsm-content').exists()
    }


}
