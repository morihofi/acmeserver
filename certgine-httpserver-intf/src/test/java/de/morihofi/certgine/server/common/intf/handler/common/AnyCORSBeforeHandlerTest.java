/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.server.common.intf.handler.common;

import de.morihofi.certgine.server.common.intf.HandlerContext;
import de.morihofi.certgine.server.common.intf.Request;
import de.morihofi.certgine.server.common.intf.Response;
import de.morihofi.certgine.server.common.intf.Router;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnyCORSBeforeHandlerTest {
    static class DummyResponse extends Response {
        java.util.Map<String,String> headers = new java.util.HashMap<>();
        @Override public void setHeader(String name, String value){headers.put(name,value);}    
        @Override public String getHeader(String name){return headers.get(name);}    
        @Override public void setBodyBytes(byte[] data){}
        @Override public java.util.Map<String,String> getHeaders(){return headers;}    
        @Override public java.io.OutputStream getOutputStream(){return java.io.OutputStream.nullOutputStream();}
    }
    @Test
    void addsCorsHeaders(){
        DummyResponse resp = new DummyResponse();
        Request req = new Request() {
            @Override
            public HttpServletRequest getHttpServletRequest() {
                return null;
            }

            @Override public String getPath(){return "/";}
            @Override public String getMethod(){return "GET";}
            @Override public String getHeader(String name){return null;}
            @Override public String getBody(){return "";}
            @Override public String getIP(){return "";}
            @Override public String getQueryParam(String name){return null;}
            @Override public byte[] getBodyBytes(){return new byte[0];}
        };
        HandlerContext ctx = new HandlerContext(req, resp, new Router());
        new AnyCORSBeforeHandler().handle(ctx);
        assertEquals("*", resp.getHeader("Access-Control-Allow-Origin"));
        assertNotNull(resp.getHeader("Access-Control-Allow-Headers"));
    }
}
