package com.example.starwars.service.viaduct

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import viaduct.service.api.SchemaId
import viaduct.service.api.Viaduct

const val DEFAULT_SCOPE_ID = "default"
const val EXTRAS_SCOPE_ID = "extras"
val DEFAULT_SCHEMA_ID = SchemaId.Scoped("publicSchema", setOf(DEFAULT_SCOPE_ID))
val EXTRAS_SCHEMA_ID = SchemaId.Scoped("publicSchemaWithExtras", setOf(DEFAULT_SCOPE_ID, EXTRAS_SCOPE_ID))

@Factory
class ViaductConfiguration(
    val micronautTenantCodeInjector: MicronautTenantCodeInjector
) {
    @Bean
    fun providesViaduct(): Viaduct {
        // Use StarWarsViaductFactory to create the Viaduct instance
        // This ensures both production and devserve use the same factory
        val factory = StarWarsViaductFactory(micronautTenantCodeInjector)
        return factory.createViaduct()
    }
}
