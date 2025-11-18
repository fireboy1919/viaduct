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
 * and tenant code injector. It supports two modes:
 *
 * 1. Production mode (via ViaductConfiguration): Micronaut provides the injector
 * 2. DevServe mode (no-arg constructor): Starts Micronaut and gets the injector
 */
@ViaductApplication
class StarWarsViaductFactory : ViaductFactory {
    private val tenantCodeInjector: TenantCodeInjector

    /**
     * No-arg constructor for devserve mode.
     * Starts Micronaut ApplicationContext and obtains the TenantCodeInjector from it.
     */
    constructor() {
        // Start Micronaut ApplicationContext
        val contextClass = Class.forName("io.micronaut.context.ApplicationContext")
        val runMethod = contextClass.getMethod("run")
        val context = runMethod.invoke(null) // ApplicationContext.run() is static

        // Get the MicronautTenantCodeInjector bean from the context
        val getBeanMethod = contextClass.getMethod("getBean", Class::class.java)
        val injectorClass = Class.forName("com.example.starwars.service.viaduct.MicronautTenantCodeInjector")
        tenantCodeInjector = getBeanMethod.invoke(context, injectorClass) as TenantCodeInjector
    }

    /**
     * Constructor for production mode.
     * Used by ViaductConfiguration with Micronaut DI providing the injector.
     *
     * @param tenantCodeInjector The dependency injection provider for tenant code.
     */
    constructor(tenantCodeInjector: TenantCodeInjector) {
        this.tenantCodeInjector = tenantCodeInjector
    }

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
