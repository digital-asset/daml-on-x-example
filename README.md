# DAML-on-X Example Ledger Implementation

This repository contains an example [DAML
ledger](https://docs.daml.com/concepts/ledger-model/index.html) implementation
using [DAML Integration
Kit](https://docs.daml.com/daml-integration-kit/index.html).

## Development

This is a Scala / sbt project. To compile it run:

    sbt compile

To run it execute:

    sbt run

Eventually, this will launch a server, which exposes [Ledger
API](https://docs.daml.com/app-dev/ledger-api-introduction/index.html) and
implements a DAML ledger. It listens on port 6865 by default.
