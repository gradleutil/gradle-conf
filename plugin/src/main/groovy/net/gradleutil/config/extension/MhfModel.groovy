package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString
class MhfModel extends JteModel {
    File mhf

    MhfModel(String name){
        super(name)
    }
}
