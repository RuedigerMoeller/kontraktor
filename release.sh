#!/usr/bin/env bash

mvn clean package -Dmaven.test.skip=true gpg:sign
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
cd ..
mvn package -Dmaven.test.skip=true # rebuild fat jars

cd modules/kontraktor-bare

mvn clean package -Dmaven.test.skip=true gpg:sign
cd target
rm -r */
jar -cf bundle.jar *
cd ..
mvn package -Dmaven.test.skip=true # rebuild fat jars

cd ../kontraktor-http

mvn clean package -Dmaven.test.skip=true gpg:sign
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
