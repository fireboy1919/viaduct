package com.example.viadapp.resolvers.di

import com.example.viadapp.resolvers.AsciiArtResolver
import com.example.viadapp.resolvers.service.AsciiArtService
import org.koin.dsl.module

/**
 * Koin module providing resolver dependencies.
 *
 * This module registers services and resolvers. The Viaduct instance
 * is registered separately in [viaductModule] so it can access
 * the Koin instance for dependency injection.
 *
 * Koin is the source of truth for resolver discovery - any class registered
 * here with the @Resolver annotation will be automatically discovered by Viaduct.
 *
 * To add a new resolver:
 * 1. Annotate your resolver class with @Resolver
 * 2. Register it as a factory in this module
 * 3. That's it - Viaduct will discover it automatically from Koin's registry
 */
val resolversModule = module {
    // Services - singleton instances shared across resolvers
    single { AsciiArtService() }

    // Resolvers - factory creates new instance per invocation (per GraphQL field resolution)
    // The @Resolver annotation makes this discoverable by KoinTenantResolverClassFinder
    factory { AsciiArtResolver(get()) }
}
