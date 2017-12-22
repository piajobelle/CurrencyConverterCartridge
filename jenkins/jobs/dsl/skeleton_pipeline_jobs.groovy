
// Folders
//def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Jobs
def buildMavenJob = mavenJob(projectFolderName + "/Cartridge_Build_CurrencyConverter_Maven")
def buildSonarJob = freeStyleJob(projectFolderName + "/Cartridge_Scan_CurrencyConverter_Sonarqube")
def buildNexusSnapshotsJob = freeStyleJob(projectFolderName + "/Cartridge_CurrencyConverter_Nexus_Snapshots")
def buildAnsibleJob = freeStyleJob(projectFolderName + "/Cartridge_Deploy_CurrencyConverter_Ansible")
def buildSeleniumJob = freeStyleJob(projectFolderName + "/Cartridge_Test_CurrencyConverter_Selenium")
def buildNexusReleasesJob = freeStyleJob(projectFolderName + "/Cartridge_CurrencyConverter_Nexus_Releases")

// Views
def pipelineView = buildPipelineView(projectFolderName + "/Cartridge_CurrencyConverter_Pipeline")

pipelineView.with{
    title('Cartridge_CurrencyConverter_Pipeline')
    displayedBuilds(10)
    selectedJob(projectFolderName + "/Cartridge_Build_CurrencyConverter_Maven")
    showPipelineParameters()
    showPipelineDefinitionHeader()
    refreshFrequency(5)
}


buildMavenJob.with{

  properties {
    copyArtifactPermissionProperty {
      projectNames('Cartridge_CurrencyConverter_Nexus_Snapshot')
    } 
  }

  scm {
    git {           
      remote {
        credentials('adopteam2id')
        url('http://gitlab/gitlab/CurrencyConverter/CurrencyConverter_Project.git')
        }
      branch('*/master')
    }
  }

  wrappers {
    preBuildCleanup()
  }

  triggers {
    gitlabPush {
      buildOnMergeRequestEvents(true)
      buildOnPushEvents(true)
      enableCiSkip(true)
      setBuildDescription(false)
      rebuildOpenMergeRequest('never')
    }
  
    goals('package')

    publishers {
      archiveArtifacts('**/*.war')
      downstream('Cartridge_Scan_CurrencyConverter_Sonarqube','SUCCESS') 
    }
  }
}

buildSonarJob.with{

  scm {
    git {
      remote {
        credentials('adopteam2id')
        url('http://gitlab/gitlab/CurrencyConverter/CurrencyConverter_Project.git')
      }
      branch('*/master')
    }
  }
  
  configure { project ->
    project / 'builders' / 'hudson.plugins.sonar.SonarRunnerBuilder' {
            properties('''sonar.projectKey=team2projkey
            sonar.projectName=Currency_Converter
            sonar.projectVersion=1
            sonar.sources=.''')
            javaOpts()
            jdk('(Inherit From Job)')
            task()
    }
  }
  
  publishers {
    downstream('Cartridge_CurrencyConverter_Nexus_Snapshots','SUCCESS') 
  }

}
  
buildNexusSnapshotsJob.with {
  
  properties {
    copyArtifactPermissionProperty {
      projectNames('Cartridge_CurrencyConverter_Nexus_Releases')
    }
  }

  steps {
    copyArtifacts('Cartridge_Build_CurrencyConverter_Maven') {
      includePatterns('target/*.war')
      buildSelector {
        latestSuccessful(true)
      }
      fingerprintArtifacts(true)
    }
     
    nexusArtifactUploader {
      nexusVersion('nexus2')
      protocol('HTTP')
      nexusUrl('nexus:8081/nexus')
      groupId('TeamTwoCurrencyConverter')
      version('1')
      repository('snapshots')
      credentialsId('adopteam2id')
      artifact {
        artifactId('CurrencyConverter')
        type('war')
        file('target/CurrencyConverter.war')
      }
    }
  }

  publishers {
    archiveArtifacts('**/*.war')
    downstream('Cartridge_Deploy_CurrencyConverter_Ansible','SUCCESS') 
  }
}
