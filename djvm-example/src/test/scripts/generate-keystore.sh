#!/bin/sh

EC_ALIAS=ec
RSA_ALIAS=rsa
KEYPASS=deterministic
STOREPASS=deterministic

rm -f keystore.pkcs12

keytool -keystore keystore.pkcs12 -storetype pkcs12 -genkey -dname 'CN=localhost, O=R3, L=London, C=UK' -keyalg RSA -keysize 4096 -validity 3650 -keypass ${KEYPASS} -storepass ${STOREPASS} -alias ${RSA_ALIAS}

keytool -keystore keystore.pkcs12 -storetype pkcs12 -genkey -dname 'CN=localhost, O=R3, L=London, C=UK' -keyalg EC -keysize 256 -validity 3650 -keypass ${KEYPASS} -storepass ${STOREPASS} -alias ${EC_ALIAS}
