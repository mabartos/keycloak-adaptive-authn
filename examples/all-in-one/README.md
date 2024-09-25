## All-in-one example deployment

All-in-one example deployment combining Authentication policies and Adaptive capabilities.

The server leverages the distribution from the `authn-policy` as we rely on the custom JS files.

Then the JAR for `adaptive` module is added with a custom imported realm.

### Start the server

1. **Via Docker/Podman** - refer to the [container README](container/README.md)
2. **Maven** - Execute `mvn exec:exec@start-server`