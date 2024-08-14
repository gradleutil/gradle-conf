package net.gradleutil.config.task

import com.networknt.schema.JsonSchema
import groovy.transform.CompileStatic
import net.gradleutil.conf.json.schema.SchemaUtil
import net.gradleutil.conf.transform.groovy.GroovyConfig
import net.gradleutil.conf.transform.Transformer
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
    @InputFile
    final RegularFileProperty schemaFile

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
        schemaFile = project.objects.fileProperty()
        packageName = project.objects.property(String).convention('config')
        rootClassName = project.objects.property(String).convention('Config')
        schemaName = project.objects.property(String)
        dslFileName = project.objects.property(String).convention(project.provider { rootClassName.get() + '.groovy' })
        outputDirectory = project.objects.directoryProperty()
        description = "Generate DSL from JSON schema"
    }

    @TaskAction
    void schemaToGroovy() {
        if (!outputDirectory.isPresent()) {
            throw new GradleException("outputDirectory is required")
        }
        if (!schemaFile.isPresent() || !schemaFile.getAsFile().get().exists()) {
            throw new GradleException("schemaFile is required ${schemaFile.getOrNull() ?: ''}")
        }
        def dest = outputDirectory.getAsFile().get()
        dest.mkdirs()
        def dslFile = project.file(dest.path + '/' + packageName.get().replace('.', '/') + '/' + dslFileName.get())
        logger.lifecycle("Generating groovy from file://${schemaFile.get()} to file://${dslFile}")
        dslFile.parentFile.mkdirs()
        Transformer.transform(schemaFile.getAsFile().get().text, packageName.get(), rootClassName.get(), schemaFile.get().asFile.parentFile.absolutePath,  dslFile)
    }


    void generateGradleExtension() {
        def schema = getJsonSchema(schemaFile.getAsFile().get())
        println GroovyConfig.toGroovyDsl(schema, 'schema', packageName.get())
    }


    @Option(option = 'schemaFile', description = 'Set the filename of the JSON schema input file')
    void setSchemaFile(final String path) {
        schemaFile.set(project.file(path))
    }


    private JsonSchema getJsonSchema(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Schema file ${file} does not exist")
        }
        try {
            return SchemaUtil.getSchema(file.text, file.parentFile.absolutePath)
        } catch (Exception e) {
            logger.error("Error loading schema: ${e.message}")
        }
    }

    String toGroovyDsl(String packageName, String schemaName = null) {
        def schema = getJsonSchema(schemaFile.getAsFile().get())
        GroovyConfig.toGroovyDsl(schema, schemaName, packageName)
    }

}