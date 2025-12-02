---
title: Development Server (devserve)
description: Run your Viaduct application with auto-reloading for rapid development
weight: 100
---

## Overview

The `devserve` task provides a development server for Viaduct applications with:
- **GraphQL endpoint**: Full GraphQL API at `/graphql`
- **Auto-reloading**: Automatic reloading when schema or code changes
- **Zero configuration**: Works out-of-the-box with any Viaduct application

## Prerequisites

The `devserve` task is automatically available in any project that applies the Viaduct application plugin:

```kotlin
plugins {
    id("com.airbnb.viaduct.application-gradle-plugin")
}
```

## Basic Usage

### Running the Server

To start the development server:

```shell
./gradlew devserve
```

The server will start on `http://localhost:8080` by default and provide:
- GraphQL endpoint: `http://localhost:8080/graphql`
- Health check: `http://localhost:8080/health`

Press `Ctrl+C` to stop the server.

### Custom Port and Host

You can customize the port and host using Gradle properties:

```shell
./gradlew devserve -Pdevserve.port=3000 -Pdevserve.host=127.0.0.1
```

Or set them in your `gradle.properties` file:

```properties
devserve.port=3000
devserve.host=127.0.0.1
```

## Auto-Reloading with Continuous Mode

For development workflows with automatic reloading when files change, use Gradle's continuous mode:

```shell
./gradlew --continuous devserve
```

In continuous mode:
1. Gradle watches all input files (schema files, Kotlin source code, etc.)
2. When changes are detected, Gradle automatically:
   - Regenerates GRT classes if schema files changed
   - Recompiles Kotlin code if source files changed
   - Restarts the devserve server with fresh code
3. Your GraphQL clients automatically reconnect to the new server instance

This provides a fast development loop where you can edit schema and code files and see changes reflected immediately.

### What Gets Watched

Continuous mode watches:
- **GraphQL schema files** (`.graphqls`) in all modules
- **Kotlin source files** in `src/main/kotlin`
- **Resource files** referenced by the application

### Development Workflow

A typical development workflow with auto-reloading:

1. Start the server in continuous mode:
   ```shell
   ./gradlew --continuous devserve
   ```

2. Use a GraphQL client (like curl or Postman) to query the API:
   ```shell
   curl -X POST http://localhost:8080/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ __schema { types { name } } }"}'
   ```

3. Make changes to your schema or resolvers:
   ```graphql
   # Add a new field to Character.graphqls
   type Character {
     # ... existing fields ...
     nickname: String
   }
   ```

4. Gradle detects the change and reloads automatically

5. Query the API again to see your changes

## How It Works

### ViaductDevServeProvider

The devserve system uses a provider pattern to get the Viaduct instance from your application. You implement `ViaductDevServeProvider` to tell devserve how to create your Viaduct:

```kotlin
@ViaductDevServeConfiguration
class MyDevServeProvider : ViaductDevServeProvider {
    override fun getViaduct(): Viaduct {
        // Return your configured Viaduct instance
        return MyViaductConfiguration.viaductService
    }
}
```

The `@ViaductDevServeConfiguration` annotation marks your provider class for discovery by the devserve system.

### Shared Configuration Pattern

For best results, share the same Viaduct configuration between your production code and devserve:

```kotlin
// In your application
object ViaductConfiguration {
    val viaductService: Viaduct by lazy {
        BasicViaductFactory.create(
            tenantRegistrationInfo = TenantRegistrationInfo(
                tenantPackagePrefix = "com.example.myapp"
            )
        )
    }
}
```

This ensures devserve uses the exact same configuration as your production application.

## Hot Reload via SIGHUP

In addition to Gradle's continuous mode, devserve supports hot-reload via the SIGHUP signal:

```shell
kill -HUP $(pgrep -f devserve)
```

This triggers a reload without restarting the server process, useful for custom automation scripts.

## Troubleshooting

### Server Won't Start

1. Check if another process is using port 8080:
   ```shell
   lsof -i :8080
   ```

2. Try a different port:
   ```shell
   ./gradlew devserve -Pdevserve.port=3000
   ```

### Changes Not Detected

1. Ensure you're using continuous mode:
   ```shell
   ./gradlew --continuous devserve
   ```

2. Check that your files are in watched directories (`src/main/kotlin`, etc.)

### ClassLoader Issues

If you see `ClassNotFoundException` or similar errors after reload:
1. Stop the server completely
2. Run a clean build: `./gradlew clean build`
3. Restart devserve
