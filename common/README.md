# Common module

This module contains common components shared by `adaptive`, and `authn-policy` modules.

## Start server with `adaptive` and `authn-policy` modules

You will be able to leverage authentication policies together with the adaptive capabilities.

1. Recompile this module with this system property:
    ```shell
    mvn clean install -DskipTests -DstartAll 
    ```
2. Start server with both modules:
   ```shell
   mvn exec@start-server -DstartAll
   ```
   
The server leverages the distribution from the `authn-policy` as we rely on the custom JS files.
Then the JAR for `adaptive` module is added with a custom imported realm.