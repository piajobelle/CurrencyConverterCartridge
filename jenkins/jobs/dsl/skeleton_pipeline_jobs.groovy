
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
