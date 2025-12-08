package viaduct.serve

import java.io.File

/**
 * Entry point for the Viaduct serve server.
 *
 * This is the main class referenced by the viaduct-application-plugin's serve task.
 * It starts a ViaductServer that:
 * 1. Discovers @ViaductServerConfiguration providers, or falls back to DefaultViaductFactory
 * 2. Serves GraphQL at /graphql
 * 3. Provides GraphiQL IDE at /graphiql
 *
 * System properties:
 * - serve.port: Port to bind to (default: 8080). Use 0 for any available port.
 * - serve.host: Host to bind to (default: 0.0.0.0)
 * - serve.classpath: Additional classpath entries separated by system path separator
 */
fun main() {
    val port = System.getProperty("serve.port", "8080").toIntOrNull() ?: 8080
    val host = System.getProperty("serve.host", "0.0.0.0")
    val classpathStr = System.getProperty("serve.classpath", "")

    val classpath = if (classpathStr.isNotEmpty()) {
        classpathStr.split(File.pathSeparator).map { File(it) }
    } else {
        emptyList()
    }

    ViaductServer(port = port, host = host, classpath = classpath).start()
}
