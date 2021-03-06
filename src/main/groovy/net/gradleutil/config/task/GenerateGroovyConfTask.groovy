package net.gradleutil.config.task

import net.gradleutil.conf.Gen
import net.gradleutil.conf.generator.GroovyConfig
import net.gradleutil.conf.generator.JsonConfig
import groovy.transform.CompileStatic
import org.everit.json.schema.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

import javax.inject.Inject

@CompileStatic
class GenerateGroovyConfTask extends DefaultTask {

    @Optional
    @Input
    final RegularFileProperty schemaFileProperty

    @Input
    final Property<String> packageName

    @Optional
    @Input
    final Property<String> rootClassName

    @Input
    final Property<String> schemaName

    @Optional
    @Input
    final Property<String> dslFileName

    @OutputDirectory
    final DirectoryProperty outputDirectory


    @Inject
    GenerateGroovyConfTask() {
        schemaFileProperty = project.objects.fileProperty()
        packageName = project.objects.property(String).convention('config')
        rootClassName = project.objects.property(String).convention('Config')
        schemaName = project.objects.property(String)
        dslFileName = project.objects.property(String).convention(project.provider{rootClassName.get() + 'DSL.groovy' })
        outputDirectory = project.objects.directoryProperty()
        description = "Generate DSL from JSON schema"
    }

    @TaskAction
    void dsl() {
        if (!outputDirectory.isPresent()) {
            throw new GradleException("DSL output file path is required")
        }
        if (!schemaFileProperty.isPresent() || !schemaFileProperty.getAsFile().get().exists()) {
            throw new GradleException("JSON schema file path is required")
        }
        def dest = outputDirectory.getAsFile().get()
        dest.mkdirs()
        def dslFile = project.file(dest.path + '/' + packageName.get().replace('.', '/') + '/' + dslFileName.get())
        logger.lifecycle("Generating DSL from file://${schemaFileProperty.get()} to file://${dslFile}")
        dslFile.parentFile.mkdirs()
        new Gen(packageName: packageName.get()).groovyClassFromSchema(schemaFileProperty.getAsFile().get().text, rootClassName.get(), dslFile)
//        def configuration = new ConfigEater(schema: schemaFile)
//        dslFile.text = configuration.toGroovyDsl(packageName, schemaName)
    }


    void generateGradleExtension() {
        def schema = getJsonSchema(schemaFileProperty.getAsFile().get())
        println GroovyConfig.toGroovyDsl(schema, 'schema', packageName.get())
    }


    @Option(option = 'schemaFile', description = 'Set the filename of the JSON schema input file')
    void setSchemaFile(final String path) {
        schemaFileProperty.set(project.file(path))
    }


    private Schema getJsonSchema(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Schema file ${file} does not exist")
        }
        try {
            return JsonConfig.getSchema(file.text)
        } catch (Exception e) {
            logger.error("Error loading schema: ${e.message}")
        }
    }

    String toGroovyDsl(String packageName, String schemaName = null) {
        def schema = getJsonSchema(schemaFileProperty.getAsFile().get())
        GroovyConfig.toGroovyDsl(schema, schemaName, packageName)
    }

}