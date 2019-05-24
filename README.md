# DAML-on-X Example Ledger Implementation

This repository contains an example [DAML
ledger](https://docs.daml.com/concepts/ledger-model/index.html) implementation
using [DAML Integration
Kit](https://docs.daml.com/daml-integration-kit/index.html).

## Usage

To run it execute:

    sbt assembly

This will create a fat jar, which can be ran with

    java -jar target/scala-2.12/damlonx-example.jar

This will launch a server, which exposes [Ledger
API](https://docs.daml.com/app-dev/ledger-api-introduction/index.html) and
implements a DAML ledger. It listens on port 6865 by default.

If you have a `.dar` file, you can run the server with it loaded in with:

    java -jar target/scala-2.12/damlonx-example.jar --port=6865 Iou.dar

See https://docs.daml.com/getting-started/quickstart.html#run-the-application-using-prototyping-tools
for details how to obtain a `.dar` file.

## Development

This is a standard Scala / sbt project. To compile it run:

    sbt compile

It uses [library artifacts from DAML Integration
Kit](https://docs.daml.com/daml-integration-kit/index.html#library-infrastructure-overview)
like `com.daml.ledger.participant-state` and others.

## Testing

The DAML Integration kit describes how to test the conformance of your ledger
server
[here](https://docs.daml.com/daml-integration-kit/index.html#integration-kit-testing).

You can test the server in this example as per that approach by running

    make it
