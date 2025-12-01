package com.example.starwars.service.viaduct

import viaduct.service.BasicViaductFactory
import viaduct.service.SchemaRegistrationInfo
import viaduct.service.TenantRegistrationInfo
import viaduct.service.api.Viaduct
import viaduct.service.api.spi.TenantCodeInjector
import viaduct.service.toSchemaScopeInfo

/**
 * Factory for creating Viaduct instances for the StarWars demo application.
 *
 * Used by ViaductConfiguration to create the Viaduct bean for production use.
 * For devserve, use StarWarsDevServeProvider instead.
 *
 * @param tenantCodeInjector The dependency injection provider for tenant code.
 */
class StarWarsViaductFactory(
    private val tenantCodeInjector: TenantCodeInjector
) {
    fun createViaduct(): Viaduct {
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
