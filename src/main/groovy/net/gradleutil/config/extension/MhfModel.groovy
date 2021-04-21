package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString
class MhfModel {
    File mhf

    final String name
    String modelName
    String packageName
    File outputDir

    MhfModel(String name){
        this.name = name
    }
}
