package net.gradleutil.config


import groovy.transform.CompileStatic
import net.gradleutil.conf.util.Inflector
import net.gradleutil.config.extension.GenerateExtension
import net.gradleutil.config.extension.GithubPackageRepo
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
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

import javax.inject.Inject

@CompileStatic
class GithubPackagesPlugin implements Plugin<ExtensionAware> {

    static final Logger log = Logging.getLogger(this)
    static final Inflector inflector = new Inflector()

    final ObjectFactory objectFactory
    final NamedDomainObjectContainer<GithubPackageRepo> githubPackageRepos
    Property<String> githubUser
    Property<String> githubApiKey
    Property<String> githubRepositoryName
    Property<String> githubRegistryUrl
    ListProperty<String> buildscriptDependencies
    ListProperty<String> repositoryUrls
    ExtensionAware container

    @Inject
    GithubPackagesPlugin(ObjectFactory objects) {
        objectFactory = objects
        githubPackageRepos = objectFactory.domainObjectContainer(GithubPackageRepo.class)
        githubUser = objects.property(String).convention(System.properties.get('user.name') as String)
        githubApiKey = objects.property(String)
        githubRepositoryName = objects.property(String).convention(new File(System.properties.get('user.dir') as String).name)
        githubRegistryUrl = objects.property(String)
        String ghUsername = System.getenv("GITHUB_PACKAGE_REGISTRY_USER") ?: System.getenv("GITHUB_ACTOR") ?: System.properties.get('user.name')
        String ghPassword = System.getenv("GITHUB_PACKAGE_REGISTRY_API_KEY") ?: System.getenv("GITHUB_TOKEN") ?: ''
        githubUser.convention(ghUsername as String)
        githubApiKey.convention(ghPassword as String)
        buildscriptDependencies = objects.listProperty(String).convention([])
        repositoryUrls = objects.listProperty(String).convention([])
    }

    @Override
    void apply(ExtensionAware container) {
        this.container = container
        container.extensions.add('githubPackagesRepo', githubPackageRepos)
        if (container instanceof Settings) {
            applySettings(container as Settings)
        } else {
            applyProject(container as Project)
        }
    }

    void applySettings(Settings settings) {
        settings.gradle.settingsEvaluated {
            String defaultUrl = "https://maven.pkg.github.com/${githubUser.get()}/${githubRepositoryName.get()}"
            githubRegistryUrl.convention(defaultUrl)
        }
        settings.gradle.beforeProject {
            applyProject(settings.gradle.rootProject)
        }
    }

    void applyProject(Project project) {
        if(project.extensions.findByType(GithubPackagesPlugin)){
            return
        }
        project.allprojects { Project p ->
            p.apply plugin: 'maven-publish'
            p.afterEvaluate {
                getGithubPackageRepos().each {
                    log.info "adding github packages repo ${it.toString()}"
                }
                applyBuildscript(p.buildscript)
                applyRepositories(p.repositories)
                applyPublishing(p.extensions.getByType(PublishingExtension), p)
            }
        }
    }


    def addGithubPackagesRepo(RepositoryHandler repositories, GithubPackageRepo githubPackageRepo) {
        repositories.with {
            addLast(
                repositories.maven(new Action<MavenArtifactRepository>() {
                    void execute(MavenArtifactRepository repo) {
                        repo.name = githubPackageRepo.name ?: githubUser.get()
                        repo.url = githubPackageRepo.registryUrl ?: githubRegistryUrl.get()
                        repo.credentials.username = githubPackageRepo.user ?: githubUser.get()
                        repo.credentials.password = githubPackageRepo.apiKey ?: githubApiKey.get()
                    }
                })
            )
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
        githubPackageRepos.each { addGithubPackagesRepo(repositories, it) }
    }

    def applyPublishing(PublishingExtension publishing, Project p) {
        githubPackageRepos.each { githubPackageRepo ->
            if (githubPackageRepo.publish) {
                log.info "adding publication for repo ${githubPackageRepo}"
                publishing.with {
                    addGithubPackagesRepo(it.repositories, githubPackageRepo)
                    it.publications.create('githubMaven' + githubPackageRepo.name, MavenPublication) {
                        //todo: figure out how we want to abstract this
                        if(p.components.findByName('java')){
                            it.from p.components.getByName('java')
                        } else {
                            p.components.each{
                                log.info "ignoring component: ${it.name}"
                            }
                        }
                    }
                }
            }
        }
    }

}