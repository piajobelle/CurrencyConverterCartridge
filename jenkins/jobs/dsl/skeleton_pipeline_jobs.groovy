def projectFolderName = "DSL_PROJECT"

//Jobs
def buildMavenJob = freeStyleJob(projectFolderName + "/Cartridge_BuildAndPackage_Activity")
def buildSonarJob = freeStyleJob(projectFolderName + "/Cartridge_CodeAnalysis_Activity")
def buildNexusBackupJob = freeStyleJob(projectFolderName + "/Cartridge_NexusBackup_Activity")
def buildAnsibleBuildJob = freeStyleJob(projectFolderName + "/Cartridge_AnsibleBuild_Activity")
def buildSeleniumCodeAnalysisJob = freeStyleJob(projectFolderName + "/Cartridge_SeleniumAnalysis_Activity")
def buildNexusReleaseJob = freeStyleJob(projectFolderName + "/Cartridge_ReleaseProject_Activity")

//View
def pipelineView = buildPipelineView(projectFolderName + "/Cartridge_CurrencyConverter_Pipeline")

pipelineView.with {
    title('/Cartridge_CurrencyConverter_Pipeline')
    displayedBuilds(3)
    selectedJob(projectFolderName + "/Cartridge_BuildAndPackage_Activity")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}

folder("${projectFolderName}") {
    displayName("${projectFolderName}")
    description("${projectFolderName}")
}

buildMavenJob.with {
    // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('Cartridge_NexusBackup_Activity')
        }
    }  
    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:piajobelle1/CurrencyConverterDTS.git')
                credentials('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            }
        }
    }
    // build triggers
     triggers {
        gitlabPush {
            buildOnMergeRequestEvents(true)
            buildOnPushEvents(true)
            enableCiSkip(false)
            setBuildDescription(false)
            rebuildOpenMergeRequest('never')
        }
    }
    //build environment
    wrappers {
        preBuildCleanup()
    }
    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('package')
        }
    }
    // post build actions
     publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('Cartridge_CodeAnalysis_Activity', 'SUCCESS')
    }
}

buildSonarJob.with {
    // source code management  
    scm {
        git {
            remote {
                url('git@gitlab:piajobelle1/CurrencyConverterDTS.git')
                credentials('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            }
        }
    }
    // build environment
    wrappers {
        preBuildCleanup()
    }
    // build
    configure { project ->
        project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
            properties('''sonar.projectKey=hudson.plugins.sonar.SonarRunnerBuilder
            sonar.projectName=testjobActivity
            sonar.projectVersion=1
            sonar.sources=.''')
            javaOpts()
            jdk('(Inherit From Job)')
            task()
        }
    }
    // post build actions
    publishers {
        downstream('Cartridge_NexusBackup_Activity', 'SUCCESS')
    }
}

buildNexusBackupJob.with {
    // build environment
    wrappers {
        preBuildCleanup()
    }
    // build
    steps {
        copyArtifacts('Cartridge_BuildAndPackage_Activity') {
            buildSelector {
            latestSuccessful(true)
                }
            includePatterns('target/*.war')
            fingerprintArtifacts(true)
        }
        nexusArtifactUploader {
            nexusVersion('NEXUS2')
            protocol('HTTP')
            nexusUrl('nexus:8081/nexus')
            credentialsId('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            groupId('DTSActivity')
            version('1')
            repository('snapshots')
            artifact {
                artifactId('CurrencyConverter')
                type('war')
                classifier('')
                file('/var/jenkins_home/jobs/DSL_PROJECT/jobs/Cartridge_NexusBackup_Activity/workspace/target/CurrencyConverter.war')
            }
        }
    }
    // post build actions
    publishers {
        archiveArtifacts {
            pattern('**/*.war')
            onlyIfSuccessful()
        }
        downstream('Cartridge_AnsibleBuild_Activity', 'SUCCESS')
    } 
}

buildAnsibleBuildJob.with {
    // general
    properties {
        label("ansible")
    }
    // source code management  
    scm {
        git {
            remote {
                url('https://github.com/piajobelle/Ansible.git')
                credentials('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            }
        }
    } 
    // build environment
    wrappers {
        preBuildCleanup()
        sshAgent('adop-jenkins-master')
        credentialsBinding{
            usernamePassword('username', 'password', '1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
        }
    }
    // build
    steps {
        shell('''ansible-playbook -i ${WORKSPACE}/hosts ${WORKSPACE}/master.yml -u ec2-user -e "image_version=$BUILD_NUMBER username=$username password=$password"''')
    }
    // post build actions
    publishers {
        downstream('Cartridge_SeleniumAnalysis_Activity', 'SUCCESS')
    } 
}

buildSeleniumCodeAnalysisJob.with {
    // source code management  
    scm {
        git {
            remote {
                url('https://github.com/piajobelle/SeleniumDTS.git')
                credentials('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            }
        }
    } 
    // build environment
    wrappers {
        preBuildCleanup()
    }
    // build
    steps {
        maven{
            mavenInstallation('ADOP Maven')
            goals('test')
        }
    }
    // post build actions
    publishers {
        downstream('Cartridge_ReleaseProject_Activity', 'SUCCESS')
    } 
}

buildNexusReleaseJob.with {
    // general
    properties {
        copyArtifactPermissionProperty {
        projectNames('Cartridge_NexusBackup_Activity')
        }
    }  
    // build environment
    wrappers {
        preBuildCleanup()
    }

    // build
    steps {
        copyArtifacts('Cartridge_NexusBackup_Activity') {
            buildSelector {
            latestSuccessful(true)
                }
            includePatterns('**/*.war')
            fingerprintArtifacts(true)
        }
        nexusArtifactUploader {
            nexusVersion('NEXUS2')
            protocol('HTTP')
            nexusUrl('nexus:8081/nexus')
            credentialsId('1a3ac5c7-f7ae-4bbc-827c-191c65e959ec')
            groupId('DTSActivity')
            version('${BUILD_NUMBER}')
            repository('releases')
            artifact {
                artifactId('CurrencyConverter')
                type('war')
                classifier('')
                file('/var/jenkins_home/jobs/DSL_PROJECT/jobs/Cartridge_ReleaseProject_Activity/workspace/target/CurrencyConverter.war')
            }
        }
    }
}
