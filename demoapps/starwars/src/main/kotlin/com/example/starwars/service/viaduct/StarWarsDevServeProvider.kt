package com.example.starwars.service.viaduct

import io.micronaut.context.ApplicationContext
import viaduct.devserve.ViaductDevServeConfiguration
import viaduct.devserve.ViaductDevServeProvider
import viaduct.service.api.Viaduct

/**
 * DevServe provider for the StarWars application.
 *
 * Simply starts Micronaut and pulls the Viaduct bean from the DI context.
 * The Viaduct is already configured through ViaductConfiguration via Micronaut's @Factory.
 */
@ViaductDevServeConfiguration
class StarWarsDevServeProvider : ViaductDevServeProvider {
    override fun getViaduct(): Viaduct {
        val context = ApplicationContext.run()
        return context.getBean(Viaduct::class.java)
    }
}
