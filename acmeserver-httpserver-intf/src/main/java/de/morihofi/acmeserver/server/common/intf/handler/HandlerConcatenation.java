package de.morihofi.acmeserver.server.common.intf.handler;

import de.morihofi.acmeserver.server.common.intf.Handler;
import de.morihofi.acmeserver.server.common.intf.HandlerContext;

import java.util.ArrayList;
import java.util.List;

public class HandlerConcatenation implements Handler {

    private List<Handler> handlers = new ArrayList<>();

    public HandlerConcatenation() {
    }

    public HandlerConcatenation(List<Handler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(HandlerContext context) throws Exception {
        for (Handler h : handlers){
            h.handle(context);
        }
    }
}
