package net.gradleutil.config


import net.gradleutil.config.extension.GitLabPackageRepo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.HttpHeaderAuthentication

import javax.inject.Inject

class GitLabPackagesPlugin implements Plugin<ExtensionAware> {

    static final Logger log = Logging.getLogger(this)

    final ObjectFactory objectFactory
    final NamedDomainObjectContainer<GitLabPackageRepo> gitLabPackageRepos
    Property<String> gitLabAuthToken
    Property<String> gitLabRepositoryName
    Property<String> gitLabRegistryUrl
    ListProperty<String> buildscriptDependencies
    ListProperty<String> repositoryUrls
    ExtensionAware container

    @Inject
    GitLabPackagesPlugin(ObjectFactory objects) {
        objectFactory = objects
        gitLabPackageRepos = objectFactory.domainObjectContainer(GitLabPackageRepo.class)
        gitLabAuthToken = objects.property(String)
        gitLabRepositoryName = objects.property(String).convention(new File(System.properties.get('user.dir') as String).name)
        gitLabRegistryUrl = objects.property(String)
        buildscriptDependencies = objects.listProperty(String).convention([])
        repositoryUrls = objects.listProperty(String).convention([])
    }

    @Override
    void apply(ExtensionAware container) {
        this.container = container
        container.extensions.add('gitLabPackagesRepo', gitLabPackageRepos)
        if (container instanceof Settings) {
            applySettings(container as Settings)
        } else {
            applyProject(container as Project)
        }
    }

    void applySettings(Settings settings) {
        String defaultUrl = "https://maven.pkg.gitLab.com/${gitLabRepositoryName.get()}"
        gitLabRegistryUrl.convention(defaultUrl)
        settings.gradle.beforeProject {
            applyRepositories(settings.pluginManagement.repositories)
            applyProject(settings.gradle.rootProject)
        }
    }

    void applyProject(Project project) {
        if (project.extensions.findByType(GitLabPackagesPlugin)) {
            return
        }
        project.allprojects { Project p ->
            if ((p.plugins.findPlugin(ConfPlugin) as ConfPlugin)?.evaluatedProjects?.contains(p.name)) {
                return
            }
            p.apply plugin: 'maven-publish'
            p.afterEvaluate {
                applyBuildscript(p.buildscript)
                applyRepositories(p.repositories)
                applyPublishing(p.extensions.getByType(PublishingExtension), p)
            }
        }
    }


    def addGitLabPackagesRepo(RepositoryHandler repositories, GitLabPackageRepo gitLabPackageRepo) {
        log.info "adding gitLab packages repo ${gitLabPackageRepo.toString()}"
        repositories.with {
            addLast(repositories.maven(new Action<MavenArtifactRepository>() {
                void execute(MavenArtifactRepository repo) {
                    repo.url = gitLabPackageRepo.registryUrl ?: gitLabRegistryUrl.get()
                    repo.allowInsecureProtocol = true
                    def token = gitLabPackageRepo.authToken ?: gitLabAuthToken.getOrElse('')
                    if (!token) {
                        log.warn "no authToken provided for ${repo.url}"
                    }
                    repo.credentials(HttpHeaderCredentials) {
                        it.setName "PRIVATE-TOKEN"
                        it.setValue token
                    }
                    repo.authentication {
                        it.register("header", HttpHeaderAuthentication)
                    }
                }
            }))
        }
    }


    def applyBuildscript(ScriptHandler buildscript) {
        buildscript.with {
            applyRepositories(repositories)
            buildscriptDependencies.get().each {
                dependencies.add('classpath', it)
            }
        }
    }

    def applyRepositories(RepositoryHandler repositories) {
        repositories.with { handler ->
            repositoryUrls.get().each {
                handler.maven {}.url = it
            }
        }
        gitLabPackageRepos.each { addGitLabPackagesRepo(repositories, it) }
    }

    def applyPublishing(PublishingExtension publishing, Project p) {
        gitLabPackageRepos.each { gitLabPackageRepo ->
            if (gitLabPackageRepo.publish) {
                log.info "adding publication for repo ${gitLabPackageRepo}"
                publishing.with {
                    addGitLabPackagesRepo(it.repositories, gitLabPackageRepo)
                    it.publications.create('gitLabMaven' + gitLabPackageRepo.name, MavenPublication) {
                        //todo: figure out how we want to abstract this
                        if (p.components.findByName('java')) {
                            it.from p.components.getByName('java')
                        } else {
                            p.components.each {
                                log.info "ignoring component: ${it.name}"
                            }
                        }
                    }
                }
            }
        }
    }

}