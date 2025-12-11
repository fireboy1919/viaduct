package com.example.viadapp.injector

import org.koin.core.Koin
import viaduct.tenant.runtime.bootstrap.TenantResolverClassFinder
import viaduct.tenant.runtime.bootstrap.TenantResolverClassFinderFactory

/**
 * A [TenantResolverClassFinderFactory] that creates finders backed by Koin.
 *
 * This factory passes the Koin instance to [KoinTenantResolverClassFinder],
 * which queries Koin's registry for resolver classes.
 *
 * @param koin The Koin instance to use for resolver discovery
 */
class KoinTenantResolverClassFinderFactory(
    private val koin: Koin
) : TenantResolverClassFinderFactory {

    override fun create(packageName: String): TenantResolverClassFinder {
        return KoinTenantResolverClassFinder(koin)
    }
}
