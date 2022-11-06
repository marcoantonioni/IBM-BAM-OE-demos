# MyDemoTemplate Project

## Build

Ref to https://code.quarkus.redhat.com/?extension-search=origin:platform%20kog

```
PRJ_GROUP=marco.demos.tests
APP_NAME="MyDemoTemplate"

RH_QM_PLUGIN_VER="2.7.6.Final-redhat-00009"

mvn com.redhat.quarkus.platform:quarkus-maven-plugin:${RH_QM_PLUGIN_VER}:create \
	-DprojectGroupId=${PRJ_GROUP} \
	-DprojectArtifactId=${APP_NAME} \
	-DplatformGroupId=com.redhat.quarkus.platform \
	-DplatformVersion=${RH_QM_PLUGIN_VER}

cd ${APP_NAME}
quarkus ext add org.kie.kogito:kogito-quarkus
quarkus ext add quarkus-resteasy-jackson
quarkus ext add io.quarkus:quarkus-smallrye-openapi
```

## Build & Run

```
quarkus build

quarkus dev
```

## Test

```
URL=http://localhost:8080
SERVICE_NAME=TestRule

curl -s -w"\n" -X GET ${URL}/hello

curl -s -X POST ${URL}/${SERVICE_NAME} -H 'accept: application/json' -H 'Content-Type: application/json' -d '{ "age": 16 }' | jq .
curl -s -X POST ${URL}/${SERVICE_NAME} -H 'accept: application/json' -H 'Content-Type: application/json' -d '{ "age": 25 }' | jq .

SERVICE_NAME=TestProcess
curl -s -X POST ${URL}/${SERVICE_NAME} -H 'accept: application/json' -H 'Content-Type: application/json' | jq .
```



This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/MyDemoTemplate-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
