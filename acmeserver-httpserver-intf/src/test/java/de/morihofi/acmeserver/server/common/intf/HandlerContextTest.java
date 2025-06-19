package de.morihofi.acmeserver.server.common.intf;

import de.morihofi.acmeserver.server.common.intf.*;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandlerContextTest {
    private Router router;
    private StubRequest request;
    private StubResponse response;

    static class StubRequest implements Request {
        String path;
        String method = "GET";
        Map<String,String> query = new HashMap<>();
        Map<String,String> headers = new HashMap<>();
        byte[] body = new byte[0];
        @Override public String getPath(){return path;}
        @Override public String getMethod(){return method;}
        @Override public String getHeader(String name){return headers.get(name);}    
        @Override public String getBody(){return new String(body, StandardCharsets.UTF_8);}    
        @Override public String getIP(){return "127.0.0.1";}    
        @Override public String getQueryParam(String name){return query.get(name);}    
        @Override public byte[] getBodyBytes(){return body;}
    }

    static class StubResponse extends Response {
        Map<String,String> headers = new HashMap<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @Override public void setHeader(String name, String value){headers.put(name,value);}    
        @Override public String getHeader(String name){return headers.get(name);}    
        @Override public void setBodyBytes(byte[] data){try{out.write(data);}catch(IOException ignore){}}
        @Override public Map<String,String> getHeaders(){return headers;}
        @Override public java.io.OutputStream getOutputStream(){return out;}
    }

    static class Data { int x; }

    @BeforeEach
    void setup(){
        router = new Router();
        request = new StubRequest();
        response = new StubResponse();
    }

    private HandlerContext ctx(){
        return new HandlerContext(request, response, router);
    }

    @Test
    @DisplayName("header and content type")
    void testHeader(){
        HandlerContext ctx = ctx();
        ctx.header("X-Test","v");
        assertEquals("v", response.getHeader("X-Test"));
        ctx.contentType("text/plain");
        assertEquals("text/plain", response.getHeader("Content-Type"));
    }

    @Test
    @DisplayName("query and path params")
    void testParams(){
        router.addHandler(new Endpoint(HandlerType.GET, "/items/{id}", c->{ }));
        request.path = "/items/5";
        request.query.put("q","1");
        HandlerContext ctx = ctx();
        assertEquals("1", ctx.queryParam("q"));
        assertEquals("5", ctx.pathParam("id"));
    }

    @Test
    @DisplayName("json serialization")
    void testJson(){
        HandlerContext ctx = ctx();
        ctx.json(Map.of("a",1));
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertTrue(response.getOutputStream().toString().contains("\"a\""));
    }

    @Test
    @DisplayName("body as class and bytes")
    void testBodyReading() throws IOException {
        request.body = "{\"x\":5}".getBytes(StandardCharsets.UTF_8);
        HandlerContext ctx = ctx();
        assertArrayEquals(request.body, ctx.bodyAsBytes());
        Data d = ctx.bodyAsClass(Data.class);
        assertEquals(5,d.x);
    }

    @Test
    @DisplayName("method enum")
    void testMethod(){
        request.method = "POST";
        HandlerContext ctx = ctx();
        assertEquals(HandlerType.POST, ctx.method());
    }
}
