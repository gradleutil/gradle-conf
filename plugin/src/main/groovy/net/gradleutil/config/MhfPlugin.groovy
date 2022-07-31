package net.gradleutil.config


import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateGroovyConfTask
import net.gradleutil.config.task.MhfModel
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.configuration.project.ProjectConfigurationActionContainer

import javax.inject.Inject

@Slf4j
class MhfPlugin implements Plugin<Project> {

    Project project
    private final ProjectConfigurationActionContainer configurationActionContainer
    private final NamedDomainObjectContainer<net.gradleutil.config.extension.MhfModel> mhfModels


    @Inject
    MhfPlugin(ProjectConfigurationActionContainer configurationActionContainer, ObjectFactory objectFactory) {
        this.configurationActionContainer = configurationActionContainer
        mhfModels = objectFactory.domainObjectContainer(net.gradleutil.config.extension.MhfModel.class)

        configurationActionContainer.add {
            mhfModels.each{model ->
                TaskProvider<MhfModel> provider = project.getTasks().register("mhfModel${it.name}", MhfModel.class, new Action<MhfModel>() {
                    void execute(MhfModel modelTask) {
                        modelTask.group = 'mhf'
                        modelTask.outputDir.set(model.outputDir ?: project.layout.buildDirectory.dir('mhf-content'))
                        modelTask.mhf.set(model.mhf)
                        modelTask.modelName.set(model.name)
                        modelTask.packageName.set(model.packageName ?: project.group.toString())
                    }
                })
                GeneratePlugin.addGenerationHooks(project, provider, model.outputDir)
            }
        }
    }

    @Override
    void apply(Project project) {
        this.project = project
        if(!project.plugins.findPlugin(BasePlugin)){
            project.pluginManager.apply(BasePlugin)
        }
        project.extensions.add('mhfModel', mhfModels)

    }

}
