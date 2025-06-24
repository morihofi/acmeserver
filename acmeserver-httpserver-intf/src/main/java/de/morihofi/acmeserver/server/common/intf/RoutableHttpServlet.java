package de.morihofi.acmeserver.server.common.intf;

import de.morihofi.acmeserver.server.common.intf.handler.AbstractExceptionHandler;
import de.morihofi.acmeserver.server.common.intf.handler.common.OptionsHandler;
import de.morihofi.acmeserver.server.common.intf.wrapper.HttpRequestWrapper;
import de.morihofi.acmeserver.server.common.intf.wrapper.HttpResponseWrapper;
import de.morihofi.acmeserver.types.httpserver.HandlerType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public abstract class RoutableHttpServlet extends HttpServlet {

    @Getter(AccessLevel.PROTECTED)
    private final Router router = new Router();

    @Getter
    @Setter
    private AbstractExceptionHandler exceptionHandler = new AbstractExceptionHandler() {
        @Override
        public void handle(Exception e, HandlerContext context) throws Exception {
            context.result("Internal Server Error, see logs for details");
        }
    };

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = req.getMethod();
        String path = req.getRequestURI();
        HandlerContext context = getHandlerContext(req, resp);

        log.info("Incoming request: method={}, path={}", method, path);

        // Let the Handler do its thing
        try {
            Handler handler = HandlerType.valueOf(method) ==
                    HandlerType.OPTIONS ?
                    new OptionsHandler() :
                    router.getHandler(path, method, context);

            if (handler == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;

            }

            handler.handle(context);
            log.info("Successfully handled request for path: {}", path);


        } catch (Exception e) {
            log.error("Error while handling request for path: {}", path, e);
            try {
                exceptionHandler.handle(e, context);
            } catch (Exception e1) {
                log.error("Exception Handler couldn't handle the exception and threw an exception itself", e1);
            }

        }
    }

    private HandlerContext getHandlerContext(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return new HandlerContext(
                new HttpRequestWrapper(req),
                new HttpResponseWrapper(resp),
                router
        );
    }
}
