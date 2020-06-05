#!/usr/bin/env bash

# MAC export GPG_TTY=$(tty)

export GPG_TTY=$(tty)

export PW=$1

echo $1

mvn clean package -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none gpg:sign -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
rm *stale-data.txt
jar -cf bundle.jar *
cd ..
mvn package install -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none  # rebuild fat jars

#cd modules/kontraktor-bare
#
#mvn clean package -Dmaven.test.skip=true gpg:sign  -Dgpg.passphrase=$PW
#mkdir target
#cd target
#rm -r */
#rm *-jar-with-dep*
#jar -cf bundle.jar *
#cd ..
#mvn package -Dmaven.test.skip=true # rebuild fat jars

cd modules/kontraktor-http

mvn clean package -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm -r */
rm khttp.jar*
rm *stale-data.txt
jar -cf bundle.jar *
cd ..
mvn install -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none # install

#cd ../reactive-streams
#
#mvn clean package -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none gpg:sign  -Dgpg.passphrase=$PW
#mkdir target
#cd target
#rm *-jar-with*
#rm -r */
#jar -cf bundle.jar *
#cd ..

cd ../kontraktor-reallive
mvn clean package -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none gpg:sign  -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm -r */
rm *stale-data.txt
jar -cf bundle.jar *
cd ..
mvn package install -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none # rebuild fat jars

cd ../service-suppport
mvn clean package -Dmaven.test.skip=true -DadditionalJOption=-Xdoclint:none gpg:sign -Dgpg.passphrase=$PW
mkdir target
cd target
rm *-jar-with*
rm *stale-data.txt
rm -r */
jar -cf bundle.jar *
cd ..

