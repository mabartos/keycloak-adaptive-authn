## Build image

In order to build your own image for the both modules `adaptive`, and `authn-policy`, execute this command:

```shell
podman build -f Containerfile -t keycloak-adaptive-all ../
```

Then, you can run the image by executing this command:

```shell
podman run -p 8080:8080 localhost/keycloak-adaptive-all start
```