@import de.morihofi.acmeserver.webui.JteLocalizer
@import io.javalin.http.Context
@import de.morihofi.acmeserver.webui.WebUI.FRONTEND_PAGES

@param JteLocalizer localizer
@param Context context


<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">${localizer.lookup("app.name")}</a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent"
                aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarSupportedContent">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                @for(FRONTEND_PAGES page : FRONTEND_PAGES.values())
                
                <li class="nav-item">
                    <a class="nav-link @if(context.path().equals(page.getRoute())) active @endif" aria-current="page" href="${page.getRoute()}">
                        <i class="${page.getIconClass()}"></i>&nbsp;
                        ${localizer.lookup(page.getTranslationKey())}
                    </a>
                </li>
                @endfor
            </ul>
            <ul class="d-flex">
                <a href="/admin" class="btn btn-outline-secondary" type="submit">
                    <i class="fa-solid fa-arrow-right-to-bracket"></i>&nbsp;
                    ${localizer.lookup("web.core.menu.adminLogin")}
                </a>
            </ul>
        </div>
    </div>
</nav>
