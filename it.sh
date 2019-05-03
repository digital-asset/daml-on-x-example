#!/usr/bin/env bash

set -euo pipefail

curl -L 'https://bintray.com/api/v1/content/digitalassetsdk/DigitalAssetSDK/com/daml/ledger/testtool/ledger-api-test-tool_2.12/$latest/ledger-api-test-tool_2.12-$latest.jar?bt_package=sdk-components' \
     -o ledger-api-test-tool.jar
java -jar ledger-api-test-tool.jar --extract || true # mask incorrect error code of the tool: https://github.com/digital-asset/daml/pull/889
java -jar target/scala-2.12/damlonx-example.jar --port=6865 SemanticTests.dar & serverPid=$!
while ! timeout 1 bash -c "echo > /dev/tcp/localhost/6865"; do
		sleep 1
done
java -jar ledger-api-test-tool.jar -h localhost -p 6865
kill $serverPid
wait $serverPid || true # mask SIGTERM error code we should get here.
