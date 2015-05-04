package org.ysb33r.gradle.gradletest.internal

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.ysb33r.gradle.gradletest.Names
import spock.lang.Specification


/**
 * @author Schalk W. Cronjé
 */
class InfrastructureIntegrationSpec extends Specification {
    static final File simpleTestSrcDir = new File(IntegrationTestHelper.PROJECTROOT,'build/resources/integrationTest/gradleTest')
    static final File repoTestFile = new File(System.getProperty('GRADLETESTREPO'))
    static final File gradleLocation  = IntegrationTestHelper.CURRENT_GRADLEHOME

    Project project = IntegrationTestHelper.buildProject('iis')
    String gradleVersion = project.gradle.gradleVersion

    File simpleTestDestDir = new File(project.projectDir,'src/' + Names.DEFAULT_TASK)
    File expectedOutputDir = new File(project.buildDir,Names.DEFAULT_TASK + '/' + project.gradle.gradleVersion )
    File initScript = new File(project.projectDir,'foo.gradle')

    boolean exists( final String path ) {
        new File("${project.buildDir}/${Names.DEFAULT_TASK}/${path}" ).exists()
    }

    void setup() {
        assert  simpleTestSrcDir.exists()
        assert repoTestFile.exists()

        FileUtils.copyDirectory simpleTestSrcDir, simpleTestDestDir

        project.repositories {
            flatDir {
                dirs repoTestFile.parentFile
            }
        }

        project.allprojects {
            apply plugin: 'org.ysb33r.gradletest'

            // Restrict the test to no downloading
            gradleLocations {
                searchGradleUserHome = false
                searchGvm = false
                download = false
            }

            dependencies {
                gradleTest 'commons-cli:commons-cli:1.2'
            }
//            // Only use the current gradle version for testing
//            gradleTest {
//                versions gradle.gradleVersion
//            }

        }

        initScript.text = '// Nothing here'

    }

    def "Creating an infrastructure for compatibility testing"() {
        when:
        Map<String,File> locations = [:]
        locations[gradleVersion] = gradleLocation
        def runners = Infrastructure.create (
            project : project,
            tests : ['SimpleTest'],
            locations : locations,
            name : Names.DEFAULT_TASK,
            sourceDir : simpleTestDestDir,
            initScript : initScript.toURI(),
            versions : [ gradleVersion ]
        )

        then: "These files must be created"
        exists 'init.gradle'
        exists "${gradleVersion}/SimpleTest/build.gradle"
        exists 'repo'

        and: 'Runners chould be created'
        runners.size() == 1
        runners[0].project == project
        runners[0].gradleLocationDir == gradleLocation
        runners[0].testProjectDir == new File(project.buildDir,Names.DEFAULT_TASK + '/' + gradleVersion + '/' + 'SimpleTest')
        runners[0].initScript == new File("${project.buildDir}/${Names.DEFAULT_TASK}/init.gradle")
        runners[0].version == gradleVersion
        runners[0].testName == 'SimpleTest'
    }
}

/*

    Project project
    File gradleLocationDir
    File testProjectDir
    File initScript
    String version
    String testName

            tests.each { test ->
                testRunners+= new TestRunner(
                    project : project,
                    gradleLocationDir : locations[ver],
                    testProjectDir : new File(dest,test),
                    testName : test,
                    version : ver,
                    initScript : initGradle
                )
            }
        }

        project.copy {
            from project.configurations.getByName(name).files
            into repo
        }.assertNormalExitValue()

 */