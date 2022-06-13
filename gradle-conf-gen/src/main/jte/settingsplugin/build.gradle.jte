@param net.gradleutil.gen.settingsplugin.GenPluginTemplate gen
plugins{
    id('groovy-gradle-plugin')
}
version = '1.0.0'
//buildscript.dependencies.classpath files("${gen.getConfFile().getAbsolutePath()}")

sourceSets.main.groovy.srcDirs = ['groovy']
sourceSets.main.java.srcDirs = []
sourceSets.main.resources.srcDirs = []

//sourceSets.main.resources.srcDirs layout.files('${gen.getConfFile().getAbsolutePath()}')

repositories {
    mavenLocal()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "net.gradleutil:conf-gen:${gen.getConfVersion()}"
}

gradlePlugin {
    plugins {
        simplePlugin {
            id = '${gen.getPluginId()}'
            implementationClass = '${gen.getImplementationClass()}'
        }
    }
}
