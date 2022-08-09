package net.gradleutil.config.extension

import groovy.transform.ToString

@ToString(includePackage = false, includeNames = true, excludes = 'authToken', ignoreNulls = true)
class GitLabPackageRepo {
    String name
    String owner
    String authToken
    String repositoryName
    String registryUrl
    Boolean publish = true

    GitLabPackageRepo(String name) {
        this.name = name
    }

}