package viaduct.devserve.fixtures

import viaduct.service.BasicViaductFactory
import viaduct.service.TenantRegistrationInfo
import viaduct.service.api.Viaduct
import viaduct.service.api.ViaductConfiguration
import viaduct.service.api.ViaductFactory

/**
 * Test fixture: A valid factory with @ViaductConfiguration annotation.
 * Creates a minimal Viaduct instance for testing purposes.
 * Uses BasicViaductFactory with the test fixtures package prefix.
 */
@ViaductConfiguration
class ValidTestFactory : ViaductFactory {
    override fun createViaduct(): Viaduct {
        // Create a minimal Viaduct using BasicViaductFactory
        // This will discover any @Resolver annotated test resolvers
        return BasicViaductFactory.create(
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = "viaduct.devserve.fixtures"
            )
        )
    }
}

/**
 * Test fixture: Another valid factory without annotation
 * (To test multiple factory detection, this would need to be annotated,
 * but leaving it unannotated allows other tests to pass)
 */
class AnotherValidTestFactory : ViaductFactory {
    override fun createViaduct(): Viaduct {
        throw NotImplementedError("Test factory - should not be called")
    }
}

/**
 * Test fixture: Factory without annotation (should be ignored)
 */
class FactoryWithoutAnnotation : ViaductFactory {
    override fun createViaduct(): Viaduct {
        throw NotImplementedError("Test factory - should not be called")
    }
}

/**
 * Test fixture: Annotated class that doesn't implement ViaductFactory
 */
@ViaductConfiguration
class AnnotatedNonFactory {
    fun doSomething() = "not a factory"
}

/**
 * Test fixture: Factory without no-arg constructor
 */
@ViaductConfiguration
class FactoryWithoutNoArgConstructor(private val param: String) : ViaductFactory {
    override fun createViaduct(): Viaduct {
        throw NotImplementedError("Test factory - should not be called")
    }
}
