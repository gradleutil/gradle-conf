package net.gradleutil.config.task


import net.gradleutil.conf.util.GenUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class GenerateConfigSchema extends DefaultTask {
    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputFile
    abstract RegularFileProperty getInputFile()

    @OutputFile
    abstract RegularFileProperty getOutputFile()

    @Optional
    @Input
    abstract Property<String> getRefName()

    @TaskAction
    void execute(InputChanges inputChanges) {

        logger.info(inputChanges.incremental
                ? 'Executing incrementally'
                : 'Executing non-incrementally'
        )

        inputChanges.getFileChanges(inputFile).each { change ->

            println "${change.changeType}: ${change.normalizedPath}"
            if (change.changeType == ChangeType.REMOVED) {
                outputFile.delete()
            } else {
                GenUtil.configFileToReferenceSchemaFile(inputFile.get().asFile, refName.get(), outputFile.get().asFile)
            }
        }
    }
}