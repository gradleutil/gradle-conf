package net.gradleutil.config.task

import net.gradleutil.conf.transform.TransformOptions
import net.gradleutil.conf.transform.Transformer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class JsonSchemaModelTask extends DefaultTask {
    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getSchemaDir()

    @Optional
    @InputDirectory
    abstract DirectoryProperty getJteDir()

    @Optional
    @Input
    abstract Property<String> getToType()

    @Input
    abstract Property<String> getPackageName()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void execute(InputChanges inputChanges) {

        logger.info(inputChanges.incremental ? 'Executing incrementally' : 'Executing non-incrementally')

        if (!inputChanges.incremental && jteDir.isPresent()) {
            new File(jteDir.get().asFile, 'jte.jar').with { file -> if (file.exists()) file.delete() }
        }

        inputChanges.getFileChanges(schemaDir).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
            logger.info "File change ${change.changeType}: ${change.file.path}"
            def jsonSchema = change.file
            def name = jsonSchema.name.replace('.schema', '').replace('.json', '')
            def packagePrefix = change.file.path.replace(schemaDir.get().asFile.path, '')
                    .replace(File.separator + change.file.name, '')
                    .replace(File.separator, '.').toLowerCase()

            def modelSourceDir = new File(getOutputDir().asFile.get(), packagePrefix.replace('.', File.separator) + File.separator + name.toLowerCase()).tap { mkdirs() }
            def fullPackageName = "${packageName.get()}${packagePrefix ?: ''}.${name.toLowerCase()}"
            groovyModel(jsonSchema, modelSourceDir, fullPackageName)
        }
    }

    void groovyModel(File jsonSchema, File modelSourceDir, String fullPackageName) {
        def options = Transformer.transformOptions()
                .jsonSchema(jsonSchema.text).packageName(fullPackageName)
                .toType(TransformOptions.Type.java)
                .rootClassName(name).outputFile(modelSourceDir)

        if(toType.isPresent()){
            options.toType(TransformOptions.Type.valueOf(toType.get()))
        }
        if (jteDir.isPresent()) {
            options.jteDirectory(jteDir.get().asFile)
        }

        Transformer.transform(options)
    }

    void javaModel(File jsonSchema, File modelSourceDir, String fullPackageName) {
        def modelFile = new File(modelSourceDir, name + '.java')
        def options = Transformer.transformOptions()
                .jsonSchema(jsonSchema.text).packageName(fullPackageName)
                .rootClassName(name).outputFile(modelFile)

        if (jteDir.isPresent()) {
            options.jteDirectory(jteDir.get().asFile)
        }

        Transformer.transform(options)
    }
}