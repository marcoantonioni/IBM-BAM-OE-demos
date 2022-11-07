# MyKogitoDemoDm Project

Demo for IBM Kogito Operator deployments

## How I built the application

set some vars
```
GROUP_ID=marco.demos.kogito
APP_NAME=MyKogitoDemoDm
```

create the quarkus project
```
RH_QM_PLUGIN_VER="2.7.6.Final-redhat-00009"
PRJ_GROUP=marco.demos.kogito
APP_NAME=MyKogitoDemoDm

mvn com.redhat.quarkus.platform:quarkus-maven-plugin:${RH_QM_PLUGIN_VER}:create \
	-DprojectGroupId=${PRJ_GROUP} \
	-DprojectArtifactId=${APP_NAME} \
	-DplatformGroupId=com.redhat.quarkus.platform \
	-DplatformVersion=${RH_QM_PLUGIN_VER}

cd ${APP_NAME}
quarkus ext add org.kie.kogito:kogito-quarkus
quarkus ext add quarkus-resteasy-jackson
quarkus ext add io.quarkus:quarkus-smallrye-openapi
quarkus ext add io.quarkus:quarkus-smallrye-health
```

## The DMN rule


I then defined a rule in DMN notation, the rule is extremely simple.

The purpose is to validate a request based on the age of the applicant and an amount threshold.

# Build and run

Run a build to verify the code and configuration

```
quarkus build
```

Run the app in interactive mode

```
quarkus dev
```

## Test locally

Using another shell run the following commands

<i>Note: if you don't have 'jq' tool installed remove the final piping '| jq .'</i>

```
# set your base URL
URL=http://localhost:8080

# for the interactive REST gui navigate to http://<your-host-name>/q/swagger-ui/

# do some health check
curl -X GET ${URL}/q/health -H 'accept: application/json'
curl -X GET ${URL}/q/health/live -H 'accept: application/json'
curl -X GET ${URL}/q/health/ready -H 'accept: application/json'
curl -X GET ${URL}/q/health/group -H 'accept: application/json'
curl -X GET ${URL}/q/health/group/* -H 'accept: application/json'
curl -X GET ${URL}/q/health/well -H 'accept: application/json'
curl -X GET ${URL}/q/health/started -H 'accept: application/json'

# if you want to download the open api schema doc execute
curl -LO ${URL}/q/openapi

# ping
curl -s -w"\n" -X GET ${URL}/ping

# set the service name (in this example the DMN rule service))
SERVICE_NAME=ValidateRequest

# get the rule model
curl -s -X GET -H 'accept: application/xml' ${URL}/${SERVICE_NAME}

# run a request with requestor values that will be accepted
curl -s -X POST ${URL}/${SERVICE_NAME} -H 'accept: application/json' -H 'Content-Type: application/json' -d '{ "RequestData": { "requestorName": "Marco", "requestorAge": 50, "amount": 10000 } }' | jq .

# run a request with requestor values that will be accepted
curl -s -X POST ${URL}/${SERVICE_NAME} -H 'accept: application/json' -H 'Content-Type: application/json' -d '{ "RequestData": { "requestorName": "baby", "requestorAge": 10, "amount": 10000 } }' | jq .
```

## Deploy on OpenShift cluster using IBM Kogito Operator

For deployment operations in OpenShift environment refer to the following post.

The post references two projects similar to these that were not created with the maven libraries certified by Red Hat.

To perform the deployment of this project modify the CR KogitoBuild as follow

https://community.ibm.com/community/user/automation/blogs/marco-antonioni/2022/11/05/use-ibm-kogito-operator-to-deploy-pamdm-applicatio

```
TNS="...your-namespace..."
APP_NAME="...your-app-name..."

PRJ_CTX_DIR="MyKogitoDemoDm"
PRJ="https://github.com/marcoantonioni/IBM-BAM-OE-demos"
kind: KogitoBuild
apiVersion: rhpam.kiegroup.org/v1
metadata:
  name: ${APP_NAME}
  namespace: ${TNS}
spec:
  gitSource:
    contextDir: ${PRJ_CTX_DIR}
    uri: ${PRJ}
  type: RemoteSource
```


# Useful infos
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

You can then execute your native executable with: `./target/MyKogitoDemoDm-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
