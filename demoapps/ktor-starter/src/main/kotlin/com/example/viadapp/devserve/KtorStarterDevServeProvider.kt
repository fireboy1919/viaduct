package com.example.viadapp.devserve

import com.example.viadapp.injector.ViaductConfiguration
import viaduct.devserve.ViaductDevServeConfiguration
import viaduct.devserve.ViaductDevServeProvider
import viaduct.service.api.Viaduct

/**
 * DevServe provider for the Ktor Starter application.
 *
 * Uses the application's ViaductConfiguration singleton to get the Viaduct instance,
 * ensuring consistent configuration with the main application.
 */
@ViaductDevServeConfiguration
class KtorStarterDevServeProvider : ViaductDevServeProvider {
    override fun getViaduct(): Viaduct {
        // Use the same ViaductConfiguration as the main app
        return ViaductConfiguration.viaductService
    }
}
