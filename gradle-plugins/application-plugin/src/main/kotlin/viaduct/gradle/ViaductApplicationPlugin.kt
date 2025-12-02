package viaduct.gradle

import centralSchemaDirectory
import grtClassesDirectory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import viaduct.gradle.ViaductPluginCommon.addViaductDependencies
import viaduct.gradle.ViaductPluginCommon.addViaductTestDependencies
import viaduct.gradle.ViaductPluginCommon.addViaductTestFixtures
import viaduct.gradle.ViaductPluginCommon.applyViaductBOM
import viaduct.gradle.ViaductPluginCommon.configureIdeaIntegration
import viaduct.gradle.task.AssembleCentralSchemaTask
import viaduct.gradle.task.GenerateGRTClassFilesTask
import java.io.File
import javax.inject.Inject

abstract class ViaductApplicationPlugin @Inject constructor(
    private val execOperations: ExecOperations
) : Plugin<Project> {
    override fun apply(project: Project): Unit =
        with(project) {
            require(this == rootProject) {
                "Apply 'com.airbnb.viaduct.application-gradle-plugin' only to the root project."
            }

            val appExt = extensions.create("viaductApplication", ViaductApplicationExtension::class.java, objects)

            // Set default BOM version to plugin version
            appExt.bomVersion.convention(ViaductPluginCommon.BOM.getDefaultVersion())

            val assembleCentralSchemaTask = setupAssembleCentralSchemaTask()
            setupOutgoingConfigurationForCentralSchema(assembleCentralSchemaTask)

            val generateGRTsTask = setupGenerateGRTsTask(appExt, assembleCentralSchemaTask)

            plugins.withId("java") {
                if (appExt.applyBOM.get()) {
                    applyViaductBOM(appExt.bomVersion.get())
                    addViaductDependencies(appExt.viaductDependencies.get())
                    addViaductTestDependencies(appExt.viaductTestDependencies.get())
                    addViaductTestFixtures(appExt.viaductTestFixtures.get())
                }
            }
            configureIdeaIntegration(generateGRTsTask)
            setupConsumableConfigurationForGRT(generateGRTsTask.flatMap { it.archiveFile })

            this.dependencies.add("api", files(generateGRTsTask.flatMap { it.archiveFile }))

            // Setup devserve task
            setupDevServeTask(generateGRTsTask)
        }

    private fun Project.setupAssembleCentralSchemaTask(): TaskProvider<AssembleCentralSchemaTask> {
        val allPartitions = configurations.create(ViaductPluginCommon.Configs.ALL_SCHEMA_PARTITIONS_INCOMING).apply {
            description = "Resolvable configuration where all viaduct-module plugins send their schema partitions."
            isCanBeConsumed = false
            isCanBeResolved = true
            attributes { attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.SCHEMA_PARTITION) }
        }

        val assembleCentralSchemaTask = tasks.register<AssembleCentralSchemaTask>("assembleViaductCentralSchema") {
            schemaPartitions.setFrom(allPartitions.incoming.artifactView {}.files)

            val baseSchemaDir = project.file("src/main/viaduct/schemabase")
            if (baseSchemaDir.exists()) {
                baseSchemaFiles.setFrom(
                    project.fileTree(baseSchemaDir) {
                        include("**/*.graphqls")
                    }
                )
            }

            outputDirectory.set(centralSchemaDirectory())
        }

        return assembleCentralSchemaTask
    }

    /** Call the bytecode-generator to generate GRT files. */
    private fun Project.setupGenerateGRTsTask(
        appExt: ViaductApplicationExtension,
        assembleCentralSchemaTask: TaskProvider<AssembleCentralSchemaTask>,
    ): TaskProvider<Jar> {
        val pluginClasspath = files(ViaductPluginCommon.getClassPathElements(this@ViaductApplicationPlugin::class.java))

        val generateGRTClassesTask = tasks.register<GenerateGRTClassFilesTask>("generateViaductGRTClassFiles") {
            grtClassesDirectory.set(grtClassesDirectory())
            schemaFiles.setFrom(assembleCentralSchemaTask.flatMap { it.outputDirectory.map { dir -> dir.asFileTree.matching { include("**/*.graphqls") }.files } })
            grtPackageName.set(appExt.grtPackageName)
            classpath.setFrom(pluginClasspath)
            mainClass.set(CODEGEN_MAIN_CLASS)
        }

        val generateGRTsTask = tasks.register<Jar>("generateViaductGRTs") {
            group = "viaduct"
            description = "Package GRT class files with the central schema."

            archiveBaseName.set("viaduct-grt")
            includeEmptyDirs = false

            from(generateGRTClassesTask.flatMap { it.grtClassesDirectory })

            from(assembleCentralSchemaTask.flatMap { it.outputDirectory }) {
                into("viaduct/centralSchema")
                exclude(BUILTIN_SCHEMA_FILE)
                includeEmptyDirs = false
            }
        }

        return generateGRTsTask
    }

    private fun Project.setupOutgoingConfigurationForCentralSchema(assembleCentralSchemaTask: TaskProvider<AssembleCentralSchemaTask>) {
        configurations.create(ViaductPluginCommon.Configs.CENTRAL_SCHEMA_OUTGOING).apply {
            description = """
              Consumable configuration consisting of a directory containing all schema fragments.  This directory
              is organized as a top-level file named $BUILTIN_SCHEMA_FILE, plus directories named "parition[/module-name]/graphql",
              where module-name is the modulePackageSuffix of the module with dots replaced by slashes (this segment is
              not present if the suffix is blank).
            """.trimIndent()
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes { attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.CENTRAL_SCHEMA) }
            outgoing.artifact(assembleCentralSchemaTask)
        }
    }

    private fun Project.setupConsumableConfigurationForGRT(artifact: Provider<RegularFile>) {
        configurations.create(ViaductPluginCommon.Configs.GRT_CLASSES_OUTGOING).apply {
            description =
                "Consumable configuration for the jar file containing the GRT classes plus the central schema's graphqls file."
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(ViaductPluginCommon.VIADUCT_KIND, ViaductPluginCommon.Kind.GRT_CLASSES)
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, LibraryElements.JAR)
                )
            }
            outgoing.artifact(artifact)
        }
    }

    private fun Project.setupDevServeTask(generateGRTsTask: TaskProvider<Jar>) {
        // Create configuration at configuration time (not execution time) so dependency substitution works
        val devserveConfig = configurations.create("devserveRuntime") {
            isCanBeConsumed = false
            isCanBeResolved = true
            isVisible = false
            // Add attributes for proper runtime classpath resolution with composite builds
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    objects.named(LibraryElements::class.java, LibraryElements.JAR)
                )
            }
        }

        // Add devserve dependency after evaluation
        afterEvaluate {
            // Gradle automatically substitutes with local :devserve project in composite builds.
            // External standalone projects will resolve from Maven Central.
            val version = ViaductPluginCommon.BOM.getDefaultVersion()
            dependencies.add(devserveConfig.name, "com.airbnb.viaduct:devserve:$version")

            // Also add devserve as compileOnly so the provider class can be compiled with the annotation
            dependencies.add("compileOnly", "com.airbnb.viaduct:devserve:$version")
        }

        // PID file location for persisting server PID across task executions
        val pidFile = layout.buildDirectory.file("devserve.pid").get().asFile

        tasks.register("devserve") {
            group = "viaduct"
            description = "Start the Viaduct development server (use with --continuous for hot-reloading)"

            // Mark as not compatible with configuration cache since it needs project access at execution time
            notCompatibleWithConfigurationCache("devserve task requires project access at execution time")

            // Ensure GRTs are generated and classes are compiled before starting
            dependsOn(generateGRTsTask)
            dependsOn("classes")

            doLast {
                // Get the runtime classpath and main classes output
                val runtimeClasspath = configurations.getByName("runtimeClasspath")
                val mainOutput = project.extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
                    .getByName("main").output

                // Build full classpath - app classes first for proper ClassLoader hierarchy
                val appClasspath = mainOutput.files + runtimeClasspath.files
                val fullClasspath = devserveConfig.files + appClasspath

                val port = project.findProperty("devserve.port")?.toString() ?: "8080"
                val host = project.findProperty("devserve.host")?.toString() ?: "0.0.0.0"

                // In continuous mode, use hot-reload via SIGHUP
                if (gradle.startParameter.isContinuous) {
                    // Check if we have a running server from a previous execution
                    val existingPid = if (pidFile.exists()) {
                        val pid = pidFile.readText().trim().toLongOrNull()
                        if (pid != null && isProcessRunning(pid)) pid else null
                    } else null

                    if (existingPid != null) {
                        // Server already running - send SIGHUP to trigger hot-reload
                        logger.lifecycle("Sending reload signal to devserve (PID: $existingPid)...")
                        try {
                            val killProcess = ProcessBuilder("kill", "-HUP", existingPid.toString())
                                .inheritIO()
                                .start()
                            val exitCode = killProcess.waitFor()
                            if (exitCode == 0) {
                                logger.lifecycle("Reload signal sent successfully. Check server logs for reload status.")
                            } else {
                                logger.warn("Failed to send reload signal (exit code: $exitCode). Server may have stopped.")
                                pidFile.delete()
                            }
                        } catch (e: Exception) {
                            logger.warn("Failed to send reload signal: ${e.message}")
                            pidFile.delete()
                        }
                    } else {
                        // First run - start the server process
                        logger.lifecycle("Starting devserve server with hot-reload support...")

                        val javaHome = System.getProperty("java.home")
                        val javaExec = "$javaHome/bin/java"
                        val classpathString = fullClasspath.joinToString(File.pathSeparator) { it.absolutePath }
                        val appClasspathString = appClasspath.joinToString(File.pathSeparator) { it.absolutePath }

                        val command = listOf(
                            javaExec,
                            "-cp", classpathString,
                            "-Ddevserve.port=$port",
                            "-Ddevserve.host=$host",
                            "-Ddevserve.classpath=$appClasspathString",
                            "viaduct.devserve.DevServeServerKt"
                        )

                        val serverProcess = ProcessBuilder(command)
                            .inheritIO()
                            .start()

                        val serverPid = serverProcess.pid()

                        // Write PID to file for future task executions
                        pidFile.parentFile.mkdirs()
                        pidFile.writeText(serverPid.toString())

                        // Give server time to start
                        Thread.sleep(3000)

                        if (!serverProcess.isAlive) {
                            pidFile.delete()
                            throw org.gradle.api.GradleException("DevServe server failed to start")
                        }

                        logger.lifecycle("DevServe server started (PID: $serverPid)")
                        logger.lifecycle("Hot-reload enabled - changes will be automatically reloaded")
                        logger.lifecycle("GraphQL endpoint: http://$host:$port/graphql")
                    }
                } else {
                    // Not in continuous mode - run directly and wait for completion
                    logger.lifecycle("Starting devserve server...")
                    logger.lifecycle("Tip: Run with --continuous for hot-reload support")
                    execOperations.javaexec {
                        classpath = files(fullClasspath)
                        mainClass.set("viaduct.devserve.DevServeServerKt")
                        systemProperty("devserve.port", port)
                        systemProperty("devserve.host", host)
                        systemProperty("devserve.classpath", appClasspath.joinToString(File.pathSeparator) { it.absolutePath })
                    }
                }
            }
        }
    }

    /** Check if a process with the given PID is still running. */
    private fun isProcessRunning(pid: Long): Boolean {
        return try {
            val process = ProcessBuilder("kill", "-0", pid.toString())
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val CODEGEN_MAIN_CLASS = "viaduct.tenant.codegen.cli.SchemaObjectsBytecode\$Main"
        const val BUILTIN_SCHEMA_FILE = "BUILTIN_SCHEMA.graphqls"
    }
}
