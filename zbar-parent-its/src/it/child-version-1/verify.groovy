def pom = new File(localRepositoryPath, 'fr/zbar/zbar-parent').listFiles().find { it.isDirectory() }
def installed = new File(pom, "zbar-parent-${pom.name}.pom").text
def dependencies = new groovy.xml.XmlSlurper().parseText(installed).dependencyManagement.dependencies.dependency
assert dependencies.find { it.artifactId == 'zbar-bom' }.version.text() == pom.name
