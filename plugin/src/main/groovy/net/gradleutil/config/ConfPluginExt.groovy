package net.gradleutil.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.extension.SettingsPlugin
import org.gradle.api.Plugin
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware

import javax.inject.Inject

@Slf4j
@CompileStatic
class ConfPluginExt implements Plugin<ExtensionAware> {

    private final ObjectFactory objectFactory
    private PluginAware pluginContainer
    final ConfConfig confConfig
    final SettingsPlugin settingsPlugin

    @Inject
    ConfPluginExt(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        confConfig = objectFactory.newInstance(ConfConfig)
        settingsPlugin = objectFactory.newInstance(SettingsPlugin)
    }

    @Override
    void apply(ExtensionAware container) {
        pluginContainer = container as PluginAware
        if (!container.extensions.findByType(ConfConfig)) {
            container.extensions.add(ConfConfig.NAMESPACE, confConfig)
        }
        container.extensions.add(SettingsPlugin.NAMESPACE, settingsPlugin)
    }

}
