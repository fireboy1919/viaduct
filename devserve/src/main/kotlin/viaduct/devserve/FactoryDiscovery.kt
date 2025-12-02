package viaduct.devserve

import io.github.classgraph.ClassGraph
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

/**
 * Discovers ViaductDevServeProvider implementations annotated with @ViaductDevServeConfiguration
 * using classpath scanning.
 */
object FactoryDiscovery {

    private val logger = LoggerFactory.getLogger(FactoryDiscovery::class.java)

    /**
     * Scans the classpath to find a class annotated with @ViaductDevServeConfiguration
     * that implements ViaductDevServeProvider.
     *
     * @return An instance of the discovered provider
     * @throws IllegalStateException if no provider is found or multiple providers are found
     */
    fun discoverProvider(): ViaductDevServeProvider {
        val providers = findProviderClasses()

        return when (providers.size) {
            0 -> throw IllegalStateException(
                "No class found with @ViaductDevServeConfiguration annotation. " +
                "Please create a class that implements ViaductDevServeProvider and annotate it with @ViaductDevServeConfiguration."
            )
            1 -> providers.first()
            else -> throw IllegalStateException(
                "Multiple classes found with @ViaductDevServeConfiguration annotation: ${providers.map { it::class.qualifiedName }}. " +
                "Only one provider should be annotated with @ViaductDevServeConfiguration per application."
            )
        }
    }

    /**
     * Finds all classes annotated with @ViaductDevServeConfiguration that implement ViaductDevServeProvider.
     *
     * @return List of instantiated provider instances
     */
    private fun findProviderClasses(): List<ViaductDevServeProvider> {
        val annotationName = ViaductDevServeConfiguration::class.java.name

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

                            // Verify it implements ViaductDevServeProvider
                            if (!ViaductDevServeProvider::class.java.isAssignableFrom(loadedClass)) {
                                logger.warn(
                                    "Skipping ${classInfo.name}: annotated with @ViaductDevServeConfiguration " +
                                    "but does not implement ViaductDevServeProvider"
                                )
                                return@mapNotNull null
                            }

                            // Verify it has a no-arg constructor
                            try {
                                loadedClass.getDeclaredConstructor()
                            } catch (e: NoSuchMethodException) {
                                logger.warn(
                                    "Skipping ${classInfo.name}: no no-argument constructor found. " +
                                    "ViaductDevServeProvider implementations must have a no-argument constructor."
                                )
                                return@mapNotNull null
                            }

                            // Create instance
                            val kClass = loadedClass.kotlin
                            @Suppress("UNCHECKED_CAST")
                            kClass.createInstance() as ViaductDevServeProvider
                        } catch (e: Exception) {
                            logger.error("Failed to instantiate provider class ${classInfo.name}", e)
                            null
                        }
                    }
            }
    }
}
