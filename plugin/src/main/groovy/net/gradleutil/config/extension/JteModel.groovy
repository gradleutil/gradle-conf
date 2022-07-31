package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString
class JteModel {
    File schemaDir
    String packageName
    String name
    File jteDir
    File outputDir

    JteModel(String name){
        this.name = name
        packageName = name
    }
}
