package viaduct.service.api

/**
 * Marks a [ViaductFactory] implementation as the application's Viaduct configuration.
 *
 * This annotation enables automatic discovery of your factory in development mode:
 * - The `./gradlew devserve` task uses classpath scanning to find the annotated factory
 * - Only one class per application should be annotated with @ViaductConfiguration
 *
 * The annotation can also be used in production environments for consistent factory
 * discovery across different deployment contexts.
 *
 * Example:
 * ```kotlin
 * @ViaductConfiguration
 * class MyViaductFactory : ViaductFactory {
 *     override fun createViaduct(): Viaduct {
 *         return ViaductBuilder()
 *             .withTenantAPIBootstrapperBuilder(myBootstrapper)
 *             .build()
 *     }
 * }
 * ```
 *
 * @see ViaductFactory
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ViaductConfiguration
