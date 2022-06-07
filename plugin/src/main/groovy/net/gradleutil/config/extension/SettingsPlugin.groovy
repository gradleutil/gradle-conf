package net.gradleutil.config.extension


import groovy.transform.CompileStatic
import net.gradleutil.conf.transform.groovy.SchemaToGroovyClass
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
class SettingsPlugin {

    static final String NAMESPACE = 'settingsPlugin'
    static final Logger log = Logging.getLogger(this)

    final ObjectFactory objectFactory
    DirectoryProperty outputDirectory
    RegularFileProperty sourceConf
    RegularFileProperty schemaFile
    Property<String> packageName
    Property<String> rootClassName
    Property<String> schemaName
    Property<ClassLoader> classLoader

    @Inject
    SettingsPlugin(ObjectFactory objects) {
        objectFactory = objects
        sourceConf = objects.fileProperty()
        schemaFile = objects.fileProperty()
        packageName = objects.property(String).convention('config')
        rootClassName = objects.property(String).convention('Config')
        schemaName = objects.property(String)
        outputDirectory = objects.directoryProperty()
        classLoader = objects.property(ClassLoader)
    }

    void apply(Settings settings) {
        def gradleExt = (settings.gradle as ExtensionAware).extensions
        generate(settings)

        settings.gradle.beforeProject { Project p ->
            def fun = (p.gradle as ExtensionAware).extensions.getByName('configPluginBuildscript') as Closure
            fun(p.buildscript, p)
        }

        gradleExt.add('configPluginBuildscript', { ScriptHandler buildscript, Project p ->
            buildscript.with {
                it.dependencies.add('classpath', ':model')
                it.repositories.with {
                    mavenLocal()
                    maven {}.setUrl "https://jitpack.io"
                }
            }
        })

        settings.includeBuild(outputDirectory)
    }

    void generate(Settings settings) {
        def outputDirectory = outputDirectory.asFile.get()
        def schemaFile = schemaFile.asFile.get()
        def packageName = packageName.get()
        def packagePath = packageName.replace('.', '/')
        def rootClassName = rootClassName.get()
        def srcDirectory = new File(outputDirectory, 'groovy')
        def dslFile = new File(srcDirectory, packagePath + '/' + rootClassName + 'DSL.groovy')
        String confVersion = '1.0.6'

        String pluginId = "${packageName}.${rootClassName.toLowerCase()}"
        String implementationClass = "${packageName}.${rootClassName}Plugin"

        log.info("Generating SettingsPlugin from file://${schemaFile} to file://${dslFile}")

        def genPluginTemplate = new net.gradleutil.gen.settingsplugin.GenPluginTemplate()
        if(classLoader.isPresent()){
            genPluginTemplate.setClassLoader(classLoader.get())
        }
        genPluginTemplate.setPluginId(pluginId)
        genPluginTemplate.setConfVersion(confVersion)
        genPluginTemplate.setConfFile(sourceConf.getAsFile().get())
        genPluginTemplate.setImplementationClass(implementationClass)
        genPluginTemplate.setPackageName(packageName)
        genPluginTemplate.setRootClassName(rootClassName)
        genPluginTemplate.setOutputDirectory(outputDirectory)
        genPluginTemplate.write()

        def options = SchemaToGroovyClass.defaultOptions().jsonSchema(schemaFile.text).packageName(packageName).rootClassName(rootClassName).outputFile(dslFile)
        options.classLoader(classLoader.get())

        SchemaToGroovyClass.schemaToSimpleGroovyClass(options)
//        def result = GroovyConfig.toGroovyDsl(JsonConfig.getSchema(schemaFile.text, true), rootClassName, packageName+'.groovydsl')
//        new File(srcDirectory, packagePath + '/' + 'groovydsl' + '/' + rootClassName + 'GroovyDSL.groovy').tap{it.parentFile.mkdirs()}.text = result

    }


}