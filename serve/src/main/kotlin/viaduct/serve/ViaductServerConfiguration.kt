package viaduct.serve

/**
 * Marks a class as a Viaduct Server Viaduct provider.
 *
 * The annotated class must implement [ViaductServerProvider] and provide
 * a no-argument constructor. Viaduct Server will:
 * 1. Scan the classpath for classes with this annotation
 * 2. Instantiate the annotated class
 * 3. Call [ViaductServerProvider.getViaduct] to obtain the Viaduct instance
 *
 * Example:
 * ```kotlin
 * @ViaductServerConfiguration
 * class MyViaduct ServerConfig : ViaductServerProvider {
 *     override fun getViaduct(): Viaduct {
 *         val context = ApplicationContext.run()
 *         return context.getBean(Viaduct::class.java)
 *     }
 * }
 * ```
 *
 * @see ViaductServerProvider
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ViaductServerConfiguration
