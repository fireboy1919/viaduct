plugins {
    id("conventions.kotlin")
    `maven-publish`
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Publish thin jar with dependencies (for both composite builds and Maven Central)
            // Gradle automatically substitutes this with the local project in composite mode
            from(components["java"])

            artifact(tasks["sourcesJar"])
            artifact(emptyJavadocJar.get())

            groupId = "com.airbnb.viaduct"
            artifactId = "devserve"

            pom {
                name.set("Viaduct DevServe")
                description.set("Development server runtime for Viaduct GraphQL applications with GraphiQL IDE")
                url.set("https://airbnb.io/viaduct/")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("airbnb")
                        name.set("Airbnb, Inc.")
                        email.set("viaduct-maintainers@airbnb.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/airbnb/viaduct.git")
                    developerConnection.set("scm:git:ssh://github.com/airbnb/viaduct.git")
                    url.set("https://github.com/airbnb/viaduct")
                }
            }
        }
    }
}

dependencies {
    // Viaduct dependencies
    implementation(libs.viaduct.service.api)
    implementation(libs.viaduct.service.wiring)
    implementation(libs.viaduct.tenant.api)

    // GraphQL
    implementation(libs.graphql.java)

    // Ktor server
    implementation("io.ktor:ktor-server-core:3.0.3")
    implementation("io.ktor:ktor-server-netty:3.0.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-server-cors:3.0.3")
    implementation("io.ktor:ktor-server-websockets:3.0.3")
    implementation("io.ktor:ktor-serialization-jackson:3.0.3")

    // Classpath scanning for finding @ViaductApplication
    implementation(libs.classgraph)

    // Logging
    implementation(libs.slf4j.api)
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core.jvm)

    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.engine)
}

/**
 * Downloads HTML from a URL.
 */
fun downloadHtml(sourceUrl: String): String {
    val tempFile = File.createTempFile("graphiql", ".html")
    try {
        ant.invokeMethod("get", mapOf("src" to sourceUrl, "dest" to tempFile))
        return tempFile.readText()
    } finally {
        tempFile.delete()
    }
}

/**
 * Applies Viaduct customizations to GraphiQL HTML.
 */
