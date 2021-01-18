package gradleutil.conf


import gradleutil.conf.generator.ConfigSchema
import gradleutil.conf.task.GenerateTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.plugins.ide.idea.IdeaPlugin

import javax.inject.Inject

class GradleConfSettingsPlugin implements Plugin<Settings> {

    private final ObjectFactory objectFactory
    ConfConvention confConvention

    @Inject
    GradleConfSettingsPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
    }

    void apply(Settings settings) {
        def convention = objectFactory.newInstance(ConfConvention, settings)
        settings.extensions.add("config", convention)
        settings.gradle.afterProject { p ->
            p.getPluginManager().apply(GradleConfPlugin)
        }
    }

}
