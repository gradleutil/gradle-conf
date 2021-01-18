package gradleutil.conf


import gradleutil.conf.generator.ConfigSchema
import gradleutil.conf.task.GenerateTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.plugins.ide.idea.IdeaPlugin
import javax.inject.Inject

class GradleConfPlugin implements Plugin<Project> {

    Project project
    ConfConvention confConvention
    private final ProjectConfigurationActionContainer configurationActionContainer
    Logger logger = Logging.getLogger(GradleConfPlugin)

    @Inject
    GradleConfPlugin(ProjectConfigurationActionContainer configurationActionContainer) {
        this.configurationActionContainer = configurationActionContainer
//        configurationActionContainer.add {
//                project.ext.conf = new Loader().load(confConvention.schemaFile.get(), confConvention.conf.get())
//        }
    }

    void apply(Project project) {
        confConvention = project.extensions.create('config', ConfConvention.class, project)

        def generateConfig = project.getTasks().register("generateConfig", GenerateTask.class, new Action<GenerateTask>() {
            void execute(GenerateTask dslTask) {
                dslTask.outputDirectory.set(confConvention.outputDirectory)
                dslTask.schemaFileProperty.set(confConvention.schemaFile)
                dslTask.rootClassName.set(confConvention.rootClassName)
                dslTask.packageName.set(confConvention.packageName.getOrElse(project.group.toString()))
                dslTask.schemaName.set(confConvention.schemaName.get())
                dslTask.dslFile.set(confConvention.dslFile)
            }
        })

//        project.compileJava.source generateConfig.outputs.files, project.sourceSets.main.java

        if (confConvention.generateBean.get()) {
            project.getPluginManager().apply(JavaPlugin)
            project.getPluginManager().apply(IdeaPlugin)
        }

        if (project.hasProperty('sourceSets')) {
            logger.info "has sourcesets"
            project.compileJava { t ->
                t.options.annotationProcessorGeneratedSourcesDirectory = confConvention.outputDirectory.get()
                t.dependsOn(generateConfig)
            }
//            project.sourceSets.main.java.srcDir confConvention.outputDirectory.get(  )
            project.idea.module {
                // Marks the already(!) added srcDir as "generated"
                generatedSourceDirs += confConvention.outputDirectory.get()
            }
//            project.sourceSets.main.output.dir confConvention.outputDirectory, builtBy: generateConfig
//            project.tasks.getByName('compileJava').dependsOn(generateConfig)
        }

        project.afterEvaluate {
            if (confConvention.schemaFile.isPresent() && confConvention.conf.isPresent()) {
                if (!confConvention.schemaFile.get().exists()) {
                    logger.lifecycle "generating schema ${confConvention.schemaFile.get()}"
                    confConvention.outputDirectory.get().mkdirs()
                    ConfigSchema.configFileToSchemaFile(confConvention.conf.get(), confConvention.schemaFile.get())
                }
                def config = new Loader().load(confConvention.schemaFile.get(), confConvention.conf.get())
                confConvention.configObject = config
            }
            if(confConvention.conf.isPresent()){
                logger.info "loading config"
                if(!confConvention.conf.get().exists()){
                    logger.warn "Config '${confConvention.conf.get()}' does not exist"
                }
                def config = new Loader().load(confConvention.conf.get())
                confConvention.configObject = config
            } else {
                logger.lifecycle "No config to load..."
            }

        }


        project.tasks.register("printConfig") {
            doLast {
                println(confConvention.json())
            }
        }
    }
}
