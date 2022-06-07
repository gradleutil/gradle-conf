package net.gradleutil.config

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.extension.SettingsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware

import javax.inject.Inject

@Slf4j
@CompileStatic
class ConfSettingsPlugin implements Plugin<Settings> {

    private final ObjectFactory objectFactory
    final SettingsPlugin settingsPlugin
    Settings settings


    @Inject
    ConfSettingsPlugin(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        settingsPlugin = objectFactory.newInstance(SettingsPlugin)
    }

    ConfConfig getConfConfig() {
        return settings.extensions.getByType(ConfConfig)
    }

    @Override
    void apply(Settings settings) {
        this.settings = settings
        def srcDir = objectFactory.directoryProperty().tap { set(new File(settings.rootDir, 'src/model')) }
        confConfig.outputDirectory = objectFactory.directoryProperty().convention(srcDir)
        def gradleExt = (settings.gradle as ExtensionAware).extensions

        settings.gradle.settingsEvaluated {
            log.info "configuration begun"
            confConfig.load()
            gradleExt.add('config', confConfig.config)
            settings.extensions.add('config', confConfig.config)
            if (confConfig.generateBean.get() && confConfig.schemaFile.isPresent()) {
                println confConfig.schemaFile.asFile.get().absolutePath
//                    settings.buildCache.local= new DirectoryBuildCache().tap {directory = new File(settings.rootDir,'.gradle')}
                settingsPlugin.outputDirectory.set(confConfig.outputDirectory)
                settingsPlugin.schemaFile.set(confConfig.schemaFile)
                settingsPlugin.schemaName.set(confConfig.schemaName)
                settingsPlugin.rootClassName.set(confConfig.rootClassName)
                settingsPlugin.packageName.set(confConfig.packageName)
                settingsPlugin.sourceConf.set(confConfig.conf)
                settingsPlugin.classLoader.set(ConfSettingsPlugin.classLoader)
                settingsPlugin.apply(settings)
            }
            gradleExt.add('confConfig', confConfig)
            log.info "configuration complete"
        }

        settings.gradle.beforeProject { Project p ->
            p.extensions.add('confConfig',confConfig)
            p.plugins.apply(ConfPlugin)
        }

    }

}
