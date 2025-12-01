package viaduct.devserve

import viaduct.service.api.Viaduct

/**
 * Interface for providing a Viaduct instance to DevServe.
 *
 * Implementations should start their DI framework (e.g., Micronaut)
 * and return the Viaduct bean from the DI context.
 *
 * This approach is simpler than implementing a factory because:
 * - The Viaduct is already configured through your DI framework
 * - No need to duplicate configuration between production and devserve
 * - Just pull the same Viaduct bean that production uses
 *
 * Example with Micronaut:
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
 * @see ViaductDevServeConfiguration
 */
interface ViaductDevServeProvider {
    /**
     * Returns the Viaduct instance to be used by DevServe.
     *
     * This method is called once during DevServe startup and after each hot-reload.
     * The implementation should start the DI context and return the Viaduct bean.
     *
     * @return The Viaduct instance configured through your DI framework
     */
    fun getViaduct(): Viaduct
}
