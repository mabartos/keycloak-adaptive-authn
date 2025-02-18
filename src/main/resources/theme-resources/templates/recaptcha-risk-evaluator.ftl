<script src="https://www.google.com/recaptcha/enterprise.js?render=${recaptchaSiteKey}"></script>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        grecaptcha.enterprise.ready(function () {
            grecaptcha.enterprise.execute('${recaptchaSiteKey}', {action: 'LOGIN'}).then(function (token) {
                const form = document.getElementById("kc-form-login");
                if (form) {
                    const input = document.createElement("input");
                    input.type = "hidden";
                    input.name = "g-recaptcha-response";
                    input.value = token;
                    form.appendChild(input);
                    form.submit();
                }
            });
        });
    });
</script>

<form id="kc-form-login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post"
      novalidate="novalidate">
</form>