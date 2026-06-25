<#-- Keycloak login background only. Spinner while FingerprintJS runs, then auto-submit. -->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false displayRequiredFields=false; section>
    <#if section = "header">
        <style>
            #kc-header,
            #kc-page-title,
            .pf-v5-c-login__main-header,
            .pf-v5-c-login__main-footer {
                display: none !important;
            }

            .pf-v5-c-login__container {
                grid-template-areas: "main" !important;
                grid-template-columns: 1fr !important;
                min-height: 100vh;
            }

            .pf-v5-c-login__main {
                background: transparent !important;
                box-shadow: none !important;
                border: none !important;
                padding: 0 !important;
                margin: 0 !important;
            }

            .pf-v5-c-login__main-body {
                padding: 0 !important;
            }
        </style>
    <#elseif section = "form">
        <link rel="modulepreload" href="${url.resourcesPath}/js/fingerprintjs-v5.min.js">

        <form id="kc-form-login" action="${url.loginAction}" method="post" novalidate="novalidate" hidden>
            <input type="hidden" name="device-visitor-id" id="device-visitor-id" value="">
        </form>

        <div id="kc-fp-loading" aria-live="polite" aria-busy="true"
             style="position:fixed;inset:0;display:flex;align-items:center;justify-content:center">
            <svg class="pf-v5-c-spinner pf-m-xl" role="progressbar" aria-valuetext="Loading..." viewBox="0 0 100 100" aria-label="Loading">
                <circle class="pf-v5-c-spinner__path" cx="50" cy="50" r="45" fill="none"></circle>
            </svg>
        </div>

        <script>
            (function () {
                function submitForm(visitorId) {
                    var form = document.getElementById("kc-form-login");
                    var input = document.getElementById("device-visitor-id");
                    if (!form || !input) {
                        return;
                    }
                    input.value = visitorId || "";
                    form.submit();
                }

                import("${url.resourcesPath}/js/fingerprintjs-v5.min.js")
                    .then(function (m) {
                        return m.load({ monitoring: false });
                    })
                    .then(function (fp) {
                        return fp.get();
                    })
                    .then(function (result) {
                        submitForm(result.visitorId);
                    })
                    .catch(function () {
                        submitForm("");
                    });
            })();
        </script>

        <noscript>
            <form action="${url.loginAction}" method="post"
                  style="position:fixed;inset:0;display:flex;align-items:center;justify-content:center">
                <input type="hidden" name="device-visitor-id" value="">
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!}">
                    ${msg("doContinue")}
                </button>
            </form>
        </noscript>
    </#if>
</@layout.registrationLayout>
