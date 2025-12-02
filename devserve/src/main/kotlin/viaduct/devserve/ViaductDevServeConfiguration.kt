package viaduct.devserve

/**
 * Marks a class as a DevServe Viaduct provider.
 *
 * The annotated class must implement [ViaductDevServeProvider] and provide
 * a no-argument constructor. DevServe will:
 * 1. Scan the classpath for classes with this annotation
 * 2. Instantiate the annotated class
 * 3. Call [ViaductDevServeProvider.getViaduct] to obtain the Viaduct instance
 *
 * Example:
 * ```kotlin
 * @ViaductDevServeConfiguration
 * class MyDevServeConfig : ViaductDevServeProvider {
 *     override fun getViaduct(): Viaduct {
 *         val context = ApplicationContext.run()
 *         return context.getBean(Viaduct::class.java)
 *     }
 * }
 * ```
 *
 * @see ViaductDevServeProvider
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ViaductDevServeConfiguration
