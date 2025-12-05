package viaduct.serve

import viaduct.service.api.Viaduct

/**
 * Interface for providing a Viaduct instance to Viaduct Server.
 *
 * Implementations should start their DI framework (e.g., Micronaut)
 * and return the Viaduct bean from the DI context.
 *
 * This approach is simpler than implementing a factory because:
 * - The Viaduct is already configured through your DI framework
 * - No need to duplicate configuration between production and serve
 * - Just pull the same Viaduct bean that production uses
 *
 * Example with Micronaut:
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
 * @see ViaductServerConfiguration
 */
interface ViaductServerProvider {
    /**
     * Returns the Viaduct instance to be used by Viaduct Server.
     *
     * This method is called once during Viaduct Server startup and after each hot-reload.
     * The implementation should start the DI context and return the Viaduct bean.
     *
     * @return The Viaduct instance configured through your DI framework
     */
    fun getViaduct(): Viaduct
}
