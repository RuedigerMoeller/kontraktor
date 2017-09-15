#!/usr/bin/env bash

export PW=$1

echo $1

mvn clean package -Dmaven.test.skip=true gpg:sign -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
cd ..
mvn package install -Dmaven.test.skip=true # rebuild fat jars

cd modules/kontraktor-bare

mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm -r */
rm *-jar-with-dep*
jar -cf bundle.jar *
cd ..
mvn package -Dmaven.test.skip=true # rebuild fat jars

cd ../kontraktor-http

mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm -r */
rm khttp.jar*
jar -cf bundle.jar *
cd ..
mvn install -Dmaven.test.skip=true # install

cd ../reactive-streams

mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
cd ..

cd ../kontraktor-reallive
mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
cd ..
mvn package install -Dmaven.test.skip=true # rebuild fat jars

cd ../kontraktor-web
mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
jar -cf bundle.jar *
cd ..

