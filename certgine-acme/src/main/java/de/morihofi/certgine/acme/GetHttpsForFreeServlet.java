package de.morihofi.certgine.acme;

import de.morihofi.certgine.server.common.intf.AbstractStaticServlet;

public class GetHttpsForFreeServlet extends AbstractStaticServlet {
    public static final String PATH_MOUNT = "/gethttpsforfree/*";

    @Override
    protected String getBasePath() {
        return "/gethttpsforfree"; // Path to the webapp directory in current classpath containing the static files
    }

    @Override
    public String getMountPath() {
        return PATH_MOUNT;
    }
}
