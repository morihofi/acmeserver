/*
 * Copyright (c) 2024 Moritz Hofmann <info@morihofi.de>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.morihofi.acmeserver.webui.handler;

import de.morihofi.acmeserver.database.Database;
import de.morihofi.acmeserver.database.objects.ACMEAccount;
import de.morihofi.acmeserver.tools.certificate.cryptoops.CryptoStoreManager;
import de.morihofi.acmeserver.webui.WebUI;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginUiHandler implements Handler {
    private final CryptoStoreManager cryptoStoreManager;

    public enum LOGIN_STATE {
        ENTER_DATA, ERR_NO_USER_FOUND, SUC_EMAIL_SENT
    }

    public LoginUiHandler(CryptoStoreManager cryptoStoreManager) {
        this.cryptoStoreManager = cryptoStoreManager;
    }

    @Override
    public void handle(@NotNull Context context) throws Exception {

        Map<String, Object> params = new HashMap<>(WebUI.getDefaultFrontendMap(cryptoStoreManager, context));

        if (context.method() == HandlerType.GET) {
            params.put("state", LOGIN_STATE.ENTER_DATA);
            context.render("pages/login.jte", params);
            return;
        }


        if (context.method() == HandlerType.POST) {


            String redirect = context.queryParam("redir");
            String email = context.queryParam("email");

            List<ACMEAccount> acmeAccountList = Database.getAllACMEAccountsForEmail(email);
            LOGIN_STATE loginState;
            if(acmeAccountList.isEmpty()){
                loginState = LOGIN_STATE.ERR_NO_USER_FOUND;
            }else {
                loginState = LOGIN_STATE.SUC_EMAIL_SENT;

                // Create user

            }

            params.put("state", loginState);
            context.render("pages/login.jte", params);
        }


    }
}
