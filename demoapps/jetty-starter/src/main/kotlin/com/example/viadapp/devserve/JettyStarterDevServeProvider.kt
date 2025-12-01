package com.example.viadapp.devserve

import com.example.viadapp.injector.ViaductConfiguration
import viaduct.devserve.ViaductDevServeConfiguration
import viaduct.devserve.ViaductDevServeProvider
import viaduct.service.api.Viaduct

/**
 * DevServe provider for the Jetty Starter application.
 *
 * Uses the application's ViaductConfiguration singleton to get the Viaduct instance,
 * ensuring consistent configuration with the main application.
 */
@ViaductDevServeConfiguration
class JettyStarterDevServeProvider : ViaductDevServeProvider {
    override fun getViaduct(): Viaduct {
        return ViaductConfiguration.viaductService
    }
}
