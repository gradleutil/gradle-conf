package net.gradleutil.config

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

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
        def jsonText = result.output.drop(result.output.indexOf('{')).replaceAll('(.*}).*', '$1')
        def json = new JsonSlurper().parseText(jsonText)

        then:
        json == [:]
    }

    def "settings confConfig"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def subprojects = ['sub1', 'sub2','sub3']
        def configFile = new File(projectDir, "someConfig.conf") << "someOption=someValue"
        new File(projectDir, "config.schema.json").text = new File('src/testPlugin/resources/json/veggies.schema.json').text
        String SETTINGS =  """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig{
                conf = file('${configFile.name}')
                schemaFile.set file('config.schema.json')
                generateBean.set true
                silent.set false
                outputDirectory.set new File(rootDir,'src/dsl')
                rootClassName.set 'Booklist'
            }
            include('sub1','sub2','sub2','sub3')
        """.stripIndent()
        String BUILD = """
        """.stripIndent()
        new File(projectDir, "build.gradle").text = ''
        new File(projectDir, "settings.gradle").text = SETTINGS
        subprojects.each {name ->
            def subprojectDir = new File(projectDir, name).tap { it.mkdir() }
            new File(subprojectDir, "build.gradle").text = BUILD
        }

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

    def "settings settingsPlugin"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def subprojects = ['sub1', 'sub2','sub3']
        def configFile = new File(projectDir, "someConfig.conf") << "someOption=someValue"
        String SETTINGS =  """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            include('sub1','sub2','sub2','sub3')
        """.stripIndent()
        String BUILD = """
            confConfig.conf = file('../${configFile.name}')
            generate{
                bob {
                    sourceConf = file('../${configFile.name}')
                    schemaName = 'config'
                    targetSchemaFile = file('config.schema')
                }
            }
        """.stripIndent()
        new File(projectDir, "settings.gradle").text = SETTINGS
        new File(projectDir, "build.gradle").text = ''
        subprojects.each {name ->
            def subprojectDir = new File(projectDir, name).tap { it.mkdir() }
            new File(subprojectDir, "build.gradle").text = BUILD
        }

        when:
        def runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("generateConfigSchema", "generateModelBob","printConfig","-Si")
        runner.withProjectDir(projectDir)
        def result = runner.build()

        then:
        def json = new JsonSlurper().parseText(result.output.drop(result.output.lastIndexOf('{\n')))
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

    def "githubrepo"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def configFile = new File(projectDir, "config.conf") << '''
        github {
          owner = "bob"
          repository = "somerepo"
          registry {
            user = someuser
            apiKey = somepassword
            url = "https://maven.pkg.github.com/"${github.owner}"/"${github.repository}
          }
        }
        '''.stripIndent()
        new File(projectDir, "build.gradle") << ""
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig {
                conf.set file('${configFile.name}')
            }
            gradle.settingsEvaluated{
                githubPackagesRepo {
                    defaultRepo {
                        registryUrl = 'https://gradle.org'
                    }
                    anotherRepo {
                    println config
                        registryUrl = config.github.registry.url
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
        def result = runner.build().output

        then:
        ['adding github packages repo ', 'adding publication for repo '].each {
            result.contains(it + 'GithubPackageRepo(name:anotherRepo, registryUrl:https://maven.pkg.github.com/bob/somerepo, publish:true)')
            result.contains(it + 'GithubPackageRepo(name:defaultRepo, registryUrl:https://gradle.org, publish:true)')
            result.contains(it + 'GithubPackageRepo(name:defaultRepo, registryUrl:https://gradle.org, publish:true)')
            result.contains(it + 'GithubPackageRepo(name:anotherRepo, registryUrl:https://maven.pkg.github.com/bob/somerepo, publish:true)')
        }
    }

    def "gitLabRepo should exist"() {
        given:
        def projectDir = new File("build/functionalTest")
        projectDir.deleteDir()
        projectDir.mkdirs()
        def configFile = new File(projectDir, "config.conf") << '''
        gitlab {
          repository = "somerepo"
          registry {
            authToken = somepassword
            url = "https://maven.pkg.github.com/"
          }
        }
        '''.stripIndent()
        new File(projectDir, "build.gradle") << ""
        new File(projectDir, "settings.gradle") << """
            plugins {
                id('net.gradleutil.gradle-conf')
            }
            confConfig {
                conf.set file('${configFile.name}')
            }
            gradle.settingsEvaluated{
                gitLabPackagesRepo {
                    defaultRepo {
                        authToken = "weehoo"
                        registryUrl = 'https://gradle.org'
                    }
                    anotherRepo {
                    println config
                        authToken = "weehoo"
                        registryUrl = config.gitlab.registry.url
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
        def result = runner.build().output

        then:
        ['adding github packages repo ', 'adding publication for repo '].each {
            result.contains(it + 'GitLapPackageRepo(name:anotherRepo, registryUrl:https://maven.pkg.github.com/bob/somerepo, publish:true)')
            result.contains(it + 'GitLapPackageRepo(name:defaultRepo, registryUrl:https://gradle.org, publish:true)')
            result.contains(it + 'GitLapPackageRepo(name:defaultRepo, registryUrl:https://gradle.org, publish:true)')
            result.contains(it + 'GitLapPackageRepo(name:anotherRepo, registryUrl:https://maven.pkg.github.com/bob/somerepo, publish:true)')
        }
    }

    def "no_githubrepo"() {
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
        def result = runner.build().output

        then:
        !result.contains('adding github packages repo')
    }

}
