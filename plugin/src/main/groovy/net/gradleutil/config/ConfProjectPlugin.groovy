package net.gradleutil.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateGroovyConfTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
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

        project.gradle.projectsEvaluated {
            if (confConfig.reloadAtConfiguration) {
                confConfig.load(true)
            }
        }

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

        addMavenPublishInfo(project)
        addShadowJarInfo(project)

    }

    static void addMavenPublishInfo(Project project) {
        project.tasks.withType(AbstractPublishToMaven).tap {
            configureEach { AbstractPublishToMaven publishTask ->
                publishTask.doLast {
                    if (publishTask instanceof PublishToMavenRepository) {
                        project.logger.lifecycle("Published ${project.group}.${project.name}:${project.version} to ${publishTask.repository.url}")
                    } else if (publishTask instanceof PublishToMavenLocal) {
                        String repoPath = project.repositories.mavenLocal().url.toURL().getFile()
                        publishTask.publication.with { p ->
                            def sb = new StringBuilder()
                            p.artifacts.each {
                                String artifactPath = p.groupId.replace(".", "/") + "/" + p.artifactId + "/" + p.version + "/" + p.artifactId + "-" + p.version
                                sb.append('\n  ' + repoPath).append(artifactPath)
                                        .append(it.classifier ? '-' + it.classifier : '').append('.' + it.extension)
                            }
                            project.logger.lifecycle("Published ${p.groupId}:${p.artifactId}:${p.version}${sb.toString()}")
                        }
                    } else {
                        project.logger.lifecycle("Published ${project.group}:${project.name}:${project.version}")
                    }
                }
            }
        }
    }

    static String formatFileSize(String name, long length) {
        def formatStr = "%,10.2f"
        return "${String.format(formatStr, length / 1024 / 1024)} Mb " + name
    }

    static void addShadowJarInfo(Project project) {
        if (project.extensions.extraProperties.has('addShadowJarInfo')) {
            return
        }
        project.extensions.extraProperties['addShadowJarInfo'] = 'true'
        project.tasks.withType(Jar).configureEach { Jar jarTask ->
            if (!jarTask.class.name.contains('shadow')) {
                return
            }
            Set<File> jars = []
            jarTask.eachFile { DefaultFileCopyDetails file ->
                if (file.name.endsWith('.jar')) {
                    jars.add(file.file as File)
                }
            }
            jarTask.doLast {
                jars.sort { it.length() }.each {
                    file ->
                        jarTask.project.logger.lifecycle(formatFileSize(file.name, file.length())
                                + getDependencyFromFile(jarTask.project, file))
                }
                def jarFile = jarTask.outputs.files.first() as File
                jarTask.project.logger.lifecycle('Total: ' + formatFileSize(jarFile.name, jarFile.length()))
            }
        }
    }

    @CompileStatic
    static String getDependencyFromFile(Project project, File file) {
        String name = null
        project.configurations.each { conf ->
            if (!conf.canBeResolved || ['default','archives'].contains(conf.name) ) {
                return
            }
            conf.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact it ->
                if (it.file.name == file.name) {
                    name = it.id.getComponentIdentifier()
                }
            }
        }
        return " (${name})"
    }

}
