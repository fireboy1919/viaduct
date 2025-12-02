package viaduct.devserve

/**
 * Returns the HTML for the GraphiQL IDE.
 *
 * The GraphiQL HTML is loaded from src/main/resources/graphiql/index.html and packaged
 * into the JAR at build time. The HTML uses CDN-hosted GraphiQL libraries and loads
 * custom plugins from separate JavaScript files:
 * - introspection-patch.js: Patches GraphQL Java introspection responses for GraphiQL 5 compatibility
 * - global-id-plugin.jsx: Provides Global ID encode/decode utilities
 *
 * @return The GraphiQL HTML content
 * @throws IllegalStateException if the GraphiQL HTML cannot be found in resources
 */
fun graphiQLHtml(): String {
    val resourcePath = "/graphiql/index.html"

    return object {}.javaClass.getResourceAsStream(resourcePath)?.use { stream ->
        stream.bufferedReader().readText()
    } ?: throw IllegalStateException(
        "GraphiQL HTML not found at $resourcePath. " +
        "Ensure the downloadGraphiQL Gradle task has been run during build."
    )
}
