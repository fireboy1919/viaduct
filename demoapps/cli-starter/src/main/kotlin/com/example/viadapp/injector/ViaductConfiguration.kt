package com.example.viadapp.injector

import viaduct.service.BasicViaductFactory
import viaduct.service.TenantRegistrationInfo
import viaduct.service.api.Viaduct

/**
 * Shared Viaduct configuration for the CLI Starter application.
 * Used by both the main application and DevServe.
 */
object ViaductConfiguration {
    val viaductService: Viaduct by lazy {
        BasicViaductFactory.create(
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = "com.example.viadapp"
            )
        )
    }
}
