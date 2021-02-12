package net.gradleutil.config

import groovy.util.logging.Slf4j
import net.gradleutil.conf.Gen
import net.gradleutil.config.extension.ConfConfig
import net.gradleutil.config.task.GenerateConfigSchema
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.caching.local.DirectoryBuildCache

import javax.inject.Inject

@Slf4j
class ConfPlugin implements Plugin<ExtensionAware> {

    private ConfConfig confConfig
    final ObjectFactory objectFactory

    @Inject
    ConfPlugin(ObjectFactory objectFactory){
        this.objectFactory = objectFactory
    }

    @Override
    void apply(ExtensionAware container) {
        assert container instanceof PluginAware
        if(!confConfig){
            confConfig = objectFactory.newInstance(ConfConfig)
        }
        if(!container.extensions.findByType(ConfConfig)){
            container.extensions.add(ConfConfig.NAMESPACE, confConfig)
        }

        if (container instanceof Project) {
            applyProject(container, confConfig)

        } else if (container instanceof Settings) {
            def settings = container as Settings
            def tempDir = objectFactory.directoryProperty().tap { set(new File(System.getProperty("java.io.tmpdir") +'/conf')) }
            confConfig.outputDirectory = objectFactory.directoryProperty().convention(tempDir)

            settings.gradle.settingsEvaluated {
                confConfig.load()
                container.gradle.ext.confConfig = confConfig
                container.gradle.ext.config = confConfig.config
                if(confConfig.generateBean.get() && confConfig.schemaFile.isPresent() ){
                    println confConfig.schemaFile.asFile.get().absolutePath
                    settings.buildCache.local= new DirectoryBuildCache().tap {directory = new File(settings.rootDir,'.gradle')}
                    includeBeanSource(settings)
                }
            }


            settings.gradle.beforeProject { Project p ->
                if(confConfig.generateBean.get()){
                    p.gradle.ext.configPluginBuildscript( p.buildscript, p )
                }
                apply(p)
            }

            container.gradle.ext.configPluginBuildscript = { ScriptHandler buildscript, Project p ->
                buildscript.with{
                    it.dependencies.add('classpath',':plugin')
                }
            }


        }
//        confConfig.configProperty.put("config.stopBubbling", "true")
    }

    static void applyProject(Project project, ConfConfig confConfig) {
        project.plugins.apply(ConfProjectPlugin)
        project.getTasks().withType(GenerateConfigSchema)
                .each { generateConfigSchema ->
                    generateConfigSchema.outputDir.convention(project.getLayout().getBuildDirectory().dir("conf"))
                }

    }

    void includeBeanSource(Settings settings){
        def genDirTemp = new File(settings.buildCache.getLocal().directory.toString() + '/plugin').tap{it.mkdirs() }
        def schemaFile = confConfig.schemaFile.asFile.get()
        def packageName = confConfig.packageName.get()
        def packagePath = packageName.replace('.','/')
        def rootClassName = confConfig.rootClassName.get()
        def dslFile = new File(genDirTemp,'src/main/groovy/' + packagePath + '/' + rootClassName + 'DSL.groovy')
/*
        new File(genDirTemp,'build.gradle').tap {it.text = """
            plugins{ id 'groovy' }\n dependencies { implementation localGroovy() }
        """}
*/
        new File(genDirTemp,'build.gradle').tap {it.text = """
            plugins{ id('groovy-gradle-plugin') }
            gradlePlugin {
                plugins {
                    simplePlugin {
                        id = '${packageName}.${rootClassName.toLowerCase()}'
                        implementationClass = '${packageName}.${rootClassName}Plugin'
                    }
                }
            }
        """.stripIndent()}

        log.info("Generating DSL from file://${schemaFile} to file://${dslFile}")
        dslFile.parentFile.mkdirs()

        new File(dslFile.parentFile,"${rootClassName}Plugin.groovy").text = """
        package ${packageName}
        import org.gradle.api.Plugin
        import org.gradle.api.Project
        class ${rootClassName}Plugin implements Plugin<Project> { 
            void apply(Project project){
                def confer = project.extensions.findByName('confConfig')
                def con = confer.load(${packageName}.${rootClassName})
                project.extensions.add('config', con)
                //project.ext.conf = con
                println 'added extension'
            } 
        }
        """.stripIndent()

        new Gen(packageName: packageName).groovyClassFromSchema(schemaFile.text, rootClassName, dslFile)
        settings.includeBuild(genDirTemp)

    }

}
