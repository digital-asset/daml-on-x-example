#!/usr/bin/env bash
# Copyright (c) 2020 The DAML Authors. All rights reserved.
# SPDX-License-Identifier: Apache-2.0


set -euo pipefail

# NOTE: This file is used by `make it` in a context where the example ledger
# server has already been built. It is not intended to be used directly.

echo "Detecting current DAML SDK version used in the SBT build..."
sdkVersion=$(sbt --error 'set showSuccess := false'  printSdkVersion)
# sdkVersion=$(cat build.sbt| egrep -o "sdkVersion.*=.*\".*\"" | perl -pe 's|sdkVersion.*?=.*?"(.*?)"|\1|')
echo "Detected SDK version is $sdkVersion"

dest="target/ledger-api-test-tool.jar"

devSdkVersion="100.0.0"
if [ "$sdkVersion" = "$devSdkVersion" ] ; then
  echo "Using the local build of the DAML Integration kit Ledger API Test Tool"
  cp "${HOME}/.m2/repository/com/daml/ledger/testtool/ledger-api-test-tool/${devSdkVersion}/ledger-api-test-tool-${devSdkVersion}.jar" ${dest}
else
  echo "Downloading DAML Integration kit Ledger API Test Tool version ${sdkVersion}..."
  curl -f -L "https://repo.maven.apache.org/maven2/com/daml/ledger-api-test-tool/${sdkVersion}/ledger-api-test-tool-${sdkVersion}.jar" \
       -o ${dest}
fi

readonly OSNAME="$(uname -s)"
if [ "$OSNAME" = "Linux" ] ; then
  export PATH=$PATH:`echo /usr/lib/postgresql/*/bin`
fi

echo "Extracting the .dar file to load in example server..."
cd target && java -jar ledger-api-test-tool.jar --extract || true # mask incorrect error code of the tool: https://github.com/digital-asset/daml/pull/889
# back to prior working directory
cd ../

echo "Launching damlonx-example server..."
java -jar target/scala-2.12/damlonx-example.jar --jdbc-url="jdbc:h2:mem:daml_on_x_example;db_close_delay=-1;db_close_on_exit=false" --port="6865" target/SemanticTests.dar target/Test-dev.dar target/Test-stable.dar & serverPid=$!
echo "Waiting for the server to start"
#crude sleep that will work cross platform
sleep 20
echo "damlonx-example server started"
echo "Launching the test tool..."
java -jar target/ledger-api-test-tool.jar localhost:6865 --all-tests --timeout-scale-factor 3.5
echo "Test tool run is complete."
echo "Killing the server..."
kill $serverPid
wait $serverPid || true # mask SIGTERM error code we should get here.

