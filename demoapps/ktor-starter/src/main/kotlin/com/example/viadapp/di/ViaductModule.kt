package com.example.viadapp.di

import com.example.viadapp.SCHEMA_ID
import com.example.viadapp.injector.KoinTenantCodeInjector
import org.koin.dsl.module
import viaduct.api.bootstrap.ViaductTenantAPIBootstrapper
import viaduct.service.api.Viaduct
import viaduct.service.runtime.SchemaConfiguration
import viaduct.service.runtime.StandardViaduct

private const val TENANT_PACKAGE_PREFIX = "com.example.viadapp"

/**
 * Koin module that provides the Viaduct instance.
 *
 * The Viaduct is registered as a singleton, configured to use:
 * - [KoinTenantCodeInjector] for resolver instantiation via Koin
 * - Default classpath scanning for resolver discovery
 *
 * Resolvers are discovered via Viaduct's default classpath scanning mechanism,
 * which finds classes annotated with @Resolver. Koin is only used for instantiation,
 * allowing constructor injection of dependencies.
 */
val viaductModule = module {
    single<Viaduct> {
        val koin = getKoin()

        val tenantAPIBootstrapper = ViaductTenantAPIBootstrapper.Builder()
            .tenantPackagePrefix(TENANT_PACKAGE_PREFIX)
            .tenantCodeInjector(KoinTenantCodeInjector(koin))
            .create()

        val schemaConfiguration = SchemaConfiguration.fromResources(
            scopes = setOf(SchemaConfiguration.ScopeConfig(SCHEMA_ID, emptySet()))
        )

        StandardViaduct.Builder()
            .withTenantAPIBootstrapper(tenantAPIBootstrapper)
            .withSchemaConfiguration(schemaConfiguration)
            .build()
    }
}
