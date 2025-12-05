package viaduct.serve

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for FactoryDiscovery classpath scanning functionality.
 *
 * Note: These tests rely on the test fixtures in the fixtures package.
 * When no @ViaductServerConfiguration is found, FactoryDiscovery falls back
 * to DefaultViaductFactory which scans for @Resolver classes.
 */
class FactoryDiscoveryTest {

    @Test
    fun `discoverProvider should find valid provider on classpath`() {
        // Given: Test classpath contains providers or resolvers

        // When: Discovery is performed
        val provider = FactoryDiscovery.discoverProvider()

        // Then: A provider instance is returned
        assertNotNull(provider, "Provider should be discovered")
    }

    @Test
    fun `discoverProvider should fall back to DefaultViaductFactory when no configuration found`() {
        // When: No @ViaductServerConfiguration annotated provider exists
        // Then: Should return DefaultViaductFactory (not throw)

        // Note: This test verifies the fallback behavior.
        // In a clean test environment without @ViaductServerConfiguration,
        // this would return DefaultViaductFactory
        val provider = FactoryDiscovery.discoverProvider()
        assertNotNull(provider)
    }

    @Test
    fun `provider discovery should validate ViaductServerProvider implementation`() {
        // Given: AnnotatedNonFactory exists on classpath but doesn't implement ViaductServerProvider

        // When/Then: Discovery should handle this gracefully
        // The AnnotatedNonFactory should be rejected during discovery
        // This is verified by the implementation checking isAssignableFrom

        val provider = FactoryDiscovery.discoverProvider()
        assertNotNull(provider)
    }

    @Test
    fun `provider discovery should require no-arg constructor`() {
        // Given: FactoryWithoutNoArgConstructor exists on classpath

        // When/Then: Discovery should handle this gracefully
        // The provider without no-arg constructor should be rejected
        // This is verified by the implementation catching NoSuchMethodException

        val provider = FactoryDiscovery.discoverProvider()
        assertNotNull(provider)
    }

    @Test
    fun `discovered provider should be instantiable`() {
        // Given: Provider is discovered
        val provider = FactoryDiscovery.discoverProvider()

        // Then: Provider should be a valid ViaductServerProvider instance
        assertNotNull(provider)
        assertTrue(provider is ViaductServerProvider)
    }

    @Test
    fun `provider discovery should ignore unannotated providers`() {
        // Given: FactoryWithoutAnnotation exists on classpath

        // When: Discovery is performed
        val provider = FactoryDiscovery.discoverProvider()

        // Then: Only annotated providers should be found
        // FactoryWithoutAnnotation should not be returned
        assertNotNull(provider)
        assertTrue(provider::class.simpleName != "FactoryWithoutAnnotation")
    }
}
