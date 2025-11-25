import viaduct.devserve.GraphiQLHtmlCustomizer

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

            // Resolve actual versions for dependencies that use "INCLUDED" placeholder
            // This ensures the POM contains real version numbers when published
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }

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
 * Downloads and customizes the official GraphiQL CDN example HTML.
 *
 * Downloads the base HTML from a specific GraphiQL release and applies Viaduct customizations
 * by parsing the HTML structure and inserting our code at appropriate locations.
 * This is more robust than text-based patches as it adapts to HTML structure changes.
 *
 * To upgrade GraphiQL:
 * 1. Update graphiqlGitTag below to the new release (e.g., "graphiql@5.3.0")
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
        logger.lifecycle("Applying Viaduct customizations...")

        val customizer = GraphiQLHtmlCustomizer(
            sourceUrl = sourceUrl,
            outputFile = outputFile.get().asFile
        )
        customizer.customize()

        logger.lifecycle("GraphiQL HTML customized and saved to: ${outputFile.get().asFile}")
        logger.lifecycle("Based on GraphiQL release: $graphiqlGitTag")
    }
}

// Ensure GraphiQL HTML is downloaded before processing resources
tasks.named("processResources") {
    dependsOn(downloadGraphiQLHtml)
}

// Clean up downloaded GraphiQL
tasks.named<Delete>("clean") {
    delete(layout.buildDirectory.dir("resources/main/graphiql"))
}
