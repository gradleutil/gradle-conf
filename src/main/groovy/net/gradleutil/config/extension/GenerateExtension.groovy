package net.gradleutil.config.extension

import groovy.transform.CompileStatic
import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
@CompileStatic
class GenerateExtension {
    public File outputDirectory
    public File sourceConf
    public File sourceSchemaFile
    public File targetSchemaFile
    public String packageName
    public String rootClassName
    public String schemaName
    public String dslFileName
    public String name

    GenerateExtension(String name) {
        this.name = name
    }

    public void setSourceConf(File sourceConf){
        this.sourceConf = sourceConf
    }

    public File getSourceConf(){
        this.sourceConf
    }

}