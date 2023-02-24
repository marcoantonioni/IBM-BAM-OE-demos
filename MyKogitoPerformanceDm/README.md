# MyKogitoPerformanceDm Project


## Deployment steps
```
REGISTRY_URL=registry.redhat.io
REGISTRY_USER=...your id...
REGISTRY_PWD=...your password...


TNS=my-performance-dm
BUILD_NAME=my-kogito-performance-dm

oc new-project ${TNS}
oc project ${TNS}

oc create -n ${TNS} secret docker-registry my-pull-secret --docker-server=${REGISTRY_URL} --docker-username=${REGISTRY_USER} --docker-password=${REGISTRY_PWD}
oc secrets -n ${TNS} link builder my-pull-secret --for=pull
oc secrets -n ${TNS} link default my-pull-secret --for=pull

#------------------------------------------------------------------------------------------
# Kogito Operator

OPERATOR_NAME=bamoe-kogito-operator
CSV_VER="8.0.2-1"

cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: ${OPERATOR_NAME}
  namespace: ${TNS}
spec:
  channel: 8.x
  installPlanApproval: Automatic
  name: bamoe-kogito-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
  startingCSV: bamoe-kogito-operator.${CSV_VER}
EOF

#------------------------------------------------------------------------------------------
# OperatorGroup
OG_NAME=my-kogito-operator-group

cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1
kind: OperatorGroup
metadata:
  generateName: ${OG_NAME}-
  name: ${OG_NAME}
  namespace: ${TNS}
spec:
  targetNamespaces:
  - ${TNS}
EOF

# oc get csv -n ${TNS} ... wait for Succeeded

#------------------------------------------------------------------------------------------
# Kogito Build

cat <<EOF | oc apply -f -
apiVersion: rhpam.kiegroup.org/v1
kind: KogitoBuild
metadata:
  name: ${BUILD_NAME}
  namespace: ${TNS}
spec:
  gitSource:
    contextDir: MyKogitoPerformanceDm
    uri: 'https://github.com/marcoantonioni/IBM-BAM-OE-demos'
  type: RemoteSource
EOF

# oc get build -n ${TNS} | grep "builder" ... wait for Complete

#------------------------------------------------------------------------------------------
# Kogito Runtime

cat <<EOF | oc apply -f -
apiVersion: rhpam.kiegroup.org/v1
kind: KogitoRuntime
metadata:
  name: ${BUILD_NAME}
  namespace: ${TNS}
spec:
  image: >-
    image-registry.openshift-image-registry.svc:5000/${TNS}/${BUILD_NAME}:latest
  replicas: 1
  resources:
    limits:
      cpu: '1'
    requests:
      cpu: '0.5'
  runtime: quarkus
EOF

# oc get KogitoRuntime -n ${TNS} -o yaml ... wait for Deployed

#------------------------------------------------------------------------------------------
# Service Monitor / Prometheus

cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  labels:
    app: ${BUILD_NAME}
  name: ${BUILD_NAME}
  namespace: ${TNS}
spec:
  endpoints:
  - interval: 15s
    port: http
    targetPort: 8080
    path: /q/metrics
    scheme: http
  namespaceSelector:
    matchNames:
    - ${TNS}
  selector:
    matchLabels:
      app: ${BUILD_NAME}
EOF

cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: Prometheus
metadata:
  name: prometheus
  namespace: ${TNS}
spec:
  serviceAccountName: prometheus
  serviceMonitorSelector:
    matchLabels:
      app: ${BUILD_NAME}
EOF

cat <<EOF | oc apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
EOF

# !!! prerequisite: Install Grafana
cat <<EOF | oc apply -f -
apiVersion: integreatly.org/v1alpha1
kind: Grafana
metadata:
  name: grafana-${BUILD_NAME}
  namespace: ${TNS}
spec:
  ingress:
    enabled: true
  config:
    auth:
      disable_signout_menu: true
    auth.anonymous:
      enabled: true
    log:
      level: warn
      mode: console
    security:
      admin_password: secret
      admin_user: root
  dashboardLabelSelector:
    - matchExpressions:
        - key: app
          operator: In
          values:
            - ${BUILD_NAME}
EOF

```

### Test OCP route

