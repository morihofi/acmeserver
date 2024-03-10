@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ProvisionerResponse
@import de.morihofi.acmeserver.certificate.acme.api.endpoints.nonAcme.serverInfo.objects.ServerInfoResponse
@import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager
@import de.morihofi.acmeserver.webui.JteLocalizer
@import io.javalin.http.Context

@param ServerInfoResponse serverInfoResponse
@param CryptoStoreManager cryptoStoreManager
@param JteLocalizer localizer
@param Context context

<!DOCTYPE html>
<html>
<head>
    @template.layout.head()

    <title>${localizer.lookup("app.name")}</title>
</head>
<body>
@template.layout.navigation(
localizer = localizer,
context = context
)

<main class="container">

    @if(serverInfoResponse.getMetadataInfo().getVersion().endsWith("-SNAPSHOT"))
        <div class="alert alert-warning" role="alert">
            <b><i class="fa-solid fa-triangle-exclamation"></i>&nbsp;${localizer.lookup("web.core.warnSnapshot.title")}</b>&nbsp;${localizer.lookup("web.core.warnSnapshot.content", localizer.lookup("app.name"))}
        </div>
    @endif

    <!-- Greeting -->
    <div class="bg-body-tertiary p-5 rounded" style="padding-top: 4.5rem;">
        <h1>${localizer.lookup("web.index.title")}</h1>
        ${localizer.lookup("web.index.intro")}<br/>
        <br/>
        <a href="/ca.crt" class="btn btn-primary btn-lg">
            <i class="fa-solid fa-download"></i>&nbsp;
            ${localizer.lookup("web.index.btn.downloadCA")}
        </a>
    </div>

    <br/>
    @if(cryptoStoreManager.isFirstRun())
        <div style="padding: 20px; border: 3px solid yellow;">
            <h2>🔑 Existing CA Certificate Upload</h2>
            <p>If you have already configured a CA certificate and want to use it within ACME Server, please use the
                button
                below
                to upload the new CA certificate.</p>
            <form action="/admin/ca-upload" method="post" enctype="multipart/form-data">
                <label for="caCertificate">CA Certificate</label>
                <input type="file" id="caCertificate" name="caCertificate" accept=".crt,.cer" required>

                <label for="caCertificateKey">CA Certificate Key</label>
                <input type="file" id="caCertificateKey" name="caCertificateKey" accept=".pem,.key" required>
                <br>
                <button type="submit">Upload CA Certificate</button>
            </form>
        </div>
    @endif

    <!-- Provisioners -->
    <h2 class="mb-3">${localizer.lookup("web.index.provisioners.title")}</h2>

    @if(!serverInfoResponse.getProvisioners().isEmpty())
        <p class="mb-4">${localizer.lookup("web.index.provisioners.intro")}</p>
        <div id="provisioner-container" class="row g-4 gap-3 mb-4">
            @for(ProvisionerResponse provisioner : serverInfoResponse.getProvisioners())
                <div class="col-md-6 col-lg-4">
                    <div class="card h-100 shadow-sm">
                        <a href="/provisioner-info?name=${provisioner.getName()}" class="position-absolute top-0 end-0 p-2 text-reset">
                            <i class="fas fa-info-circle"></i>
                        </a>

                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title">${provisioner.getName()}</h5>

                            <div class="input-group mt-3 mb-1">
                                <div class="form-floating flex-grow-1">
                                    <input type="text" class="form-control"
                                           name="acme-directory-${provisioner.getName()}"
                                           value="${cryptoStoreManager.getProvisionerForName(provisioner.getName()).getApiURL() + "/directory"}"
                                           readonly>
                                    <label for="acme-directory-${provisioner.getName()}">${localizer.lookup("web.index.provisioners.directoryUrl")}</label>
                                </div>
                                <button
                                        class="btn btn-outline-secondary"
                                        type="button"
                                        id="button-addon-${provisioner.getName()}"
                                        onclick="copy(this);"
                                ><i class="fa-solid fa-copy"></i>
                                </button>
                            </div>


                            <a href="/gethttpsforfree/?provisioner=${provisioner.getName()}"
                               class="btn btn-secondary mt-2"
                               target="_blank"
                            >
                                <i class="fas fa-globe"></i> ${localizer.lookup("web.index.provisioners.list.goToGetHttpsForFree")}
                            </a>
                        </div>
                    </div>
                </div>
            @endfor
        </div>

    @else
        <p>${localizer.lookup("web.index.provisioners.noProvisionersIntro")}</p>
    @endif


    <!-- Server Info -->
    <h2>${localizer.lookup("web.core.component.serverInfo.title")}</h2>
    @template.components.serverInfo(serverInfoResponse = serverInfoResponse, localizer = localizer)


</main>
<script>
    function copy(element) {
      const inputElement = element.parentElement.querySelector('input');

      copyTextToClipboard(inputElement.value).then(function(success) {
        element.classList.remove('btn-outline-secondary');
        if (success) {
          element.classList.add('btn-success');
        } else {
          element.classList.add('btn-danger');
        }
      }).catch(function() {
          element.classList.add('btn-danger');
      });

      setTimeout(function(){
          element.classList.add('btn-outline-secondary');
          element.classList.remove('btn-success');
          element.classList.remove('btn-danger');
      }, 750);
    }


    function copyTextToClipboard(text) {
      // Use of the Clipboard API, if available
      if (navigator.clipboard && window.isSecureContext) {
        // The Clipboard.writeText() function returns a Promise
        return navigator.clipboard.writeText(text).then(function() {
          console.log('Copying to clipboard was successful!');
          return true; // Successfully copied
        }, function(err) {
          console.error('Could not copy text: ', err);
          return false; // Copy failed
        });
      } else {
        // Fallback for browsers without clipboard API or in insecure contexts
        var textArea = document.createElement("textarea");
        Object.assign(textArea.style, {
          position: 'fixed',
          top: 0,
          left: 0,
          width: '2em',
          height: '2em',
          padding: 0,
          border: 'none',
          outline: 'none',
          boxShadow: 'none',
          background: 'transparent'
        });

        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();

        try {
          var successful = document.execCommand('copy');
          console.log('Copying text command was ' + (successful ? 'successful' : 'unsuccessful'));
          return successful;
        } catch (err) {
          console.log('Oops, unable to copy', err);
          return false;
        } finally {
          document.body.removeChild(textArea);
        }
      }
    }

</script>
@template.layout.footer(
serverInfoResponse = serverInfoResponse
)
</body>
</html>
