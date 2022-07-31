package net.gradleutil.config.extension

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import net.gradleutil.conf.Loader
import net.gradleutil.conf.config.Config
import net.gradleutil.conf.config.ConfigFactory
import net.gradleutil.conf.util.ConfUtil
import net.gradleutil.conf.util.GenUtil
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

@Slf4j
@CompileStatic
class ConfConfig {
    static final String NAMESPACE = 'confConfig'

    final ObjectFactory objectFactory
    DirectoryProperty outputDirectory
    DirectoryProperty modelPluginDirectory
    RegularFileProperty conf
    RegularFileProperty confOverride
    RegularFileProperty schemaFile
    Property<String> baseName
    Property<String> packageName
    Property<String> rootClassName
    Property<String> schemaName
    Property<String> projectExtensionName
    Property<Boolean> allowUnresolved
    Property<Boolean> generateBean
    Property<Boolean> generateSchema
    Property<Boolean> useSystemProperties
    Property<Boolean> useSystemEnvironment
    Property<Boolean> reloadAtConfiguration
    Property<Boolean> silent

    Config configObject
    Map<String, Object> config
    MapProperty<String, Object> configProperty
    ClassLoader classLoader

    @Inject
    ConfConfig(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        outputDirectory = objectFactory.directoryProperty()
        modelPluginDirectory = objectFactory.directoryProperty()
        projectExtensionName = objectFactory.property(String).convention('config')
        baseName = objectFactory.property(String).convention('config.conf')
        packageName = objectFactory.property(String).convention('config')
        rootClassName = objectFactory.property(String).convention('Config')
        schemaName = objectFactory.property(String).convention('Config')
        schemaFile = objectFactory.fileProperty()//.convention(outputDirectory.file("schemas.json"))
        conf = objectFactory.fileProperty()
        confOverride = objectFactory.fileProperty()
        allowUnresolved = objectFactory.property(Boolean).convention(true)
        useSystemProperties = objectFactory.property(Boolean).convention(false)
        useSystemEnvironment = objectFactory.property(Boolean).convention(false)
        generateBean = objectFactory.property(Boolean).convention(false)
        generateSchema = objectFactory.property(Boolean).convention(false)
        silent = objectFactory.property(Boolean).convention(true)
        configProperty = objectFactory.mapProperty(String, Object)
        reloadAtConfiguration = objectFactory.property(Boolean).convention(false)
        classLoader = objectFactory.class.classLoader
    }

    ConfConfig load() {
        if (config) {
            return this
        }
        log.info("loading config: ${ conf.asFile.getOrNull() }")
        def loaderOptions = Loader.defaultOptions()
                .useSystemProperties(useSystemProperties.get())
                .useSystemEnvironment(useSystemEnvironment.get())
                .baseName(baseName.get())
                .conf(conf.asFile.getOrNull())
                .confOverride(confOverride.asFile.getOrNull())
                .schemaFile(schemaFile.asFile.getOrNull())
                .allowUnresolved(allowUnresolved.get())
                .classLoader(classLoader)
                .silent(silent.get())

        if (generateSchema.get()) {
            if (!schemaFile.getAsFile().get().exists()) {
                log.info "generating schema ${schemaFile.get()}"
                outputDirectory.getAsFile().get().mkdirs()
                GenUtil.configFileToReferenceSchemaFile(conf.getAsFile().get(), rootClassName.get(), schemaFile.getAsFile().get())
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
        return Loader.create(configObject, clazz, Loader.defaultOptions().allowUnresolved(true).silent(false))
    }

    static Config load(File config) {
        return Loader.load(config)
    }

}