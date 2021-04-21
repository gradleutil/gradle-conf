package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true, excludes = 'apiKey', ignoreNulls = true)
class GithubPackageRepo {
    String name
    String user
    String owner
    String apiKey
    String repositoryName
    String registryUrl
    Boolean publish = true

    GithubPackageRepo(String name) {
        this.name = name
    }

}