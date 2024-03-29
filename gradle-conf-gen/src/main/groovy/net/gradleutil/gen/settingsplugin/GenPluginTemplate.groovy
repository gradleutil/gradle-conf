package net.gradleutil.gen.settingsplugin

import gg.jte.TemplateOutput
import gg.jte.output.StringOutput
import groovy.util.logging.Log
import net.gradleutil.gen.Generator

@Log(value = "logger")
class GenPluginTemplate {

    File confFile
    String confVersion
    String pluginId
    String implementationClass
    String packageName
    String rootClassName
    File outputDirectory
    ClassLoader classLoader

    void write() {

        def packagePath = packageName.replace('.', '/')
        def srcDirectory = new File(outputDirectory, 'groovy')
        def packageDirectory = new File(srcDirectory, packagePath)
        packageDirectory.mkdirs()

        def opt = Generator.generatorOptions().classLoader(GenPluginTemplate.classLoader)
        def templateEngine = Generator.getTemplateEngine(opt)

        TemplateOutput buildGradle = new StringOutput()
        templateEngine.render("settingsplugin/build.gradle.jte", this, buildGradle)

        TemplateOutput pluginGroovy = new StringOutput()
        templateEngine.render("settingsplugin/plugin.groovy.jte", this, pluginGroovy)

        TemplateOutput loadedGroovy = new StringOutput()
        templateEngine.render("settingsplugin/loaded.groovy.jte", this, loadedGroovy)

        new File(outputDirectory, 'settings.gradle').text = 'rootProject.name="model"'
        new File(packageDirectory, "${rootClassName}Plugin.groovy").text = pluginGroovy
        new File(outputDirectory, 'build.gradle').text = buildGradle
        new File(packageDirectory, "loaded.groovy").text = loadedGroovy

    }

}