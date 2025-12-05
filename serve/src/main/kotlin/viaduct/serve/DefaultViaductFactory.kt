package viaduct.serve

import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory
import viaduct.api.Resolver
import viaduct.service.BasicViaductFactory
import viaduct.service.TenantRegistrationInfo
import viaduct.service.api.Viaduct

/**
 * Internal default ViaductServerProvider implementation that uses classpath scanning to automatically
 * discover and register resolvers annotated with @Resolver.
 *
 * This factory is used as the fallback when no @ViaductServerConfiguration is found on the classpath.
 * It provides a zero-configuration experience for simple applications:
 * - Scans the classpath for @Resolver annotated classes
 * - Automatically determines the package prefix from discovered resolvers
 * - Uses default (no-argument) constructors to instantiate resolvers
 *
 * Limitations:
 * - Only works with resolvers that have no-argument constructors
 * - Cannot inject dependencies into resolvers
 * - For more complex setups, create a custom ViaductServerProvider
 */
internal class DefaultViaductFactory : ViaductServerProvider {

    private val logger = LoggerFactory.getLogger(DefaultViaductFactory::class.java)

    override fun getViaduct(): Viaduct {
        logger.info("No @ViaductServerConfiguration found. Using default classpath scanning.")
        logger.info("NOTE: Default mode only works with @Resolver classes that have zero-argument constructors.")
        logger.info("For dependency injection or custom configuration, create a class implementing ViaductServerProvider")
        logger.info("and annotate it with @ViaductServerConfiguration.")

        // Discover resolvers
        val resolvers = discoverResolvers()

        if (resolvers.isEmpty()) {
            logger.warn("No @Resolver annotated classes found on classpath!")
            logger.warn("Make sure your resolver classes are:")
            logger.warn("  1. Annotated with @Resolver")
            logger.warn("  2. Have a no-argument constructor")
            logger.warn("  3. Are on the classpath")
        }

        // Determine package prefix from discovered resolvers
        val effectivePackagePrefix = detectPackagePrefix(resolvers)

        logger.info("Using package prefix: $effectivePackagePrefix")
        logger.info("Found ${resolvers.size} resolver(s): ${resolvers.map { it.simpleName }}")

        // Create Viaduct using BasicViaductFactory
        return BasicViaductFactory.create(
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = effectivePackagePrefix
            )
        )
    }

    /**
     * Discovers all classes annotated with @Resolver on the classpath.
     */
    private fun discoverResolvers(): List<Class<*>> {
        val annotationName = Resolver::class.java.name

        logger.debug("Scanning classpath for @Resolver annotated classes...")

        return ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .scan()
            .use { scanResult ->
                scanResult
                    .getClassesWithAnnotation(annotationName)
                    .mapNotNull { classInfo ->
                        try {
                            val clazz = classInfo.loadClass()

                            // Verify it has a no-arg constructor
                            try {
                                clazz.getDeclaredConstructor()
                                logger.debug("Found resolver: ${clazz.name}")
                                clazz
                            } catch (e: NoSuchMethodException) {
                                logger.warn(
                                    "Skipping resolver ${clazz.name}: no no-argument constructor found. " +
                                    "Add a no-argument constructor or use a custom ViaductFactory."
                                )
                                null
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to load resolver class ${classInfo.name}", e)
                            null
                        }
                    }
            }
    }

    /**
     * Detects the common package prefix from discovered resolver classes.
     *
     * Uses the most specific common package prefix among all resolvers.
     * For example, if resolvers are in:
     * - com.example.app.resolvers.QueryResolver
     * - com.example.app.resolvers.MutationResolver
     * - com.example.app.users.UserResolver
     *
     * The detected prefix will be "com.example.app"
     */
    private fun detectPackagePrefix(resolvers: List<Class<*>>): String {
        if (resolvers.isEmpty()) {
            logger.warn("No resolvers found, defaulting to empty package prefix")
            return ""
        }

        // Get all package names
        val packages = resolvers.mapNotNull { it.`package`?.name }

        if (packages.isEmpty()) {
            logger.warn("No package information found for resolvers, defaulting to empty package prefix")
            return ""
        }

        // Find the common prefix
        val commonPrefix = packages.reduce { acc, packageName ->
            commonPrefix(acc, packageName)
        }

        // Trim to the last complete package segment
        val trimmedPrefix = commonPrefix.substringBeforeLast(".", commonPrefix)

        logger.debug("Detected package prefix from ${packages.size} resolver package(s): $trimmedPrefix")

        return trimmedPrefix
    }

    /**
     * Finds the common prefix between two strings (package names).
     */
    private fun commonPrefix(s1: String, s2: String): String {
        val minLength = minOf(s1.length, s2.length)
        var i = 0
        while (i < minLength && s1[i] == s2[i]) {
            i++
        }
        return s1.substring(0, i)
    }
}
