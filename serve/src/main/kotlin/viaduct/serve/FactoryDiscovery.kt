package viaduct.serve

import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

/**
 * Discovers ViaductServerProvider implementations annotated with @ViaductServerConfiguration
 * using classpath scanning.
 */
object FactoryDiscovery {

    private val logger = LoggerFactory.getLogger(FactoryDiscovery::class.java)

    /**
     * Scans the classpath to find a class annotated with @ViaductServerConfiguration
     * that implements ViaductServerProvider.
     *
     * If no provider is found, falls back to DefaultViaductFactory which uses
     * classpath scanning to discover @Resolver annotated classes with no-arg constructors.
     *
     * @return An instance of the discovered provider, or DefaultViaductFactory if none found
     * @throws IllegalStateException if multiple providers are found
     */
    fun discoverProvider(): ViaductServerProvider {
        val providers = findProviderClasses()

        return when (providers.size) {
            0 -> {
                logger.info("No @ViaductServerConfiguration found, using default classpath scanning")
                DefaultViaductFactory()
            }
            1 -> providers.first()
            else -> throw IllegalStateException(
                "Multiple classes found with @ViaductServerConfiguration annotation: ${providers.map { it::class.qualifiedName }}. " +
                "Only one provider should be annotated with @ViaductServerConfiguration per application."
            )
        }
    }

    /**
     * Finds all classes annotated with @ViaductServerConfiguration that implement ViaductServerProvider.
     *
     * @return List of instantiated provider instances
     */
    private fun findProviderClasses(): List<ViaductServerProvider> {
        val annotationName = ViaductServerConfiguration::class.java.name

        return ClassGraph()
            .enableAnnotationInfo()
            .enableClassInfo()
            .scan()
            .use { scanResult ->
                scanResult
                    .getClassesWithAnnotation(annotationName)
                    .mapNotNull { classInfo ->
                        try {
                            val loadedClass = classInfo.loadClass()

                            // Verify it implements ViaductServerProvider
                            if (!ViaductServerProvider::class.java.isAssignableFrom(loadedClass)) {
                                logger.warn(
                                    "Skipping ${classInfo.name}: annotated with @ViaductServerConfiguration " +
                                    "but does not implement ViaductServerProvider"
                                )
                                return@mapNotNull null
                            }

                            // Verify it has a no-arg constructor
                            try {
                                loadedClass.getDeclaredConstructor()
                            } catch (e: NoSuchMethodException) {
                                logger.warn(
                                    "Skipping ${classInfo.name}: no no-argument constructor found. " +
                                    "ViaductServerProvider implementations must have a no-argument constructor."
                                )
                                return@mapNotNull null
                            }

                            // Create instance
                            val kClass = loadedClass.kotlin
                            @Suppress("UNCHECKED_CAST")
                            kClass.createInstance() as ViaductServerProvider
                        } catch (e: Exception) {
                            logger.error("Failed to instantiate provider class ${classInfo.name}", e)
                            null
                        }
                    }
            }
    }
}
