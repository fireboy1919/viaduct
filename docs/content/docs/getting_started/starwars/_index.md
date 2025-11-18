---
title: StarWars Demo Application
description: Explore an advanced Viaduct application.
weight: 6
---
## Overview
This application implements a comprehensive GraphQL API for the Star Wars universe, demonstrating how Viaduct handles
complex data relationships, advanced resolver patterns, and sophisticated schema design.

## What you'll find

The StarWars demo showcases:
- **Node Resolvers**: Direct object resolution patterns
- **Field Resolvers**: Field-level data fetching strategies
- **Batch Resolution**: Efficient bulk data loading techniques
- **Mutations**: Modifying data through GraphQL mutations
- **Variables Provider**: Dynamic variable injection and management
- **Backing Data**: Using Kotlin objects as data sources in Viaduct
- **Global ID System**: Viaduct's approach to unique entity identification across your schema grts

## Getting the starwars application

The StarWars application is available on GitHub at [github.com/viaduct-graphql/starwars](https://github.com/viaduct-graphql/starwars).

```shell
git clone https://github.com/viaduct-graphql/starwars.git
cd starwars
```

## Running the application

Follow the instructions in the repository's README to build and run the application:

```shell
./gradlew test
./gradlew run
```

### Using the Development Server

For interactive development with GraphiQL IDE, use the development server:

```shell
./gradlew devserve
```

Then open `http://localhost:8080/graphiql` in your browser to explore the StarWars GraphQL API.

For hot-reloading during development (automatically reloads when you edit schema or code files):

```shell
./gradlew --continuous devserve
```

Learn more in the [Development Server documentation](../../developers/devserve/).

After exploring the StarWars application, you'll have a solid understanding of how to build production-ready GraphQL
applications with Viaduct.

## Related resources

- [Viaduct Documentation](../)
- [GitHub Repository](https://github.com/viaduct-graphql)

{{< prevnext >}}
