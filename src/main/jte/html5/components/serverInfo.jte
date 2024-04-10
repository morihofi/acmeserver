@import de.morihofi.acmeserver.certificate.api.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.compontents.misc.CustomFormatter

@param ServerInfoResponse serverInfoResponse
@param JteLocalizer localizer


<div class="table-responsive">
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th>${localizer.lookup("web.core.table.heading.attribute")}</th>
            <th>${localizer.lookup("web.core.table.heading.value")}</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td class="icon-text">
                <i class="fa-solid fa-tag"></i>&nbsp;
                ${localizer.lookup("web.core.component.serverInfo.version")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getVersion()}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-regular fa-clock"></i>&nbsp;
                ${localizer.lookup("web.core.component.serverInfo.buildTime")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getBuildTime()}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-solid fa-code-commit"></i>&nbsp;
                ${localizer.lookup("web.core.component.serverInfo.gitCommit")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getGitCommit()}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-brands fa-java"></i>&nbsp;
                ${localizer.lookup("web.core.component.serverInfo.jvmVersion")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getJavaVersion()}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-solid fa-stopwatch"></i>
                ${localizer.lookup("web.core.component.serverInfo.jvmUptime")}
            </td>
            <td>${CustomFormatter.formatDuration(serverInfoResponse.getMetadataInfo().getJvmUptime(), localizer)}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-solid fa-hourglass"></i>
                ${localizer.lookup("web.core.component.serverInfo.startupTime")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getStartupTime() + " " + localizer.lookup("web.core.misc.time.seconds")}</td>
        </tr>
        <tr>
            <td class="icon-text">
                <i class="fa-solid fa-server"></i>&nbsp;
                ${localizer.lookup("web.core.component.serverInfo.os")}
            </td>
            <td>${serverInfoResponse.getMetadataInfo().getOperatingSystem()}</td>
        </tr>
        </tbody>
    </table>

</div>