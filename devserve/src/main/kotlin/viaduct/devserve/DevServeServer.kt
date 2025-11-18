package viaduct.devserve

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import viaduct.service.api.ExecutionInput
import viaduct.service.api.Viaduct

/**
 * Development server for Viaduct applications.
 *
 * Provides:
 * - GraphQL endpoint at POST /graphql
 * - GraphiQL IDE at GET /graphiql
 * - Health check at GET /health
 */
class DevServeServer(
    private val port: Int = 8080,
    private val host: String = "0.0.0.0"
) {
    private val logger = LoggerFactory.getLogger(DevServeServer::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var viaduct: Viaduct
    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null
    var actualPort: Int = 0
        private set

    /**
     * Starts the development server.
     *
     * Discovers and instantiates a ViaductFactory, creates a Viaduct instance,
     * then starts the Ktor server.
     *
     * If port is set to 0, the server will bind to any available port.
     */
    fun start() {
        logger.info("Starting Viaduct Development Server...")

        try {
            // Discover and instantiate ViaductFactory
            logger.info("Discovering ViaductFactory...")
            val factory = FactoryDiscovery.discoverFactory()
            logger.info("Found factory: ${factory::class.qualifiedName}")

            // Create Viaduct instance from factory
            logger.info("Creating Viaduct instance from factory...")
            viaduct = factory.createViaduct()
            logger.info("Viaduct instance created successfully")

            // Capture references for use in server configuration
            val loggerRef = logger
            val mapperRef = objectMapper
            val hostRef = host
            val portRef = port

            // Start the server
            server = embeddedServer(Netty, port = portRef, host = hostRef) {
                configureApplication(viaduct, loggerRef, mapperRef)
            }

            // Start server without blocking initially
            server!!.start(wait = false)

            // Get the actual bound port from the resolved connectors
            actualPort = runBlocking {
                (server!!.engine as io.ktor.server.netty.NettyApplicationEngine).resolvedConnectors().first().port
            }

            if (portRef == 0) {
                loggerRef.info("Viaduct DevServe running on dynamically assigned port: $actualPort")
            } else {
                loggerRef.info("Viaduct DevServe running on port: $actualPort")
            }
            loggerRef.info("Server address: http://$hostRef:$actualPort")
            loggerRef.info("GraphiQL IDE: http://$hostRef:$actualPort/graphiql")

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(Thread {
                server?.let {
                    loggerRef.info("Shutting down Viaduct DevServe...")
                    it.stop(1000, 2000)
                }
            })

            // Wait for the server to finish
            Thread.currentThread().join()

        } catch (e: Exception) {
            logger.error("Failed to start DevServe server", e)
            throw e
        }
    }

    /**
     * Stops the development server.
     * Useful for testing and programmatic shutdown.
     */
    fun stop() {
        server?.let {
            logger.info("Stopping Viaduct DevServe...")
            it.stop(1000, 2000)
            server = null
        }
    }

    /**
     * Configures the Ktor application.
     */
    private fun Application.configureApplication(
        viaductInstance: Viaduct,
        loggerRef: org.slf4j.Logger,
        mapperRef: ObjectMapper
    ) {
        install(ContentNegotiation) {
            jackson()
        }

        install(CORS) {
            anyHost()
            allowHeader("Content-Type")
        }

        routing {
            // Health check endpoint
            get("/health") {
                call.respondText("OK", ContentType.Text.Plain, HttpStatusCode.OK)
            }

            // GraphQL endpoint
            post("/graphql") {
                try {
                    val body = call.receiveText()
                    val request = mapperRef.readValue<GraphQLRequest>(body)

                    // Log introspection queries for debugging
                    if (request.operationName == "IntrospectionQuery") {
                        loggerRef.info("Received schema introspection query from GraphiQL")
                    } else {
                        loggerRef.debug("Executing GraphQL query: ${request.query}")
                    }

                    val executionInput = ExecutionInput.create(
                        operationText = request.query,
                        operationName = request.operationName,
                        variables = request.variables ?: emptyMap()
                    )

                    val result = viaductInstance.executeAsync(executionInput).await()

                    val response = mapOf(
                        "data" to result.getData<Any>(),
                        "errors" to result.errors?.map { error ->
                            mapOf(
                                "message" to error.message,
                                "locations" to error.locations,
                                "path" to error.path,
                                "extensions" to error.extensions
                            )
                        }
                    )

                    val json = mapperRef.writeValueAsString(response)
                    call.respondText(json, ContentType.Application.Json)

                } catch (e: Exception) {
                    loggerRef.error("Error executing GraphQL query", e)
                    val errorResponse = mapOf(
                        "errors" to listOf(
                            mapOf(
                                "message" to (e.message ?: "Internal server error"),
                                "extensions" to mapOf("exception" to e::class.simpleName)
                            )
                        )
                    )
                    val json = mapperRef.writeValueAsString(errorResponse)
                    call.respondText(json, ContentType.Application.Json, HttpStatusCode.InternalServerError)
                }
            }

            // GraphiQL IDE
            get("/graphiql") {
                call.respondText(graphiQLHtml(), ContentType.Text.Html)
            }

            // Serve GraphiQL static resources (JS files for plugins)
            get("/js/{file}") {
                val file = call.parameters["file"]
                if (file != null) {
                    val resourcePath = "/graphiql/js/$file"
                    val resourceStream = this::class.java.getResourceAsStream(resourcePath)

                    if (resourceStream != null) {
                        val content = resourceStream.bufferedReader().use { it.readText() }
                        val contentType = when {
                            file.endsWith(".js") -> ContentType.Text.JavaScript
                            file.endsWith(".jsx") -> ContentType.Text.JavaScript
                            else -> ContentType.Application.OctetStream
                        }
                        call.respondText(content, contentType)
                    } else {
                        loggerRef.warn("Static resource not found: $resourcePath")
                        call.respond(HttpStatusCode.NotFound, "File not found: $file")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "File parameter missing")
                }
            }

            // Root redirects to GraphiQL
            get("/") {
                call.respondText(
                    """<html><head><meta http-equiv="refresh" content="0; url=/graphiql"></head></html>""",
                    ContentType.Text.Html,
                    HttpStatusCode.OK
                )
            }
        }
    }

    /**
     * Data class for GraphQL requests.
     */
    private data class GraphQLRequest(
        val query: String,
        val variables: Map<String, Any?>? = null,
        val operationName: String? = null
    )
}

/**
 * Entry point for the development server.
 *
 * Can be invoked from a Gradle task or run directly.
 *
 * System properties:
 * - devserve.port: Port to bind to (default: 8080). Use 0 for any available port.
 * - devserve.host: Host to bind to (default: 0.0.0.0)
 */
fun main() {
    val port = System.getProperty("devserve.port", "8080").toIntOrNull() ?: 8080
    val host = System.getProperty("devserve.host", "0.0.0.0")

    DevServeServer(port = port, host = host).start()
}
