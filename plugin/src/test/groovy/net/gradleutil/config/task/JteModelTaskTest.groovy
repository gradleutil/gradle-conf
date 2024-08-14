package net.gradleutil.config.task

import net.gradleutil.conf.transform.Transformer
import net.gradleutil.conf.transform.groovy.EPackageRenderer
import net.gradleutil.conf.util.Inflector
import spock.lang.Specification

class InflectorTest extends Specification {


    def "test inflector inflects"() {
        setup:
        def version
        def inflector = Inflector.instance

        when:
        version = inflector.pluralize('sheet')
        then:
        version == 'sheets'

        when:
        version = inflector.singularize('sheets')
        then:
        version == 'sheet'

    }

    void testUpdate() {
        File jsonSchema
        def name = jsonSchema.name.replace('.schema', '').replace('.json', '')
        def packagePrefix = jsonSchema.path.replace(schemaDir.get().asFile.path, '')
                .replace(File.separator + jsonSchema.name, '')
                .replace(File.separator, '.').toLowerCase()

        def modelSourceDir = new File(getOutputDir().asFile.get(), packagePrefix.replace('.', File.separator)
                + File.separator + name.toLowerCase()).tap { mkdirs() }

        def fullPackageName = "${packageName.get()}${packagePrefix ?: ''}.${name.toLowerCase()}"

        def options = Transformer.transformOptions()
                .jsonSchema(jsonSchema.text).packageName(fullPackageName)
                .rootClassName(name).outputFile(modelSourceDir)

        if (jteDir.isPresent()) {
            options.jteDirectory(jteDir.get().asFile)
        }

        EPackageRenderer.schemaToEPackageRender(options)
    }
}
