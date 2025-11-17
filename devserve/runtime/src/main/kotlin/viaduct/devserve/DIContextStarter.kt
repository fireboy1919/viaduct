package viaduct.devserve

import org.slf4j.LoggerFactory
import viaduct.service.api.Viaduct

/**
 * Attempts to start a DI framework context and obtain a Viaduct bean.
 *
 * This class uses reflection to detect and start various DI frameworks without
 * requiring compile-time dependencies. Currently supports:
 * - Micronaut
 *
 * The devserve framework remains framework-agnostic while still being able to
 * work with applications that use dependency injection.
 */
object DIContextStarter {
    private val logger = LoggerFactory.getLogger(DIContextStarter::class.java)

    /**
     * Attempts to start a DI context and get a Viaduct bean.
     *
     * Tries the following in order:
     * 1. Micronaut ApplicationContext
     *
     * @return Viaduct instance from DI context, or null if no DI framework is available
     */
    fun tryStartAndGetViaduct(): Viaduct? {
        // Try Micronaut first
        tryMicronaut()?.let { return it }

        // Future: Add support for other frameworks (Spring, Guice, etc.)

        return null
    }

    /**
     * Attempts to start Micronaut ApplicationContext and get Viaduct bean.
     * Uses reflection to avoid compile-time dependency on Micronaut.
     *
     * @return Viaduct instance from Micronaut context, or null if Micronaut is not available
     */
    private fun tryMicronaut(): Viaduct? {
        return try {
            logger.info("Checking for Micronaut ApplicationContext...")

            // Check if Micronaut is available on classpath
            val contextClass = Class.forName("io.micronaut.context.ApplicationContext")
            logger.info("Micronaut detected on classpath")

            // Start the ApplicationContext
            logger.info("Starting Micronaut ApplicationContext...")
            val runMethod = contextClass.getMethod("run")
            val context = runMethod.invoke(null) // ApplicationContext.run() is static

            logger.info("Micronaut ApplicationContext started successfully")

            // Get the Viaduct bean from the context
            logger.info("Looking for Viaduct bean in Micronaut context...")
            val getBeanMethod = contextClass.getMethod("getBean", Class::class.java)
            val viaductClass = Class.forName("viaduct.service.api.Viaduct")
            val viaduct = getBeanMethod.invoke(context, viaductClass) as Viaduct

            logger.info("Viaduct bean obtained from Micronaut context")
            viaduct

        } catch (e: ClassNotFoundException) {
            logger.debug("Micronaut not found on classpath")
            null
        } catch (e: NoSuchMethodException) {
            logger.warn("Micronaut found but methods not available", e)
            null
        } catch (e: Exception) {
            logger.warn("Failed to start Micronaut context or get Viaduct bean", e)
            null
        }
    }
}