```
export BG_PAYLOAD="{  \"Flight List\": [    {      \"Flight Number\": \"100\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2022-01-01T07:00:00.000Z\",      \"Arrival\": \"2222-01-01T07:00:00.000Z\",      \"Capacity\": 10,      \"Status\": \"cancelled\"    },    {      \"Flight Number\": \"101\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2023-01-01T07:00:00.000Z\",      \"Arrival\": \"2223-01-01T07:00:00.000Z\",      \"Capacity\": 8,      \"Status\": \"scheduled\"    },    {      \"Flight Number\": \"102\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2023-02-01T07:00:00.000Z\",      \"Arrival\": \"2223-02-01T07:00:00.000Z\",      \"Capacity\": 5,      \"Status\": \"scheduled\"    }		  ],  \"Passenger List\": [    {      \"Name\": \"passenger1\",      \"Status\": \"bronze\",      \"Miles\": 200,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger2\",      \"Status\": \"bronze\",      \"Miles\": 80,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger3\",      \"Status\": \"bronze\",      \"Miles\": 120,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger4\",      \"Status\": \"bronze\",      \"Miles\": 160,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger5\",      \"Status\": \"bronze\",      \"Miles\": 10,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger6\",      \"Status\": \"bronze\",      \"Miles\": 300,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger7\",      \"Status\": \"silver\",      \"Miles\": 550,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger8\",      \"Status\": \"silver\",      \"Miles\": 600,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger9\",      \"Status\": \"silver\",      \"Miles\": 500,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger10\",      \"Status\": \"gold\",      \"Miles\": 1000,      \"Flight Number\": \"100\"    }		  ]}"

echo ${BG_PAYLOAD} > /tmp/payload

BUILD_NAME=my-kogito-performance-dm
SERVER_URL=http://$(oc get route ${BUILD_NAME} -o jsonpath='{.spec.host}')
curl -s -H 'accept: application/json' -H 'Content-Type: application/json' -X POST ${SERVER_URL}/FlightRebooking -d @/tmp/payload | jq .
```


## Memos

Prometheus metrics, add to pom.xml
```
    <dependency>
      <groupId>org.kie.kogito</groupId>
      <artifactId>monitoring-prometheus-quarkus-addon</artifactId>
    </dependency>

```

Create ServiceMonitor
```
TNS=my-performance-dm
MONITORED_APP_NAME=my-kogito-performance-dm
SM_NAME=monitor-${MONITORED_APP_NAME}

cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  labels:
    app: ${SM_NAME}
  name: ${SM_NAME}
  namespace: ${TNS}
spec:
  endpoints:
  - interval: 15s
    port: http
    targetPort: 8080
    path: /q/metrics
    scheme: http
  namespaceSelector:
    matchNames:
    - ${TNS}
  selector:
    matchLabels:
      app: ${MONITORED_APP_NAME}
EOF
```

```
TNS=my-performance-dm
MONITORED_APP_NAME=my-kogito-performance-dm
cat <<EOF | oc apply -f -
apiVersion: monitoring.coreos.com/v1
kind: Prometheus
metadata:
  name: prometheus
  namespace: ${TNS}
spec:
  serviceAccountName: prometheus
  serviceMonitorSelector:
    matchLabels:
      app: ${MONITORED_APP_NAME}
EOF
```

Prereq. install Grafana
```
TNS=my-performance-dm
MONITORED_APP_NAME=my-kogito-performance-dm

cat <<EOF | oc apply -f -
apiVersion: integreatly.org/v1alpha1
kind: Grafana
metadata:
  name: grafana-${MONITORED_APP_NAME}
  namespace: ${TNS}
spec:
  ingress:
    enabled: true
  config:
    auth:
      disable_signout_menu: true
    auth.anonymous:
      enabled: true
    log:
      level: warn
      mode: console
    security:
      admin_password: secret
      admin_user: root
  dashboardLabelSelector:
    - matchExpressions:
        - key: app
          operator: In
          values:
            - ${MONITORED_APP_NAME}
EOF
```

## Payload samples

### FlightRebooking

```
export BG_PAYLOAD="{  \"Flight List\": [    {      \"Flight Number\": \"100\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2022-01-01T07:00:00.000Z\",      \"Arrival\": \"2222-01-01T07:00:00.000Z\",      \"Capacity\": 10,      \"Status\": \"cancelled\"    },    {      \"Flight Number\": \"101\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2023-01-01T07:00:00.000Z\",      \"Arrival\": \"2223-01-01T07:00:00.000Z\",      \"Capacity\": 8,      \"Status\": \"scheduled\"    },    {      \"Flight Number\": \"102\",      \"From\": \"Roma\",      \"To\": \"Nowhere\",      \"Departure\": \"2023-02-01T07:00:00.000Z\",      \"Arrival\": \"2223-02-01T07:00:00.000Z\",      \"Capacity\": 5,      \"Status\": \"scheduled\"    }		  ],  \"Passenger List\": [    {      \"Name\": \"passenger1\",      \"Status\": \"bronze\",      \"Miles\": 200,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger2\",      \"Status\": \"bronze\",      \"Miles\": 80,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger3\",      \"Status\": \"bronze\",      \"Miles\": 120,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger4\",      \"Status\": \"bronze\",      \"Miles\": 160,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger5\",      \"Status\": \"bronze\",      \"Miles\": 10,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger6\",      \"Status\": \"bronze\",      \"Miles\": 300,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger7\",      \"Status\": \"silver\",      \"Miles\": 550,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger8\",      \"Status\": \"silver\",      \"Miles\": 600,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger9\",      \"Status\": \"silver\",      \"Miles\": 500,      \"Flight Number\": \"100\"    },    {      \"Name\": \"passenger10\",      \"Status\": \"gold\",      \"Miles\": 1000,      \"Flight Number\": \"100\"    }		  ]}"
echo ${BG_PAYLOAD} > ./payload
curl -s -H 'accept: application/json' -H 'Content-Type: application/json' -X POST http://localhost:8080/FlightRebooking -d @./payload | jq .
```



