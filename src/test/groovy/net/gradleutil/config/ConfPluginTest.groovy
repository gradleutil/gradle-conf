package net.gradleutil.config

import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateGroovyConfTask
import net.gradleutil.config.transform.TransformUtil
import org.gradle.api.logging.LogLevel
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ConfPluginTest extends Specification {

    def "plugin registers task"() {
        given:
        def project = ProjectBuilder.builder().build()
        def buildDir = project.buildDir
        buildDir.mkdirs()
        new File(buildDir, "schema.json") << '''{
  "$schema": "http://json-schema.org/draft-06/schema#",
  "$ref": "#/definitions/Dataobject",
  "definitions": {
    "Dataobject": {
      "type": "object",
      "additionalProperties": false,
      "properties": {
        "$schema": {
          "type": "string",
          "title": "Schema",
          "description": "Pointer to the schema against which this document should be validated."
        },
        "version": {
          "type": "string"
        }
      }
    }
  }
}'''

        when:
        project.plugins.apply("net.gradleutil.conf")

        then:
        project.tasks.findByName("printConfig") != null
        def genConfigTask = project.tasks.findByName("generateConfig") as GenerateGroovyConfTask
        genConfigTask != null
        genConfigTask.setSchemaFile("${project.buildDir}/schema.json")
        genConfigTask.generateGradleExtension()
    }


    def "extracts closure from source and runs against project"() {
        given:
        def project = ProjectBuilder.builder().build()
        def buildDir = project.buildDir
        buildDir.mkdirs()
        project.logging.setLevelInternal(LogLevel.DEBUG)
        project.buildFile.text = """
        plugins{ id('java') }
        confConfig {
         conf.set file('wee')
        } 
        assert config.wee=='goo'
        tasks.withType(AbstractPublishToMaven) { publishTask ->
            doLast{
                if(publishTask instanceof PublishToMavenRepository){
                    logger.lifecycle("Published \${project.group}.\${project.name}:\${version} to \${publishTask.repository.url}")
                } else {
                    logger.lifecycle("Published \${project.group}.\${project.name}:\${version}")
                }
            }
        }
        """.stripIndent()

        when:
        project.plugins.apply("net.gradleutil.conf")

        then:
        def confConfig = project.extensions.findByType(ConfConfig)
        confConfig != null
        def objectClosures = [confConfig: confConfig]
        TransformUtil.runClosuresOnObjectsFromSource(project.buildFile.text, project, objectClosures)
        confConfig.load()

        project.tasks.findByName("printConfig") != null
        project.extensions.findByType(ConfConfig).conf.getAsFile().get().path.endsWith('wee')
        def genConfigTask = project.tasks.findByName("generateConfig") as GenerateGroovyConfTask
        genConfigTask != null
    }

    def "plugin sets project build dir"() {
        given:
        def project = ProjectBuilder.builder().build()
        def buildDir = project.buildDir
        buildDir.mkdirs()
        project.logging.setLevelInternal(LogLevel.DEBUG)

        when:
        project.plugins.apply("net.gradleutil.conf")

        then:
        project.tasks.findByName("printConfig") != null

        def confConfig = project.extensions.findByType(ConfConfig)
        confConfig != null
        confConfig.outputDirectory.getAsFile().get().path == project.buildDir.path + '/conf'
        def genConfigTask = project.tasks.findByName("generateConfig") as GenerateGroovyConfTask
        genConfigTask != null
    }

    def "plugin sets custom config"() {
        given:
        def project = ProjectBuilder.builder().build()
        def buildDir = project.buildDir
        def configFile = new File(project.projectDir, "someConfig.conf") << "someOption=someValue"
        buildDir.mkdirs()

        when:
        project.plugins.apply("net.gradleutil.conf")

        then:
        project.tasks.findByName("printConfig") != null
        def confConfig = project.extensions.findByType(ConfConfig)
        confConfig != null
        confConfig.conf.set configFile
        project.config.someOption == 'someValue'
    }

    def "plugin generates DSL config"() {
        given:
        def project = ProjectBuilder.builder().build()
        def buildDir = project.buildDir
        def configFile = new File(project.projectDir, "someConfig.conf") << "someOption=someValue"
        buildDir.mkdirs()

        when:
        project.plugins.apply("net.gradleutil.conf")

        then:
        project.tasks.findByName("printConfig") != null
        def confConfig = project.extensions.findByType(ConfConfig)
        confConfig != null
        confConfig.conf.set configFile
        project.config.someOption == 'someValue'
    }

}
