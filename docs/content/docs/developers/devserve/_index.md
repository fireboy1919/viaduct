---
title: Development Server (devserve)
description: Run your Viaduct application with GraphiQL IDE and auto-reloading
weight: 100
---

## Overview

The `devserve` task provides a development server for Viaduct applications with:
- **GraphiQL IDE**: Interactive GraphQL explorer in your browser
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
- GraphiQL IDE: `http://localhost:8080/graphiql`
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
3. The browser automatically reconnects to the new server instance

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

2. Open GraphiQL in your browser: `http://localhost:8080/graphiql`

3. Make changes to your schema or resolvers:
   ```graphql
   # Add a new field to Character.graphqls
   type Character {
     # ... existing fields ...
     nickname: String
   }
   ```

4. Gradle automatically detects the change, regenerates code, and restarts the server

5. Refresh GraphiQL to see the new field in the schema

## Using GraphiQL

GraphiQL provides an interactive environment for exploring and testing your GraphQL API:

### Features

- **Query Editor**: Write and execute GraphQL queries
- **Schema Documentation**: Browse your schema's types and fields
- **Auto-completion**: Get suggestions as you type
- **Query History**: Access previously executed queries
- **Variables Panel**: Test queries with different variable values

### Example Query

Try this query in GraphiQL:

```graphql
{
  allCharacters(limit: 5) {
    name
    birthYear
    homeworld {
      name
    }
  }
}
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, either:
- Stop the process using the port
- Use a different port with `-Pdevserve.port=<port>`

### Server Not Restarting in Continuous Mode

If the server doesn't restart after changes:
1. Check that you're using `--continuous` flag
2. Verify your changes are in watched files (schema or source code)
3. Check Gradle output for any compilation errors
4. Try stopping and restarting the continuous build

### Changes Not Reflected

If code changes don't appear in GraphiQL:
1. Hard refresh your browser (`Cmd+Shift+R` or `Ctrl+Shift+F5`)
2. Check the Gradle output for any errors during recompilation
3. Verify the server actually restarted (look for "Starting devserve server..." in logs)

## Comparison with Production

`devserve` is for development only. For production deployments:
- Configure your actual HTTP server (Ktor, Jetty, etc.)
- Set up proper authentication and authorization
- Configure production logging and monitoring
- Review the [Service Engineers](../../service_engineers/) documentation

## Next Steps

- Learn about [Testing](../testing/) your Viaduct application
- Explore [Resolvers](../resolvers/) to add business logic
- Understand [Schema Management](../schema_change_management/) for evolving your API