```
{
  "Flight List": [
    {
      "Flight Number": "100",
      "From": "Roma",
      "To": "Nowhere",
      "Departure": "2022-01-01T07:00:00.000Z",
      "Arrival": "2222-01-01T07:00:00.000Z",
      "Capacity": 10,
      "Status": "cancelled"
    },
    {
      "Flight Number": "101",
      "From": "Roma",
      "To": "Nowhere",
      "Departure": "2023-01-01T07:00:00.000Z",
      "Arrival": "2223-01-01T07:00:00.000Z",
      "Capacity": 8,
      "Status": "scheduled"
    },
    {
      "Flight Number": "102",
      "From": "Roma",
      "To": "Nowhere",
      "Departure": "2023-02-01T07:00:00.000Z",
      "Arrival": "2223-02-01T07:00:00.000Z",
      "Capacity": 5,
      "Status": "scheduled"
    }
		
  ],
  "Passenger List": [
    {
      "Name": "passenger1",
      "Status": "bronze",
      "Miles": 200,
      "Flight Number": "100"
    },
    {
      "Name": "passenger2",
      "Status": "bronze",
      "Miles": 80,
      "Flight Number": "100"
    },
    {
      "Name": "passenger3",
      "Status": "bronze",
      "Miles": 120,
      "Flight Number": "100"
    },
    {
      "Name": "passenger4",
      "Status": "bronze",
      "Miles": 160,
      "Flight Number": "100"
    },
    {
      "Name": "passenger5",
      "Status": "bronze",
      "Miles": 10,
      "Flight Number": "100"
    },
    {
      "Name": "passenger6",
      "Status": "bronze",
      "Miles": 300,
      "Flight Number": "100"
    },
    {
      "Name": "passenger7",
      "Status": "silver",
      "Miles": 550,
      "Flight Number": "100"
    },
    {
      "Name": "passenger8",
      "Status": "silver",
      "Miles": 600,
      "Flight Number": "100"
    },
    {
      "Name": "passenger9",
      "Status": "silver",
      "Miles": 500,
      "Flight Number": "100"
    },
    {
      "Name": "passenger10",
      "Status": "gold",
      "Miles": 1000,
      "Flight Number": "100"
    }
		
  ]
}
```

### Example of Prometheus metrics

#### quantiles

```
api_execution_elapsed_seconds{artifactId="MyKogitoPerformanceDm"}
```

last minute
```
increase(api_execution_elapsed_seconds{artifactId="MyKogitoPerformanceDm"}[1m])
```

#### max wait time

```
http_server_requests_seconds_max{method="GET",outcome="SUCCESS",status="200",uri="/monitoring/dashboards/domain-dashboard-MyKogitoPerformanceDm_1.0.0-SNAPSHOT-PerformanceConsumeCpuForTime.json",}
```

```
http_server_requests_seconds_max{method="GET",outcome="SUCCESS",status="200",uri="/monitoring/dashboards/domain-dashboard-MyKogitoPerformanceDm_1.0.0-SNAPSHOT-PerformanceLotsOfRules.json",}
```

#### total http requests

```
api_http_response_code_total{artifactId="MyKogitoPerformanceDm"}
```

#### total http requests in last timeframe

```
increase(api_http_response_code_total{artifactId="MyKogitoPerformanceDm"}[1m])
```

#### cpu by pod (use your pod name)

```
sum(kube_pod_resource_limit{resource='cpu',pod='my-kogito-performance-dm-5684687f84-qf8sp',namespace='my-performance-dm'})
```

#### rules executed in time frame (breakdown of rules invocations)

```
increase(boolean_dmn_result_total[1m])
```


## Example of posts

Local test

http://localhost:8080/q/metrics


### post PerformanceAdult
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceAdult' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "age": 20
}'
```

### post PerformanceConsumeCpu
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceConsumeCpu' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "loops": 100000,
  "innerLoops": 50000
}'
```

### post PerformanceConsumeCpuForTime
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceConsumeCpuForTime' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "milliseconds": 100
}'
```

### post PerformanceDriver
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceDriver' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "age": 20,
  "driveLicense": true,
  "violations": 4
}'
```

### post PerformanceSuspendDriveLicense
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceSuspendDriveLicense' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "actualViolations": 5,
  "newViolation": 1,
  "violationThisYear": 2
}'
```

### post PerformanceLotsOfRules
```
curl -X 'POST' \
  'http://localhost:8080/PerformanceLotsOfRules' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "Lev1Input1": true,
  "Lev1Input2": 5,
  "Lev1Input3": true,
  "Lev2Input1": "GOOD",
  "Lev2Input2": true,
  "Lev2Input3": 20,
  "Lev2Input4": "HIGH"
}'
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

You can then execute your native executable with: `./target/MyKogitoPerformanceDm-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)
