package org.ysb33r.gradle.gradletest.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * @author Schalk W. Cronjé
 */
@CompileStatic
class IntegrationTestHelper {
    static final String TESTROOT = new File( System.getProperty('TESTROOT') ?: 'build/tmp/integrationtestroot').absoluteFile
    static final String PROJECTROOT = new File( System.getProperty('PROJECTROOT') ?: '.' ).absoluteFile
    static final boolean OFFLINE = System.getProperty('OFFLINE')
    static final File CURRENT_GRADLEHOME = new File( System.getProperty('CURRENT_GRADLEHOME') )
    static final File TESTDIST = new File( System.getProperty('GRADLETESTDIST')  )

    static Project buildProject(final String testName) {
        File pDir = new File(TESTROOT,testName)
        if(pDir.exists()) {
            pDir.deleteDir()
        }
        pDir.mkdirs()
        ProjectBuilder.builder().withName(testName).withProjectDir(pDir).build()
    }

    static File findInstalledVersionDir( final File startDir,final String version ) {
        File found = null
        startDir.eachDir { File intermediateDir ->
            intermediateDir.eachDirMatch ~/gradle-${version}/, { installDir ->
                found = installDir
            }
        }
        found
    }

    @CompileDynamic
    static String versionFromFile(File dist) {
        def matcher = dist.name =~ /gradle-(.+?)(?:-(all|bin))?\.zip/
        assert matcher.matches()
        matcher[0][1]
    }


}