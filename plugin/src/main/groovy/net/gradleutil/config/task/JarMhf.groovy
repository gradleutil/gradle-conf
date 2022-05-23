package net.gradleutil.config.task

import net.gradleutil.gen.Generator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class JarMhf extends DefaultTask {
    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getInputDir()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void execute(InputChanges inputChanges) {

        logger.info(inputChanges.incremental
                ? 'Executing incrementally'
                : 'Executing non-incrementally'
        )

        inputChanges.getFileChanges(inputDir).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
            def jarPath = new File(getOutputDir().asFile.get(), 'mhf.jar').toPath()

            println "${change.changeType}: ${change.normalizedPath}"
            Generator.jar(getInputDir().asFile.get().toPath(), jarPath)

        }
    }
}