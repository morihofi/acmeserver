package de.morihofi.certgine.server.common.intf.handler.common;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Router;
import de.morihofi.certgine.server.common.intf.Response;
import de.morihofi.certgine.server.common.intf.Request;
import de.morihofi.certgine.server.common.intf.HttpStatusCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionsHandlerTest {
    static class DummyResponse extends Response {
        int status;
        @Override public void setHeader(String name, String value) {}
        @Override public String getHeader(String name) { return null; }
        @Override public void setBodyBytes(byte[] data) {}
        @Override public java.util.Map<String, String> getHeaders(){return java.util.Collections.emptyMap();}
        @Override public java.io.OutputStream getOutputStream(){return java.io.OutputStream.nullOutputStream();}
    }
    @Test
    void optionsStatusDependsOnHandler(){
        Router router = new Router();
        DummyResponse resp = new DummyResponse();
        Request req = new Request() {
            @Override
            public HttpServletRequest getHttpServletRequest() {
                return null;
            }

            @Override public String getPath(){return "/path";}
            @Override public String getMethod(){return "OPTIONS";}
            @Override public String getHeader(String name){return null;}
            @Override public String getBody(){return "";}
            @Override public String getIP(){return "";}
            @Override public String getQueryParam(String name){return null;}
            @Override public byte[] getBodyBytes(){return new byte[0];}
        };
        HandlerContext ctx = new HandlerContext(req, resp, router);
        OptionsHandler handler = new OptionsHandler();
        handler.handle(ctx);
        assertEquals(HttpStatusCode.NOT_FOUND.getCode(), resp.getStatus());
        router.addHandler(new de.morihofi.certgine.server.common.intf.Endpoint(de.morihofi.certgine.types.httpserver.HandlerType.GET,"/path", c->{ }));
        resp.setStatus(0);
        handler.handle(ctx);
        assertEquals(HttpStatusCode.NO_CONTENT.getCode(), resp.getStatus());
    }
}
