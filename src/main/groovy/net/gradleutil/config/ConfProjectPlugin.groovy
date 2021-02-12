package net.gradleutil.config

import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateGroovyConfTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModule

import javax.inject.Inject

@Slf4j
class ConfProjectPlugin implements Plugin<Project> {

    Project project
    private TaskProvider<GenerateGroovyConfTask> generateConfig
    private final ProjectConfigurationActionContainer configurationActionContainer


    @Inject
    ConfProjectPlugin(ProjectConfigurationActionContainer configurationActionContainer) {
        this.configurationActionContainer = configurationActionContainer
        configurationActionContainer.add {
            if (confConfig.generateBean.get()) {
                project.plugins.apply("${confConfig.packageName.get()}.${confConfig.rootClassName.get().toLowerCase()}")
            }
            if (confConfig.configObject == null) {
                println 'loooooooooooo'
                confConfig.load()
                //project.ext.config = confConfig.load().config
            }
        }
    }

    ConfConfig getConfConfig() {
        return project.extensions.getByType(ConfConfig)
    }

    @Override
    void apply(Project project) {
        this.project = project
        confConfig.configProperty.set project.providers.provider {
            confConfig.load().config
        }
/*
        project.buildscript.configurations.classpath.with { dep ->
            println dep.resolve().each{it.dump()}
            dep.dependencies.each {
                println it.name
            }
        }
*/

        project.ext.configProperty = confConfig.configProperty

/*
        project.ext.config = project.providers.provider({
            project.ext.configProperty.get()
        })
*/
        //dumb hack to resolve the properties during configure stage, versus the failed provider approach above
        if (!confConfig.generateBean.get()) {
            project.ext.config = new ConfGetter(confConfig)
        }

        confConfig.outputDirectory.convention(project.layout.buildDirectory.dir('conf'))
        confConfig.conf.convention(project.layout.projectDirectory.file('config.conf'))

        generateConfig = project.getTasks().register("generateConfig", GenerateGroovyConfTask.class, new Action<GenerateGroovyConfTask>() {
            void execute(GenerateGroovyConfTask dslTask) {
                dslTask.outputDirectory.set(confConfig.outputDirectory)
                dslTask.schemaFileProperty.set(confConfig.schemaFile)
                dslTask.rootClassName.set(confConfig.rootClassName)
                dslTask.packageName.set(confConfig.packageName.getOrElse(project.group.toString()))
                dslTask.schemaName.set(confConfig.schemaName.get())
            }
        })

        project.tasks.addRule("Pattern: printConfig<.name.space.path>") { String taskName ->
            if (taskName.startsWith("printConfig")) {
                project.task(taskName) {
                    outputs.upToDateWhen { false }
                    doLast {
                        def namespace = taskName.replaceAll(/printConfig\.?/, '')
                        confConfig.printConfig(namespace)
                    }
                }
            }
        }

        if (confConfig.generateBean.get()) {
            project.pluginManager.apply(JavaPlugin)
            project.pluginManager.apply(IdeaPlugin)

            if (project.hasProperty('sourceSets')) {
                log.info "has sourcesets"
                project.compileJava { JavaCompile t ->
                    t.options?.annotationProcessorGeneratedSourcesDirectory?.with { confConfig.outputDirectory.get() }
                    t.options?.generatedSourceOutputDirectory?.with { confConfig.outputDirectory.get() }
                    t.dependsOn(generateConfig)
                }
                (project.idea.module as IdeaModule).with {
                    // Marks the already(!) added srcDir as "generated"
                    generatedSourceDirs += confConfig.outputDirectory.get()
                }
                // project.sourceSets.main.output.dir confConvention.outputDirectory, builtBy: generateConfig
                // project.tasks.getByName('compileJava').dependsOn(generateConfig)
            }

        }

    }

}
