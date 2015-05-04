package org.ysb33r.gradle.gradletest.internal

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.gradle.util.CollectionUtils
import org.ysb33r.gradle.gradletest.Distribution
import org.ysb33r.gradle.gradletest.Names

/** The downloader is a background task which will download Gradle distributions to a local
 * folder in the build directory.
 *
 *  This task is only enabled if the gradle job was not run offline.
 * In the current implementation it will only look for 'bin' variants.
 *
 * @author Schalk W. Cronjé
 */
class GradleTestDownloader extends DefaultTask {

    GradleTestDownloader() {
        enabled = !project.gradle.startParameter.offline
        group = Names.TASK_GROUP
        description = 'Downloads gradle distributions if needed'
    }

    /** Get the list of versions that needs to be downloaded.
     *
     * @return Set of version strings.
     */
    @Input
    Set<String> getVersions() {
        this.versions
    }

    /** Get the output directory.
     * The default is {@code ${buildDir}/gradleDist
     */

    @OutputDirectory
    File getOutputDir() {
        this.outputDir
    }

    // TODO: Decide whether this is needed. Probably not in the first version though.
//    /** Overrides te output directory
//     *
//     * @param dir
//     */
//    void setOutputDir(Object dir) {
//        this.outputDir=project.file(dir)
//    }
//
//    /** Overrides te output directory
//     *
//     * @param dir
//     */
//    void outputDir(Object dir) {
//        setOutputDir(dir)
//    }

    /** Adds one or more versions that need to be downloaded.
     *
     * @param v List of version that needs downloading.
     */
    void versions(Object... v) {
        versions+= CollectionUtils.stringize(v as List)
    }

    /** Adds one or more versions that need to be downloaded.
     *
     * @param v List of version that needs downloading.
     */
    void versions(Iterable<Object> v) {
        versions += CollectionUtils.stringize(v as List)
    }

    /** Returns the URIs this downloader will attempt to use
     *
     */
    @SkipWhenEmpty
    @Input
    Set<URI> getUris() {
        Set<URI> uriSet = []
        if(project.extensions.getByName(Names.EXTENSION).useGradleSite) {
            uriSet += Names.GRADLE_SITE
        }
        uriSet.addAll(project.extensions.getByName(Names.EXTENSION).uris)
        uriSet
    }

    /** Returns a set of downloaded distributions and their locations. This only becomes set after execution
     *
     */
    Set<Distribution> getDownloaded() {
        downloaded
    }

    /** Downloads any necessary distributions and unpacks them.
     * Any exceptions that occur during downloading will stop downloading of other distributions
     *
     *  @throw StopActionException if a requested version has not been found
     */
    @TaskAction
    void exec() {
        Set<URI> uriSet = uris
        if(!uris.empty) {
            outputDir.mkdirs()
            versions.each { String version ->
                File target = new File(outputDir, "gradle-${version}-bin.zip")
                downloadFileFrom(uriSet,target)
                if(!target.exists()) {
                    throw new StopActionException("Could not find Gradle ${version}")
                }
                unpackFile(target)
            }
            populateDownloaded()
        }
    }

    private void downloadFileFrom(final Set<URI> uriSet,final File target) {
        uriSet.each { uri ->
            if (!target.exists()) {
                try {
                    Unpacker.downloadTo(project.gradle, uri, outputDir, version, 'bin')
                } catch (FileNotFoundException) {
                    // Expected exception in case of URI not existent
                }
            }
        }
    }

    private void unpackFile(final File target) {
        Unpacker.unpackTo(new File(outputDir,'gradleDist'),target,logger)
        if(project.extensions.getByName(Names.EXTENSION).downloadToGradleUserHome) {
            Unpacker.unpackToUserHome(project.gradle,target)
        }
    }

    private void populateDownloaded() {
        downloaded = DistributionInternal.searchCacheFolder(outputDir,logger)
    }

    private Set<String> versions = []
    private File outputDir = new File(project.buildDir,Names.DOWNLOAD_FOLDER)
    private List<Distribution> downloaded = null

    /** Updates the versions for a given downloader task. Intended to be called in
     * {@code afterEvaluate} phase.
     *
     * @param task Downloader tasks to be updated
     * @param useTheseVersions List of versions to be downloaded.
     */
    static void updateVersions( GradleTestDownloader task,Iterable<String> useTheseVersions) {
        task.versions useTheseVersions
    }
}