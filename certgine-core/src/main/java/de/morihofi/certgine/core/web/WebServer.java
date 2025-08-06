/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.web;

import de.morihofi.certgine.core.Main;
import de.morihofi.certgine.types.events.AbstractEvent;
import de.morihofi.certgine.types.events.EventSubscriber;
import de.morihofi.certgine.types.events.ServerShutdownEvent;
import de.morihofi.certgine.types.intf.IServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;

/**
 * Web Server for the Website, API and ACME Service
 */
@Slf4j
public class WebServer implements EventSubscriber {
    /**
     * Instance of IServerInstance providing access to server-related configurations and utilities.
     */
    private final IServerInstance serverInstance;

    private final Server server;

    private final TlsCertificateManager tlsManager;
    private final ServletRegistrar servletRegistrar;


    /**
     * Constructor for WebServer.
     *
     * @param serverInstance The instance of IServerInstance providing access to server-related configurations and utilities.
     */
    public WebServer(IServerInstance serverInstance) {
        this.serverInstance = serverInstance;
        log.info("Registering WebServer event listener");
        serverInstance.getEventBus().register(this);

        // We don't use virtual thread pool here, because it may deadlock in Java 21.
        // This is a known issue with Jetty and virtual threads and fixed in newer Java versions.
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("WebServer-ThreadPool");

        this.server = new Server(threadPool);

        this.tlsManager = new TlsCertificateManager(serverInstance, server);
        this.servletRegistrar = new ServletRegistrar(serverInstance, serverInstance.getModuleRegistry());
        serverInstance.getEventBus().register(tlsManager);
    }

    /**
     * Method to start the Web and API Server
     *
     * @throws Exception thrown when startup fails
     */
    public void startServer() throws Exception {
        log.info("Starting ACME API WebServer");

        if (serverInstance.getAppConfig().getServer().getPorts().getHttp() > 0) {
            ServerConnector httpConnector = new ServerConnector(server);
            httpConnector.setPort(serverInstance.getAppConfig().getServer().getPorts().getHttp());
            server.addConnector(httpConnector);
            log.info("HTTP is configured on port {}", serverInstance.getAppConfig().getServer().getPorts().getHttp());
        }

        if (serverInstance.getAppConfig().getServer().getPorts().getHttps() > 0) {
            tlsManager.setupTls();
        } else {
            log.error("HTTPS support is DISABLED");
            throw new IllegalArgumentException("HTTPS support cannot be disabled");
        }

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        servletRegistrar.addBundledServlets(context);

        server.start();

        tlsManager.initialize();

        // async certificate issuance handled by modules if available

        logActiveConnectorsAndWaitReady();

        log.info("\u2705 Ready for incoming requests");
        Main.startupTime = (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime()) / 1000L;
        log.info("Startup took {} seconds", Main.startupTime);
    }


    @Override
    public List<Class<? extends AbstractEvent>> canHandle() {
        return List.of(ServerShutdownEvent.class);
    }

    @Override
    public void onEvent(AbstractEvent event) throws Exception {
        if (event instanceof ServerShutdownEvent) {
            log.info("Received ServerShutdownEvent, shutting down WebServer...");
            server.stop();
            serverInstance.getModuleRegistry().shutdownScheduler();
        }
    }

    /**
     * Logs the active connectors and waits until they accept connections.
     */
    void logActiveConnectorsAndWaitReady() throws InterruptedException {
        for (Connector connector : server.getConnectors()) {
            if (connector instanceof ServerConnector sc) {
                String host = sc.getHost();
                if (host == null || host.isBlank()) {
                    host = "127.0.0.1";
                }
                int port = sc.getLocalPort();

                // Determine the scheme based on connection factories
                String scheme = "http";
                for (ConnectionFactory factory : sc.getConnectionFactories()) {
                    if (factory.getProtocol().toLowerCase().contains("ssl")) {
                        scheme = "https";
                        break;
                    }
                }

                String url = String.format("%s://%s:%d", scheme, host, port);
                log.info("Jetty listening on {}", url);
                waitUntilAccepting(host, port, Duration.ofSeconds(10));
            }
        }
    }


    /**
     * Waits until a TCP connection to the given host and port succeeds within the specified timeout.
     *
     * @param host    host to connect to
     * @param port    port to connect to
     * @param timeout maximum time to wait
     * @throws InterruptedException          if the thread is interrupted while waiting
     * @throws IllegalStateException         if the connection could not be established within the timeout
     */
    static void waitUntilAccepting(String host, int port, Duration timeout) throws InterruptedException {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), (int) Math.min(timeout.toMillis(), 1000));
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("Jetty not accepting connections on " + host + ":" + port);
    }
}
