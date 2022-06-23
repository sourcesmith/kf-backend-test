# KrakenFlex Back End Test

### Prerequisites

This project produces a **Java** app targeting the latest **LTS version, 17**.
The project is a **Maven** project so a maven installation is also required.
All commands given are assumed to be run in a terminal with the current working directory being
the projects base directory.  Un*x style path separators are assumed.

The java version installed can be checked with command `java -version`.

### Running tests
Tests can be run from the project base dir with the Maven command `mvn test`.

The project follows the standard maven directory structure and the source of the tests can be
found under `src/test/java`.

Note: the tests take a little while to run since I added retries with exponential back off to the requests.

In the event of needing to regenerate the capture wiremock stubs for the client test the API key must be supplied as the
value of the key ```apiKey``` in the ```src/test/resources/api.properties``` file.

### Building and running the app.
The app is in the form of a runnable uber-jar.  This can be built by running the Maven command `mvn package` which
will also run the tests as part of the build.  Once the jar is built, it can be executed with the command
`java -jar target/kf-backend-test-1.0-SNAPSHOT.jar -a <API_KEY>` where is API_KEY is the API key to be used to authorize
requests.

A successful execution of the command will result in the output `Site outages updated.` and a `0` exit code.
The app defaults to the site ID *norwich-pear-tree*.

The source for the application itself is found under `src/main/java`.
