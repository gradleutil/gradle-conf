package gradleutil.conf

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigRenderOptions
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

class ConfConvention {

    private final Project project
    Property<File> outputDirectory
    Property<File> dslFile
    Property<File> schemaFile
    Property<File> conf
    Property<String> rootClassName
    Property<String> packageName
    Property<String> schemaName
    Property<Boolean> generateBean
    Config configObject

    ConfConvention(Project project) {
        this.project = project
        def generatedResourceDir = project.file("$project.buildDir/generated-resources")
        outputDirectory = project.objects.property(File).convention(generatedResourceDir)
        dslFile = project.objects.property(File).convention(project.file("$generatedResourceDir/DSL.groovy"))
        rootClassName = project.objects.property(String).convention('Config')
        packageName = project.objects.property(String)
        schemaName = project.objects.property(String).convention('Config')
        schemaFile = project.objects.property(File).convention(project.file("$generatedResourceDir/schema.json"))
        conf = project.objects.property(File).convention(project.file('config.conf'))
        generateBean = project.objects.property(Boolean).convention(true)
    }

    @Inject
    ConfConvention(ObjectFactory objectFactory, Settings settings) {
        def projectSettings = settings.rootProject
        def generatedResourceDir = new File( System.getProperty("java.io.tmpdir") )
        outputDirectory = objectFactory.property(File).convention(generatedResourceDir)
        dslFile = objectFactory.property(File).convention(new File("$generatedResourceDir/DSL.groovy"))
        rootClassName = objectFactory.property(String).convention('Config')
        packageName = objectFactory.property(String)
        schemaName = objectFactory.property(String).convention('Config')
        schemaFile = objectFactory.property(File).convention(new File("$generatedResourceDir/schema.json"))
        conf = objectFactory.property(File).convention(new File('config.conf'))
        generateBean = objectFactory.property(Boolean).convention(true)
    }


    String json(String path = '' ) {
        def jsonString
        if(!configObject){
            throw new Exception("No config to print")
        }
        if( path ) {
            if( !configObject.hasPath( path ) ) {
                throw new Exception( "Config does not have path ${ path }" )
            }
            jsonString = configObject.getValue( path ).render( ConfigRenderOptions.concise().setFormatted( true ) )
        } else {
            jsonString = configObject.root().render( ConfigRenderOptions.concise().setFormatted( true ) )
        }
        return jsonString
    }


    String printConfig( String path = '' ) {
        println json( path )
    }

    static <T> T loadResource(String name, Class<T> clazz) {
        Config config = ConfigFactory.load( name)
        return Loader.create(config, clazz)
    }



/*
    def methodMissing(String name, args) {
        println "WAAAAA ${name} ${args}"
    }

    def propertyMissing(String name) {
//        return new LazyResolver()
        return { name } as Provider<String>
    }

    class LazyResolver {

        def propertyMissing(String name) {
            println "ERAAAAA ${name}"
            return { name } as Provider<String>
//            return new LazyResolver()
        }

    }
*/

}