@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.webui.JteLocalizer

@param boolean showFooter = true
@param ServerInfoResponse serverInfoResponse = null
@param JteLocalizer localizer

@if(showFooter)
    <div class="container">
        <footer class="d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top">
            <div class="col-md-4 d-flex align-items-center">
                <span class="mb-3 mb-md-0 text-muted">Made with ❤️ by morihofi</span>
            </div>

            @if(serverInfoResponse != null)
                <div class="col-md-4 d-flex align-items-center justify-content-center">
                    <span class="mb-3 mb-md-0 text-muted">Version ${serverInfoResponse.getMetadataInfo().getVersion()}</span>
                    @if(serverInfoResponse.getMetadataInfo().getUpdate().isUpdateAvailable())&nbsp;<a href="${serverInfoResponse.getMetadataInfo().getUpdate().getReleasesUrl()}" target="_blank"><span class="badge bg-info text-dark"><i class="fa-solid fa-up-long"></i>&nbsp;${localizer.lookup("web.update.updateAvailable")}</span></a>@endif
                </div>
            @endif

            <div class="col-md-4 d-flex align-items-center justify-content-end">
                <span class="mb-3 mb-md-0 text-muted">
                    <a class="text-muted" target="_blank" href="https://github.com/morihofi/acmeserver">
                        <i class="fa-brands fa-github"></i>
                    </a>
                </span>

            </div>

        </footer>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.6/dist/umd/popper.min.js"
            integrity="sha384-oBqDVmMz9ATKxIep9tiCxS/Z9fNfEXiDAYTujMAeBAsjFuCZSmKbSSUnQlmh/jp3"
            crossorigin="anonymous"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.min.js"
            integrity="sha384-cuYeSxntonz0PPNlHhBs68uyIAVpIIOZZ5JqeqvYYIcEL727kskC66kF92t6Xl2V"
            crossorigin="anonymous"></script>
@endif
<script>
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl)
    });
</script>