package com.example.viadapp.injector

import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import org.koin.core.Koin
import viaduct.api.Resolver
import viaduct.api.internal.ObjectBase
import viaduct.api.types.Arguments
import viaduct.tenant.runtime.bootstrap.TenantResolverClassFinder

/**
 * A [TenantResolverClassFinder] that discovers resolver classes from Koin's registry.
 *
 * This makes Koin the source of truth for resolver discovery - any class registered
 * in Koin that has the [@Resolver][Resolver] annotation will be discovered automatically.
 *
 * @param koin The Koin instance to query for resolver classes
 * @param grtPackagePrefix The package prefix for GRT classes (still loaded via classloader)
 */
class KoinTenantResolverClassFinder(
    private val koin: Koin,
    private val grtPackagePrefix: String = "viaduct.api.grts"
) : TenantResolverClassFinder {

    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    override fun resolverClassesInPackage(): Set<Class<*>> {
        // Query Koin for all registered definitions and filter for @Resolver annotated classes
        return koin.rootScope.beanRegistry
            .getAllDefinitions()
            .map { it.primaryType.java }
            .filter { clazz ->
                clazz.kotlin.hasAnnotation<Resolver>()
            }
            .toSet()
    }

    override fun nodeResolverForClassesInPackage(): Set<Class<*>> = emptySet()

    override fun getSubTypesOf(typeName: String): Set<Class<*>> = emptySet()

    override fun grtClassForName(typeName: String): KClass<ObjectBase> {
        @Suppress("UNCHECKED_CAST")
        return classLoader.loadClass("$grtPackagePrefix.$typeName").kotlin as KClass<ObjectBase>
    }

    override fun argumentClassForName(typeName: String): KClass<out Arguments> {
        @Suppress("UNCHECKED_CAST")
        return classLoader.loadClass("$grtPackagePrefix.$typeName").kotlin as KClass<out Arguments>
    }
}
