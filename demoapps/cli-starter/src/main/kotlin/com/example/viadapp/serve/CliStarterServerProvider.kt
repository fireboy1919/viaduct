package com.example.viadapp.serve

import com.example.viadapp.injector.ViaductConfiguration
import viaduct.serve.ViaductServerConfiguration
import viaduct.serve.ViaductServerProvider
import viaduct.service.api.Viaduct

/**
 * Viaduct Server provider for the CLI Starter application.
 *
 * Uses the application's ViaductConfiguration singleton to get the Viaduct instance,
 * ensuring consistent configuration with the main application.
 */
@ViaductServerConfiguration
class CliStarterServerProvider : ViaductServerProvider {
    override fun getViaduct(): Viaduct {
        return ViaductConfiguration.viaductService
    }
}