fun customizeGraphiQLHtml(html: String): String {
    var customized = html

    // 1. Update title
    customized = customized.replace(
        Regex("<title>.*?</title>"),
        "<title>GraphiQL - Viaduct DevServe</title>"
    )

    // 2. Update copyright
    customized = customized.replace(
        "Copyright (c) 2025 GraphQL Contributors",
        "Copyright (c) 2025 Airbnb, Inc."
    )

    // 3. Change demo endpoint to /graphql
    customized = customized.replace(
        "url: 'https://countries.trevorblades.com'",
        "url: '/graphql'"
    )

    // 4. Find the module script and inject our customizations
    val moduleScriptPattern = Regex(
        """(<script type="module">)(.*?)(</script>)""",
        RegexOption.DOT_MATCHES_ALL
    )

    val moduleScriptMatch = moduleScriptPattern.find(customized)
    if (moduleScriptMatch != null) {
        val (opening, scriptContent, closing) = moduleScriptMatch.destructured

        // Add our imports at the beginning of the module script
        val viaductImports = """
      import { loadJSX } from '/js/jsx-loader.js';
      import { createPatchedFetcher } from '/js/introspection-patch.js';
"""

        // Wrap the fetcher creation with our patch
        var modifiedScript = scriptContent.replace(
            Regex("""const fetcher = createGraphiQLFetcher\(\{[\s\S]*?\}\);"""),
            """const baseFetcher = createGraphiQLFetcher({
        url: '/graphql',
      });
      const fetcher = createPatchedFetcher(baseFetcher);"""
        )

        // Modify the plugin initialization to load our Global ID plugin
        modifiedScript = modifiedScript.replace(
            "const plugins = [HISTORY_PLUGIN, explorerPlugin()];",
            """// Load Viaduct plugins asynchronously
      async function loadPlugins() {
        try {
          const pluginModule = await loadJSX('/js/global-id-plugin.jsx');
          const createGlobalIdPlugin = pluginModule.createGlobalIdPlugin;
          const globalIdPlugin = createGlobalIdPlugin(React);
          return [HISTORY_PLUGIN, explorerPlugin(), globalIdPlugin];
        } catch (error) {
          console.error('Failed to load Viaduct Global ID plugin:', error);
          return [HISTORY_PLUGIN, explorerPlugin()];
        }
      }"""
        )

        // Replace the App rendering to be async and use our plugins
        modifiedScript = modifiedScript.replace(
            Regex("""function App\(\)[\s\S]*?root\.render\(React\.createElement\(App\)\);"""),
            """async function initGraphiQL() {
        const plugins = await loadPlugins();
        const explorer = plugins.find(p => p.title === 'Explorer');
        const defaultQuery = `# Welcome to Viaduct DevServe!
#
# Start typing your GraphQL query here.
# Press Ctrl+Space for autocomplete.
# Click the Docs button to explore the schema.
# Use the Global ID Utils plugin (key icon) to encode/decode Viaduct Global IDs.

query {
  # Your query here
}
`;

        function App() {
          return React.createElement(GraphiQL, {
            fetcher,
            plugins,
            visiblePlugin: explorer,
            defaultQuery,
            defaultEditorToolsVisibility: true,
          });
        }

        const container = document.getElementById('graphiql');
        const root = ReactDOM.createRoot(container);
        root.render(React.createElement(App));
      }

      initGraphiQL();"""
        )

        // Reconstruct the script with our imports
        val newModuleScript = opening + viaductImports + modifiedScript + closing
        customized = customized.replace(moduleScriptMatch.value, newModuleScript)
    } else {
        throw GradleException(
            "Could not find module script in GraphiQL HTML. HTML structure may have changed."
        )
    }

    return customized
}

/**
 * Downloads and customizes the official GraphiQL CDN example HTML.
 *
 * Downloads the base HTML from a specific GraphiQL release and applies Viaduct customizations
 * by parsing the HTML structure and inserting our code at appropriate locations.
 * This is more robust than text-based patches as it adapts to HTML structure changes.
 *
 * To upgrade GraphiQL:
 * 1. Update graphiqlGitTag below to the new release (e.g., "graphiql@3.1.0")
 * 2. Run: ./gradlew :devserve:downloadGraphiQLHtml
 * 3. Test the result
 */
val downloadGraphiQLHtml by tasks.registering {
    group = "build"
    description = "Download and customize GraphiQL HTML from official repository"

    // GraphiQL release tag to use - update this to upgrade
    val graphiqlGitTag = "graphiql@5.2.1"

    val outputDir = layout.buildDirectory.dir("resources/main/graphiql")
    val outputFile = outputDir.map { it.file("index.html") }

    outputs.file(outputFile)
    outputs.cacheIf { true }

    doLast {
        val sourceUrl = "https://raw.githubusercontent.com/graphql/graphiql/$graphiqlGitTag/examples/graphiql-cdn/index.html"

        logger.lifecycle("Downloading GraphiQL HTML from: $sourceUrl")
        val html = downloadHtml(sourceUrl)

        logger.lifecycle("Applying Viaduct customizations...")
        val customized = customizeGraphiQLHtml(html)

        // Write the customized HTML
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(customized)
        }

        logger.lifecycle("GraphiQL HTML customized and saved to: ${outputFile.get().asFile}")
        logger.lifecycle("Based on GraphiQL release: $graphiqlGitTag")
    }
}

// Ensure GraphiQL HTML is downloaded before processing resources
tasks.named("processResources") {
    dependsOn(downloadGraphiQLHtml)
}

// Clean up downloaded GraphiQL
tasks.named("clean") {
    doLast {
        delete(layout.buildDirectory.dir("resources/main/graphiql"))
    }
}
