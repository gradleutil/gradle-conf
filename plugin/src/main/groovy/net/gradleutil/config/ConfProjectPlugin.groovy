package net.gradleutil.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateGroovyConfTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configuration.project.ProjectConfigurationActionContainer

import javax.inject.Inject

@Slf4j
@CompileStatic
class ConfProjectPlugin implements Plugin<Project> {

    Project project
    private TaskProvider<GenerateGroovyConfTask> generateConfig
    private final ProjectConfigurationActionContainer configurationActionContainer

    @Inject
    ConfProjectPlugin(ProjectConfigurationActionContainer configurationActionContainer, ObjectFactory objectFactory) {
        this.configurationActionContainer = configurationActionContainer
        configurationActionContainer.add {
            if (confConfig.generateBean.get()) {
                project.plugins.apply("${confConfig.packageName.get()}.${confConfig.rootClassName.get().toLowerCase()}")
            }
            if (confConfig.configObject == null || confConfig.configObject.entrySet().size() == 0) {
                log.info("loading config: project")
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
        if (!project.plugins.findPlugin(BasePlugin)) {
            project.pluginManager.apply(BasePlugin)
        }
        confConfig.configProperty.set project.providers.provider {
            confConfig.load().config
        }

        /*
        project.afterEvaluate { Project p ->
            p.tasks.getByName('javaCompile').doFirst { JavaCompile variant ->
                def jarPath = ConfProjectPlugin.getProtectionDomain().getCodeSource().getLocation().toURI().path
                variant.classpath.plus p.files(jarPath)
            }
        }
        */

        /*
        project.afterEvaluate {
            project.gradle.addListener(new DependencyResolutionListener() {
                @Override
                void beforeResolve(ResolvableDependencies resolvableDependencies) {
                    def jarPath = ConfProjectPlugin.getProtectionDomain().getCodeSource().getLocation().toURI().path
                    def compileDeps = project.configurations.getByName("implementation").getDependencies()
                    compileDeps.add(project.getDependencies().create(project.files(jarPath)))
                    project.getGradle().removeListener(this)
                }

                @Override
                void afterResolve(ResolvableDependencies resolvableDependencies) {}
            })
        }
        */

        project.extensions.add('configProperty', confConfig.configProperty)

        //confConfig.generateBean will use the generated plugin instead
        if (!confConfig.generateBean.get()) {
/*
            project.ext.config = project.providers.provider({
                project.ext.configProperty.get()
            })
*/
            //TODO: not dumb hack to resolve the properties during configure stage, versus the failed provider approach above
            project.extensions.add confConfig.projectExtensionName.get(), new ConfGetter(confConfig)
        }

        confConfig.outputDirectory.convention(project.layout.buildDirectory.dir('conf'))
        confConfig.conf.convention(project.layout.projectDirectory.file('config.conf'))
        generateConfig = project.getTasks().register("generateConfig", GenerateGroovyConfTask.class, new Action<GenerateGroovyConfTask>() {
            void execute(GenerateGroovyConfTask dslTask) {
                dslTask.outputDirectory.set(confConfig.outputDirectory)
                dslTask.schemaFile.set(confConfig.schemaFile)
                dslTask.rootClassName.set(confConfig.rootClassName)
                dslTask.packageName.set(confConfig.packageName.getOrElse(project.group.toString()))
                dslTask.schemaName.set(confConfig.schemaName)
            }
        })

        project.tasks.addRule("Pattern: printConfig<.name.space.path>") { String taskName ->
            if (taskName.startsWith("printConfig")) {
                 project.task(taskName) { Task t ->
                    t.outputs.upToDateWhen { false }
                    t.doLast {
                        def namespace = taskName.replaceAll(/printConfig\.?/, '')
                        confConfig.printConfig(namespace)
                    }
                }
            }
        }

    }

    private void configureJavaPluginDefaults() {
        JavaPluginConvention javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSet mainSourceSet = javaPluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        mainSourceSet.compileClasspath
    }

}
