/**
 * Introspection Response Patch for GraphiQL 5
 *
 * GraphiQL 5 strictly requires certain fields to always be present (even if empty),
 * but GraphQL Java omits these fields when they would be empty arrays.
 * This patch ensures compatibility between GraphQL Java's introspection responses
 * and GraphiQL 5's expectations.
 */

/**
 * Creates a patched fetcher that fixes introspection responses for GraphiQL compatibility.
 *
 * @param {Function} baseFetcher - The base fetcher to wrap
 * @returns {Function} A patched fetcher that normalizes introspection responses
 */
export function createPatchedFetcher(baseFetcher) {
  return async (graphQLParams, options) => {
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
}
