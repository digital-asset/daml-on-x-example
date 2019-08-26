# DAML-on-X Example Ledger Implementation

This repository contains an example [DAML
ledger](https://docs.daml.com/concepts/ledger-model/index.html) implementation
using [DAML Integration
Kit](https://docs.daml.com/daml-integration-kit/index.html).

## Prerequisites

This application assumes that postgres is installed on the machine and the
paths to corresponding utilities such as initdb and pd_ctl have been defined in the
environment.


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

## Replicating in your project

This application is meant as an example that drives integrations of DAML to 
different ledger platforms. An integration will typically replace the
ExampleInMemoryParticipantState with a dedicated implementations of ReadService 
and WriteService. These services can be implemented together or separately,
depending on the preference. It is also possible to implement the server as two
processes, one containing the ReadService + IndexerService and the other 
containing the WriteService and the IndexService.

In order to simplify deployment in a demo environment
an epheperal database is spun off at the execution time. The complexities of setting
up and cleaning up such a database are contained in a class called EphemeralPostgres.
In a proper implementation, this should be vastly simplified by simply starting a 
Postgres database outside of the ledger api server process and passing a jdbc
connection URL as a command line argument.  
