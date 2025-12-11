package com.example.viadapp.di

import com.example.viadapp.SCHEMA_ID
import com.example.viadapp.injector.KoinTenantCodeInjector
import com.example.viadapp.injector.KoinTenantResolverClassFinderFactory
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
 * - [KoinTenantResolverClassFinderFactory] for resolver discovery from Koin's registry
 *
 * This makes Koin the source of truth for both resolver discovery and instantiation,
 * similar to how Spring manages Viaduct in the starwars demo app.
 *
 * Any class registered in Koin with the @Resolver annotation will be automatically
 * discovered and used by Viaduct.
 */
val viaductModule = module {
    single<Viaduct> {
        val koin = getKoin()

        val tenantAPIBootstrapper = ViaductTenantAPIBootstrapper.Builder()
            .tenantPackagePrefix(TENANT_PACKAGE_PREFIX)
            .tenantCodeInjector(KoinTenantCodeInjector(koin))
            .tenantResolverClassFinderFactory(KoinTenantResolverClassFinderFactory(koin))
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
