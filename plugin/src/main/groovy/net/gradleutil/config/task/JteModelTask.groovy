package net.gradleutil.config.task

import net.gradleutil.conf.transform.groovy.EPackageRenderer
import net.gradleutil.conf.transform.groovy.SchemaToGroovyClass
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileType
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class JteModelTask extends DefaultTask {
    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getSchemaDir()

    @Incremental
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getJteDir()

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
            logger.info "Schema file change ${change.changeType}: ${change.file.path}"
            update(change.file)
        }

        inputChanges.getFileChanges(jteDir).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
            logger.info "jte file change ${change.changeType}: ${change.file.path}"
            project.fileTree(schemaDir).files.each {
                update(it)
            }
        }
    }

    void update(File jsonSchema){
        def name = jsonSchema.name.replace('.schema', '').replace('.json', '')
        def packagePrefix = jsonSchema.path.replace(schemaDir.get().asFile.path, '')
                .replace(File.separator + jsonSchema.name, '')
                .replace(File.separator, '.').toLowerCase()

        def modelSourceDir = new File(getOutputDir().asFile.get(), packagePrefix.replace('.', File.separator)
                + File.separator + name.toLowerCase()).tap { mkdirs() }

        def fullPackageName = "${packageName.get()}${packagePrefix ?: ''}.${name.toLowerCase()}"

        def options = SchemaToGroovyClass.defaultOptions()
                .jsonSchema(jsonSchema.text).packageName(fullPackageName)
                .rootClassName(name).outputFile(modelSourceDir)

        if (jteDir.isPresent()) {
            options.jteDirectory(jteDir.get().asFile)
        }

        EPackageRenderer.schemaToEPackageRender(options)

    }
}