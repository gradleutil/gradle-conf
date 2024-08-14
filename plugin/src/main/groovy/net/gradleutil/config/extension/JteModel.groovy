package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString
class JteModel {
    File schemaDir
    String packageName
    String name
    String toType
    File jteDir
    File outputDir
    Boolean convertToCamelCase

    JteModel(String name){
        this.name = name
        packageName = name
    }
}
