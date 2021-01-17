package gradleutil.conf.task

import gradleutil.conf.Gen
import gradleutil.conf.generator.GroovyConfig
import gradleutil.conf.generator.JsonConfig
import groovy.transform.CompileStatic
import org.everit.json.schema.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

import javax.inject.Inject

@CompileStatic
class GenerateTask extends DefaultTask {

    @Optional
    @Input
    final Property<File> schemaFileProperty

    @Input
    final Property<String> packageName

    @Optional
    @Input
    final Property<String> rootClassName

    @Input
    final Property<String> schemaName

    @Optional
    @OutputFile
    final Property<File> dslFile

    @OutputDirectory
    final Property<File> outputDirectory


    @Inject
    GenerateTask() {
        schemaFileProperty = project.objects.property(File)
        packageName = project.objects.property(String)
        rootClassName = project.objects.property(String).convention('Config')
        schemaName = project.objects.property(String)
        dslFile = project.objects.property(File)
        outputDirectory = project.objects.property(File)
        description = "Generate DSL from JSON schema"
    }

    @TaskAction
    void dsl() {
        outputDirectory.get().mkdirs()
        if (!dslFile.isPresent()) {
            throw new GradleException("DSL output file path is required")
        }
        if (!schemaFileProperty.isPresent() || !schemaFileProperty.get().exists()) {
            throw new GradleException("JSON schema file path is required")
        }
        def dslFile = project.file(outputDirectory.get().path + '/' + packageName.get().replace('.', '/') + '/' + dslFile.get().name)
        logger.lifecycle("Generating DSL from file://${schemaFileProperty.get()} to file://${dslFile}")
        dslFile.parentFile.mkdirs()
        new Gen(packageName: packageName.get()).groovyClassFromSchema(schemaFileProperty.get().text, rootClassName.get(), dslFile)
//        def configuration = new ConfigEater(schema: schemaFile)
//        dslFile.text = configuration.toGroovyDsl(packageName, schemaName)
    }


    void generateGradleExtension() {
        boolean useBackticks = false
        boolean generateJava7Code = false
/*
        GenOpts genOpts = new GenOpts( packageName, 'className',true,false,false,false,false,false,false )
        GradleExtension generator = new GradleExtension( genOpts )
        generator.generate( ModelBuilder.apply( schemaFile.text, true ).objectType() )
*/
        def schema = getJsonSchema(schemaFileProperty.get())
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
        def schema = getJsonSchema(schemaFileProperty.get())
        GroovyConfig.toGroovyDsl(schema, schemaName, packageName)
    }

}