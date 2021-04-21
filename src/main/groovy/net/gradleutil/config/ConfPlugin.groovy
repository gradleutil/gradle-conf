package net.gradleutil.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware

import javax.inject.Inject

@Slf4j
@CompileStatic
class ConfPlugin implements Plugin<ExtensionAware> {

    private final ObjectFactory objectFactory

    @Inject
    ConfPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
    }

    @Override
    void apply(ExtensionAware container) {
        assert container instanceof PluginAware
        container.plugins.apply(ConfPluginExt)
        if (container instanceof Project) {
            container.plugins.apply(ConfProjectPlugin)
            container.plugins.apply(MhfPlugin)
        } else if (container instanceof Settings) {
            container.plugins.apply(ConfSettingsPlugin)
        }
        container.plugins.apply(GithubPackagesPlugin)
        container.plugins.apply(GeneratePlugin)
    }

}
