@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.handler.LoginUiHandler
@import de.morihofi.acmeserver.webui.handler.LoginUiHandler.LOGIN_STATE
@import io.javalin.http.Context
@import io.javalin.http.HandlerType

@param JteLocalizer localizer
@param Context context
@param LOGIN_STATE state


<!DOCTYPE html>
<html>
<head>
    @template.layout.head()

    <title>${localizer.lookup("app.name")}</title>
    <style>
        html,
body {
  height: 100%;
}

body {
  display: flex;
  align-items: center;
  padding-top: 40px;
  padding-bottom: 40px;
  background-color: #f5f5f5;
}

.form-signin {
  max-width: 330px;
  padding: 15px;
}

.form-signin .form-floating:focus-within {
  z-index: 2;
}

.form-signin input[type="email"] {
  margin-bottom: -1px;
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}

.form-signin input[type="password"] {
  margin-bottom: 10px;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}



    </style>
</head>
<body class="text-center">

<main class="form-signin w-100 m-auto">
    @if(context.queryParam("redir") != null || context.method() == HandlerType.POST)

        @if(state == LOGIN_STATE.ENTER_DATA)
            <form method="post" action="${context.path()}">
                <h1 class="h1 mb-4 fw-normal">${localizer.lookup("app.name")}</h1>

                <h1 class="h3 mb-3 fw-normal">${localizer.lookup("web.login.form.signInTitle")}</h1>

                <div class="form-floating  mb-3">
                    <input type="email" name="email" class="form-control" id="floatingInput"
                           placeholder="name@example.com">
                    <label for="floatingInput">${localizer.lookup("web.login.form.email")}</label>
                </div>

                <input type="hidden" name="redir" value="${context.queryParam("redir")}">

                <button class="w-100 btn btn-lg btn-primary" type="submit"><i
                            class="fa-solid fa-arrow-right-to-bracket"></i>&nbsp;${localizer.lookup("web.login.form.login")}
                </button>
                <p class="mt-5 mb-3 text-muted">&copy; ${localizer.lookup("app.name")}</p>


                <div class="alert alert-primary" role="alert">
                    <b>${localizer.lookup("web.login.info.title")}</b> ${localizer.lookup("web.login.info.content")}
                </div>

            </form>
        @elseif(state == LOGIN_STATE.SUC_EMAIL_SENT)
            <div class="alert alert-success" role="alert">
                <b>${localizer.lookup("web.login.success.title")}</b> ${localizer.lookup("web.login.success.content")}
            </div>
        @elseif(state == LOGIN_STATE.ERR_NO_USER_FOUND)
            <div class="alert alert-danger mb-3" role="alert">
                <b>${localizer.lookup("web.login.error.title")}</b> ${localizer.lookup("web.login.error.content")}
            </div>
            <a href="javascript:history.back();" class="w-100 btn btn-lg btn-primary" type="submit">
                <i class="fa-solid fa-chevron-left"></i>&nbsp;${localizer.lookup("web.login.button.goBack")}
            </a>
        @endif

    @else
        <%-- Error, cause no redirect was given --%>
        <div class="alert alert-danger mb-3" role="alert">
            <b>Error:</b> Missing redirect
        </div>

    @endif

</main>


@template.layout.footer(
showFooter = false
)
</body>
</html>