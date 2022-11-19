# MyKogitoPerformanceDm Project

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

### Example of Prometheus metrics

- quantiles
api_execution_elapsed_seconds{artifactId="MyKogitoPerformanceDm"}

- max wait time
http_server_requests_seconds_max{method="GET",outcome="SUCCESS",status="200",uri="/monitoring/dashboards/domain-dashboard-MyKogitoPerformanceDm_1.0.0-SNAPSHOT-PerformanceConsumeCpuForTime.json",}

http_server_requests_seconds_max{method="GET",outcome="SUCCESS",status="200",uri="/monitoring/dashboards/domain-dashboard-MyKogitoPerformanceDm_1.0.0-SNAPSHOT-PerformanceLotsOfRules.json",}

- total http requests
api_http_response_code_total{artifactId="MyKogitoPerformanceDm"}

- total http requests in last timeframe
increase(api_http_response_code_total{artifactId="MyKogitoPerformanceDm"}[1m])

- cpu by pod (use your pod name)
sum(kube_pod_resource_limit{resource='cpu',pod='my-kogito-performance-dm-5684687f84-qf8sp',namespace='my-performance-dm'})

- rules executed in time frame
increase(boolean_dmn_result_total[1m])

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
