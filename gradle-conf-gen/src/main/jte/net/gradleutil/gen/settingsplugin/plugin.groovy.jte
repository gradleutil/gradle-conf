@param net.gradleutil.gen.settingsplugin.GenPluginTemplate gen
package ${gen.getPackageName()}

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import ${gen.getPackageName()}.${gen.getRootClassName()}

@Slf4j
@CompileStatic
class ${gen.getRootClassName()}Plugin implements Plugin<Project> {

    void apply(Project project){
        log.info "applying project $project.name: ${gen.getPackageName()}.${gen.getRootClassName().toLowerCase()}"
        def confer = project.extensions.findByName('confConfig')
        ${gen.getRootClassName()} con = confer.invokeMethod('load',  ${gen.getRootClassName()}) as ${gen.getRootClassName()}
        project.extensions.add('config', con)
        log.info "finished applying project $project.name"
    }

}