package com.example.starwars.service.viaduct

import viaduct.service.BasicViaductFactory
import viaduct.service.SchemaRegistrationInfo
import viaduct.service.TenantRegistrationInfo
import viaduct.service.api.Viaduct
import viaduct.service.api.ViaductApplication
import viaduct.service.api.ViaductFactory
import viaduct.service.api.spi.TenantCodeInjector
import viaduct.service.toSchemaScopeInfo

/**
 * ViaductFactory for the StarWars demo application.
 *
 * This factory creates the Viaduct instance with the appropriate configuration
 * and tenant code injector. It is used by both production (via ViaductConfiguration)
 * and devserve mode.
 *
 * @param tenantCodeInjector The dependency injection provider for tenant code.
 *                           In production, this is MicronautTenantCodeInjector.
 *                           In devserve, this is created from the Micronaut ApplicationContext.
 */
@ViaductApplication
class StarWarsViaductFactory(
    private val tenantCodeInjector: TenantCodeInjector
) : ViaductFactory {

    override fun createViaduct(): Viaduct {
        return BasicViaductFactory.create(
            schemaRegistrationInfo = SchemaRegistrationInfo(
                scopes = listOf(
                    DEFAULT_SCHEMA_ID.toSchemaScopeInfo(),
                    EXTRAS_SCHEMA_ID.toSchemaScopeInfo(),
                ),
                packagePrefix = "com.example.starwars",
                resourcesIncluded = ".*\\.graphqls"
            ),
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = "com.example.starwars",
                tenantCodeInjector = tenantCodeInjector
            )
        )
    }
}
