package net.gradleutil.config.extension

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import net.gradleutil.conf.Loader
import net.gradleutil.conf.generator.ConfigSchema
import net.gradleutil.conf.util.ConfUtil
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

class ConfConfig {
    static final String NAMESPACE = 'confConfig'
    static final log = Logging.getLogger(this)

    final ObjectFactory objectFactory
    DirectoryProperty outputDirectory
    RegularFileProperty conf
    RegularFileProperty confOverride
    RegularFileProperty schemaFile
    Property<String> baseName
    Property<String> packageName
    Property<String> rootClassName
    Property<String> schemaName
    Property<Boolean> generateBean
    Property<Boolean> generateSchema
    Property<Boolean> useSystemProperties
    Property<Boolean> useSystemEnvironment
    Property<Boolean> reloadAtConfiguration

    Config configObject
    Map<String, Object> config
    MapProperty<String, Object> configProperty

    @Inject
    ConfConfig(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        outputDirectory = objectFactory.directoryProperty()
        baseName = objectFactory.property(String).convention('config.conf')
        packageName = objectFactory.property(String).convention('config')
        rootClassName = objectFactory.property(String).convention('Config')
        schemaName = objectFactory.property(String).convention('Config')
        schemaFile = objectFactory.fileProperty().convention(outputDirectory.file("schema.json"))
        conf = objectFactory.fileProperty()
        confOverride = objectFactory.fileProperty()
        useSystemProperties = objectFactory.property(Boolean).convention(false)
        useSystemEnvironment = objectFactory.property(Boolean).convention(false)
        generateBean = objectFactory.property(Boolean).convention(false)
        generateSchema = objectFactory.property(Boolean).convention(false)
        configProperty = objectFactory.mapProperty(String, Object)
        reloadAtConfiguration = objectFactory.property(Boolean).convention(false)
    }

    ConfConfig load() {
        if (config) {
            return this
        }
        log.info("loading config")
        def loaderOptions = Loader.defaultOptions()
                .setUseSystemProperties(useSystemProperties.get())
                .setUseSystemEnvironment(useSystemEnvironment.get())
                .setBaseName(baseName.get())
                .setConf(conf.asFile.getOrNull())
                .setConfOverride(confOverride.asFile.getOrNull())
                .setSchemaFile(schemaFile.asFile.getOrNull())

        if (generateSchema.get()) {
            if (!schemaFile.getAsFile().get().exists()) {
                log.lifecycle "generating schema ${schemaFile.get()}"
                outputDirectory.getAsFile().get().mkdirs()
                ConfigSchema.configFileToSchemaFile(conf.getAsFile().get(), schemaFile.getAsFile().get())
            }
        }

        configObject = Loader.load(loaderOptions)
        config = configObject.root().unwrapped()
        log.info "loaded config (${config.keySet()})"

        this
    }

    String json(String path = '') {
        ConfUtil.configToJson(configObject, path)
    }


    void printConfig(String path = '') {
        println json(path)
    }

    static <T> T loadResource(String name, Class<T> clazz) {
        Config config = ConfigFactory.load(name)
        return Loader.create(config, clazz)
    }

    def <T> T load(Class<T> clazz) {
        return Loader.create(configObject, clazz)
    }

    static Config load(File config) {
        return Loader.load(config)
    }

}