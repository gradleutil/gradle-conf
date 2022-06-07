@param net.gradleutil.gen.settingsplugin.GenPluginTemplate gen
package ${gen.getPackageName()}

import groovy.transform.CompileStatic
import net.gradleutil.conf.Loader

@CompileStatic
class loaded {
    ${gen.getRootClassName()} example = Loader.create(new File('${gen.getConfFile().getAbsolutePath()}').text, ${gen.getRootClassName()})
}
