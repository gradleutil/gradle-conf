package net.gradleutil.config

import groovy.transform.CompileStatic
import net.gradleutil.conf.json.schema.Schema
import net.gradleutil.conf.transform.groovy.GroovyConfig
import net.gradleutil.conf.transform.groovy.SchemaToGroovyClass
import net.gradleutil.conf.transform.json.JsonToSchema
import net.gradleutil.config.extension.GenerateExtension
import net.gradleutil.config.task.GenerateConfigSchema
import net.gradleutil.config.task.GenerateGroovyConfTask
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.PluginAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel

import javax.inject.Inject

@CompileStatic
class GeneratePlugin implements Plugin<ExtensionAware> {

    static final String NAMESPACE = 'generate'
    static final Logger log = Logging.getLogger(this)

    final ObjectFactory objectFactory
    public final NamedDomainObjectContainer<GenerateExtension> generateExtensions
    private PluginAware pluginContainer


    @Inject
    GeneratePlugin(ObjectFactory objects) {
        objectFactory = objects
        generateExtensions = objectFactory.domainObjectContainer(GenerateExtension.class)
    }

    @Override
    void apply(ExtensionAware container) {
        pluginContainer = container as PluginAware
        container.extensions.add(GeneratePlugin.NAMESPACE, generateExtensions)
        pluginContainerEvaluated {
            afterEvaluate(container, generateExtensions)
        }
    }

    void afterEvaluate(ExtensionAware container, NamedDomainObjectContainer<GenerateExtension> generateExtensions) {
        generateExtensions.each { generateExtension ->
            log.info('visiting gen extension ' + generateExtension.name)
            if (container instanceof Project) {
                def project = container as Project
                if (!project.plugins.findPlugin(GroovyPlugin)) {
                    project.pluginManager.apply(GroovyPlugin)
                }
                if (!project.plugins.findPlugin(IdeaPlugin)) {
                    project.pluginManager.apply(IdeaPlugin)
                }
                generateExtension.outputDirectory = generateExtension.outputDirectory ?: project.layout.buildDirectory.dir('conf-generated').get().asFile
                project.dependencies.add('implementation', project.dependencies.localGroovy())

                if (generateExtension.dslFileName) {
                    dsl(generateExtension)
                }
                TaskProvider<GenerateGroovyConfTask> generateGroovyConfTaskTaskProvider = project.getTasks().register(
                        "generateModel" + generateExtension.name.capitalize(), GenerateGroovyConfTask.class, { task ->
                })
                generateGroovyConfTaskTaskProvider.configure({ task ->
                    task.outputDirectory.set(generateExtension.outputDirectory)
                    task.schemaFile.set(generateExtension.sourceSchemaFile)
                    task.rootClassName.set(generateExtension.rootClassName)
                    task.packageName.set(generateExtension.packageName)
                    task.schemaName.set(generateExtension.schemaName)
                })
                if (generateExtension.sourceConf && generateExtension.targetSchemaFile) {
                    TaskProvider<GenerateConfigSchema> generateConfSchemaTaskProvider = project.getTasks().register(
                            "generateConfigSchema" + generateExtension.name.capitalize(), GenerateConfigSchema.class, { task ->
                        task.inputFile.set(generateExtension.sourceConf)
                        task.refName.set(generateExtension.name)
                        task.outputFile.set(generateExtension.targetSchemaFile)
                    })
                    generateGroovyConfTaskTaskProvider.get().schemaFile.set(generateConfSchemaTaskProvider.get().outputFile)
                }

                if (project.hasProperty('sourceSets')) {
                    log.info "has sourcesets"
                    project.tasks.getByName('compileJava').with {
                        def t = it as JavaCompile
                        t.options?.generatedSourceOutputDirectory?.with { generateExtension.outputDirectory }
                        t.dependsOn(generateGroovyConfTaskTaskProvider)
                    }
                    (project.extensions.getByName('idea') as IdeaModel).getModule().with {
                        log.info "adding idea generated directory marker: ${generateExtension.outputDirectory}"
                        // Marks the already(!) added srcDir as "generated"
                        it.generatedSourceDirs.add(generateExtension.outputDirectory)
                    }
                }
                project.afterEvaluate {
                }
            }
        }
    }

    void dsl(GenerateExtension genExt) {
        if (!genExt.outputDirectory) {
            throw new GradleException("DSL output file path is required")
        }
        if (!genExt.sourceSchemaFile || !genExt.sourceSchemaFile.exists()) {
            throw new GradleException("JSON schema file path is required")
        }
        def dest = genExt.outputDirectory
        dest.mkdirs()
        def dslFile = new File(dest.path + '/' + genExt.packageName.replace('.', '/') + '/' + genExt.dslFileName)
        log.lifecycle("Generating DSL from file://${genExt.sourceSchemaFile} to file://${dslFile}")
        dslFile.parentFile.mkdirs()
        SchemaToGroovyClass.schemaToSimpleGroovyClass(genExt.sourceSchemaFile.text, genExt.packageName, genExt.rootClassName, dslFile)
//        def configuration = new ConfigEater(schema: schemaFile)
//        dslFile.text = configuration.toGroovyDsl(packageName, schemaName)
    }


    static void generateGradleExtension(GenerateExtension genExt) {
        def schema = getJsonSchema(genExt.sourceSchemaFile)
        println GroovyConfig.toGroovyDsl(schema, 'schema', genExt.packageName)
    }


    private static Schema getJsonSchema(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("Schema file ${file} does not exist")
        }
        try {
            return JsonToSchema.getSchema(file.text)
        } catch (Exception e) {
            log.error("Error loading schema: ${e.message}")
        }
        null
    }

    String toGroovyDsl(GenerateExtension genExt) {
        def schema = getJsonSchema(genExt.sourceSchemaFile)
        GroovyConfig.toGroovyDsl(schema, genExt.schemaName, genExt.packageName)
    }

    void pluginContainerEvaluated(Closure closure) {
        if (pluginContainer instanceof Settings) {
            pluginContainer.gradle.settingsEvaluated closure
        } else if (pluginContainer instanceof Project) {
            pluginContainer.afterEvaluate closure
        }
    }

}