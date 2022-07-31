package net.gradleutil.config

import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.JteModel
import net.gradleutil.config.task.JsonSchemaModelTask
import net.gradleutil.config.task.JteModelTask
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.configuration.project.ProjectConfigurationActionContainer

import javax.inject.Inject

@Slf4j
class JteModelPlugin implements Plugin<Project> {

    Project project
    private final ProjectConfigurationActionContainer configurationActionContainer
    private final NamedDomainObjectContainer<JteModel> jsonSchemaModels


    @Inject
    JteModelPlugin(ProjectConfigurationActionContainer configurationActionContainer, ObjectFactory objectFactory) {
        this.configurationActionContainer = configurationActionContainer
        jsonSchemaModels = objectFactory.domainObjectContainer(JteModel.class)

        configurationActionContainer.add {
            jsonSchemaModels.each { model ->
                TaskProvider<JteModelTask> provider = project.getTasks().register("jteModel${model.name}", JteModelTask.class, new Action<JteModelTask>() {
                    void execute(JteModelTask modelTask) {
                        modelTask.group = 'jteModel'
                        modelTask.outputDir.set(model.outputDir ?: project.layout.buildDirectory.dir('jte-content'))
                        modelTask.schemaDir.set(model.schemaDir)
                        modelTask.jteDir.set(model.jteDir)
                        modelTask.packageName.set(model.packageName ?: project.group.toString())
                    }
                })
                GeneratePlugin.addGenerationHooks(project,provider, model.outputDir)
            }
        }
    }

    @Override
    void apply(Project project) {
        this.project = project
        if (!project.plugins.findPlugin(BasePlugin)) {
            project.pluginManager.apply(BasePlugin)
        }
        project.extensions.add('jteModel', jsonSchemaModels)
    }

}
