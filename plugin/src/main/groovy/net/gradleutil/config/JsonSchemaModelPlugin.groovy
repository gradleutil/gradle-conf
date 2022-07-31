package net.gradleutil.config

import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.JsonSchemaModel
import net.gradleutil.config.task.JsonSchemaModelTask
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configuration.project.ProjectConfigurationActionContainer

import javax.inject.Inject

@Slf4j
class JsonSchemaModelPlugin implements Plugin<Project> {

    Project project
    private final ProjectConfigurationActionContainer configurationActionContainer
    private final NamedDomainObjectContainer<JsonSchemaModel> jsonSchemaModels


    @Inject
    JsonSchemaModelPlugin(ProjectConfigurationActionContainer configurationActionContainer, ObjectFactory objectFactory) {
        this.configurationActionContainer = configurationActionContainer
        jsonSchemaModels = objectFactory.domainObjectContainer(JsonSchemaModel.class)

        configurationActionContainer.add {
            jsonSchemaModels.each { model ->
                TaskProvider<JsonSchemaModelTask> provider = project.getTasks().register("jsonSchemaModel${model.name}", JsonSchemaModelTask.class, new Action<JsonSchemaModelTask>() {
                    void execute(JsonSchemaModelTask modelTask) {
                        modelTask.group = 'jsonSchemaModel'
                        modelTask.outputDir.set(model.outputDir ?: project.layout.buildDirectory.dir('jsm-content'))
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
        project.extensions.add('jsonSchemaModel', jsonSchemaModels)
    }

}
