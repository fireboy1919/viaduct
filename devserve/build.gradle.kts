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
 * Task to generate GraphiQL HTML file with Global ID plugin.
 * Creates GraphiQL 5 IDE with Explorer and Global ID utilities for Viaduct development.
 */
val generateGraphiQL by tasks.registering {
    group = "build"
    description = "Generate GraphiQL HTML with plugins for the development server"

    val outputDir = layout.buildDirectory.dir("resources/main/graphiql")
    val outputFile = outputDir.map { it.file("index.html") }

    outputs.file(outputFile)
    outputs.cacheIf { true }

    doLast {
        logger.lifecycle("Generating GraphiQL HTML with plugins...")

        val graphiqlHtml = """
<!--
 *  Copyright (c) 2025 Airbnb, Inc.
 *  All rights reserved.
 *
 *  This source code is licensed under the Apache 2.0 license found in the
 *  LICENSE file in the root directory of this source tree.
-->
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>GraphiQL - Viaduct DevServe</title>
    <style>
      body {
        margin: 0;
      }

      #graphiql {
        height: 100dvh;
      }

      .loading {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 4rem;
      }
    </style>
    <link rel="stylesheet" href="https://esm.sh/graphiql/dist/style.css" />
    <link
      rel="stylesheet"
      href="https://esm.sh/@graphiql/plugin-explorer/dist/style.css"
    />
    <!-- Babel standalone for JSX transpilation -->
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
    <!--
     * Note:
     * The ?standalone flag bundles the module along with all of its `dependencies`, excluding `peerDependencies`, into a single JavaScript file.
     * `@emotion/is-prop-valid` is a shim to remove the console error ` module "@emotion /is-prop-valid" not found`. Upstream issue: https://github.com/motiondivision/motion/issues/3126
    -->
    <script type="importmap">
      {
        "imports": {
          "react": "https://esm.sh/react@19.1.0",
          "react/": "https://esm.sh/react@19.1.0/",

          "react-dom": "https://esm.sh/react-dom@19.1.0",
          "react-dom/": "https://esm.sh/react-dom@19.1.0/",

          "graphiql": "https://esm.sh/graphiql?standalone&external=react,react-dom,@graphiql/react,graphql",
          "graphiql/": "https://esm.sh/graphiql/",
          "@graphiql/plugin-explorer": "https://esm.sh/@graphiql/plugin-explorer?standalone&external=react,@graphiql/react,graphql",
          "@graphiql/react": "https://esm.sh/@graphiql/react?standalone&external=react,react-dom,graphql,@graphiql/toolkit,@emotion/is-prop-valid",

          "@graphiql/toolkit": "https://esm.sh/@graphiql/toolkit?standalone&external=graphql",
          "graphql": "https://esm.sh/graphql@16.11.0",
          "@emotion/is-prop-valid": "data:text/javascript,"
        }
      }
    </script>
    <script type="module">
      import React from 'react';
      import ReactDOM from 'react-dom/client';
      import { GraphiQL, HISTORY_PLUGIN } from 'graphiql';
      import { createGraphiQLFetcher } from '@graphiql/toolkit';
      import { explorerPlugin } from '@graphiql/plugin-explorer';
      import { loadJSX } from '/js/jsx-loader.js';
      import 'graphiql/setup-workers/esm.sh';

      const baseFetcher = createGraphiQLFetcher({
        url: '/graphql',
      });

      // Patch fetcher to fix introspection response for GraphiQL compatibility
      // GraphiQL 5 strictly requires certain fields to always be present (even if empty),
      // but GraphQL Java omits these fields when they would be empty arrays
      const fetcher = async (graphQLParams, options) => {
        const result = await baseFetcher(graphQLParams, options);

        // Check if this is an introspection query response
        if (result?.data?.__schema) {
          // Fix directives missing args field
          if (result.data.__schema.directives) {
            result.data.__schema.directives = result.data.__schema.directives.map(directive => {
              if (!directive.hasOwnProperty('args')) {
                return { ...directive, args: [] };
              }
              return directive;
            });
          }

          // Fix types missing interfaces and fields missing args
          if (result.data.__schema.types) {
            result.data.__schema.types = result.data.__schema.types.map(type => {
              const fixedType = { ...type };

              // Fix OBJECT and INTERFACE types missing interfaces field
              if ((type.kind === 'OBJECT' || type.kind === 'INTERFACE') && !type.hasOwnProperty('interfaces')) {
                fixedType.interfaces = [];
              }

              // Fix fields missing args field
              if (type.fields) {
                fixedType.fields = type.fields.map(field => {
                  if (!field.hasOwnProperty('args')) {
                    return { ...field, args: [] };
                  }
                  return field;
                });
              }

              return fixedType;
            });
          }
        }

        return result;
      };

      // Load JSX plugin and initialize
      async function initializeGraphiQL() {
        try {
          const pluginModule = await loadJSX('/js/global-id-plugin.jsx');
          const createGlobalIdPlugin = pluginModule.createGlobalIdPlugin;

          // Create plugins
          const explorer = explorerPlugin();
          const globalIdPlugin = createGlobalIdPlugin(React);
          const plugins = [HISTORY_PLUGIN, explorer, globalIdPlugin];

          // Initialize GraphiQL with plugins
          renderGraphiQL(plugins);
        } catch (error) {
          console.error('Failed to load JSX plugin:', error);
          // Fallback: render GraphiQL without the global ID plugin
          const explorer = explorerPlugin();
          const plugins = [HISTORY_PLUGIN, explorer];
          renderGraphiQL(plugins);
        }
      }

      function renderGraphiQL(plugins) {
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
          const explorer = plugins.find(p => p.title === 'Explorer');
          return React.createElement(GraphiQL, {
            fetcher,
            plugins,
            visiblePlugin: explorer, // Open explorer by default
            defaultQuery,
            defaultEditorToolsVisibility: true,
          });
        }

        const container = document.getElementById('graphiql');
        const root = ReactDOM.createRoot(container);
        root.render(React.createElement(App));
      }

      // Initialize the application
      initializeGraphiQL();
    </script>
  </head>
  <body>
    <div id="graphiql">
      <div class="loading">Loading…</div>
    </div>
  </body>
</html>
        """.trimIndent()

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(graphiqlHtml)
        }

        logger.lifecycle("GraphiQL HTML generated successfully at: ${outputFile.get().asFile}")
    }
}

// Ensure GraphiQL is generated before processing resources
tasks.named("processResources") {
    dependsOn(generateGraphiQL)
}

// Clean up generated GraphiQL
tasks.named("clean") {
    doLast {
        delete(layout.buildDirectory.dir("resources/main/graphiql"))
    }
}
