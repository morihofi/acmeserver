package de.morihofi.certgine.core.servlet.api;

import de.morihofi.certgine.server.common.intf.HandlerContext;

/**
 * A class representing a local context for GraphQL execution, containing a HTTP context.
 * This class is used to provide additional context information, such as the http context, to GraphQL execution.
 *
 * @param handlerContext The HTTP context associated with the GraphQL execution.
 * @author Moritz Hofmann
 */
public record GraphQLLocalContext(HandlerContext handlerContext) {
    /**
     * Gets the Handler HTTP context associated with the GraphQL execution.
     *
     * @return The Handler HTTP context.
     */
    @Override
    public HandlerContext handlerContext() {
        return handlerContext;
    }
}