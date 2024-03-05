@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.WebUI
@import io.javalin.http.Context
@import de.morihofi.acmeserver.webui.WebUI.FRONTEND_ADMIN_PAGES

@param JteLocalizer localizer
@param Context context

<div class="d-flex flex-column flex-shrink-0 p-3 text-bg-dark vh-100" style="width: 280px;">
    <a href="/" class="d-flex align-items-center mb-3 mb-md-0 me-md-auto text-white text-decoration-none">
        <span class="fs-4">ACME Server</span>
    </a>
    <hr>
    <ul class="nav nav-pills flex-column mb-auto">
        @for(FRONTEND_ADMIN_PAGES page : FRONTEND_ADMIN_PAGES.values())
        <li class="nav-item">
            <a href="${page.getRoute()}" class="nav-link @if(context.path().equals(page.getRoute())) active @endif" aria-current="page">
                <i class="${page.getIconClass()}"></i>
                ${localizer.lookup(page.getTranslationKey())}
            </a>
        </li>
        @endfor
    </ul>
    <hr>
    <div class="dropdown">
        <a href="#" class="d-flex align-items-center text-white text-decoration-none dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
            <img src="https://github.com/mdo.png" alt="" width="32" height="32" class="rounded-circle me-2">
            <strong>Administrator</strong>
        </a>
        <ul class="dropdown-menu dropdown-menu-dark text-small shadow" style="">
            <li><a class="dropdown-item" href="#"><i class="me-2 fa-solid fa-sliders"></i> Settings</a></li>
            <li><hr class="dropdown-divider"></li>
            <li><a class="dropdown-item" href="#"><i class="me-2 fa-solid fa-arrow-right-from-bracket"></i> Sign out</a></li>
        </ul>
    </div>
</div>
