package gradleutil.conf

import gradleutil.conf.task.GenerateTask
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class GradleConfPluginTest extends Specification {

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
        project.plugins.apply("gradle-conf")

        then:
        project.tasks.findByName("printConfig") != null
        def genConfigTask = project.tasks.findByName("generateConfig") as GenerateTask
        genConfigTask != null
        genConfigTask.setSchemaFile("${project.buildDir}/schema.json")
        genConfigTask.setDslFile(project.file("${project.buildDir}/dsl.groovy"))
        genConfigTask.generateGradleExtension()
    }

}
