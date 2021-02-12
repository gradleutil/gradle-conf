package net.gradleutil.config

import net.gradleutil.config.extension.ConfConfig

class ConfGetter {
    private Map<Object,Object> configMap
    private final ConfConfig confConfig

    ConfGetter(final ConfConfig confConfig){
        this.confConfig = confConfig
    }

    def getConfig(){
        if(!configMap){
            configMap = confConfig.load().config as Map<Object, Object> ?: [:]
        }
        configMap
    }

    def propertyMissing(String name) {
        getConfig().get(name)
    }
}