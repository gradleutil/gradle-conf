package net.gradleutil.config.task

import net.gradleutil.conf.transform.groovy.SchemaToGroovyClass
import net.gradleutil.conf.util.GenUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class MhfModel extends DefaultTask {
    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFile
    abstract RegularFileProperty getMhf()

    @Input
    abstract Property<String> getModelName()

    @Input
    abstract Property<String> getPackageName()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void execute(InputChanges inputChanges) {

        logger.info(inputChanges.incremental
                ? 'Executing incrementally'
                : 'Executing non-incrementally'
        )

        inputChanges.getFileChanges(mhf).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
            println "File change ${change.changeType}: ${change.normalizedPath}"
            def jsonSchema = GenUtil.configFileToReferenceSchemaJson(mhf.get().asFile, modelName.get())
            def modelFile = new File(getOutputDir().asFile.get(), modelName.get() + '.groovy')
            SchemaToGroovyClass.schemaToSimpleGroovyClass(jsonSchema, packageName.get(), modelName.get(), modelFile)
        }
    }
}