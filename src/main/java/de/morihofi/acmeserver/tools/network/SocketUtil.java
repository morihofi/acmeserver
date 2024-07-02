/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package de.morihofi.acmeserver.tools.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Utility class to find a port to debug on.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SocketUtil {
    private static final Random fgRandom = new Random(System.currentTimeMillis());

    /**
     * Returns a free port number on the specified host within the given range,
     * or -1 if none found.
     *
     * @param host       name or IP address of host on which to find a free port
     * @param searchFrom the port number from which to start searching
     * @param searchTo   the port number at which to stop searching
     * @return a free port in the specified range, or -1 of none found
     * @deprecated Use <code>findFreePort()</code> instead. It is possible that this
     * method can return a port that is already in use since the implementation does
     * not bind to the given port to ensure that it is free.
     */
    @Deprecated
    public static int findUnusedLocalPort(String host, int searchFrom, int searchTo) {

        for (int i = 0; i < 10; i++) {
            int port = getRandomPort(searchFrom, searchTo);
            try (Socket s = new Socket(host, port)) {
                s.getLocalPort(); // avoid warning on s being unused
            } catch (ConnectException e) {
                return port;
            } catch (IOException e) {
            }
        }
        return -1;
    }

    private static int getRandomPort(int low, int high) {
        return (int) (fgRandom.nextFloat() * (high - low)) + low;
    }

    /**
     * Returns a free port number on localhost, or -1 if unable to find a free port.
     *
     * @return a free port number on localhost, or -1 if unable to find a free port
     * @since 3.0
     */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
        }
        return -1;
    }
}
