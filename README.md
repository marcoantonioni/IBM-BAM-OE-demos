# IBM Business Automation Manager Open Editions

Demos and other stuffs

NOTE: The projects if this folder have been created using RH Quarkus distribution 

```
RH_QM_PLUGIN_VER="2.7.6.Final-redhat-00009"

mvn com.redhat.quarkus.platform:quarkus-maven-plugin:${RH_QM_PLUGIN_VER}:create \
	-DprojectGroupId=${PRJ_GROUP} \
	-DprojectArtifactId=${APP_NAME} \
	-DplatformGroupId=com.redhat.quarkus.platform \
	-DplatformVersion=${RH_QM_PLUGIN_VER}
```

## Maven local configuration

See
https://access.redhat.com/documentation/en-us/red_hat_build_of_quarkus/quarkus-2-7/guide/06259b29-78a7-4463-9368-8c7de618e784


```
# verify if settins file is in your home repo
cat ~/.m2/settings.xml

# if not present copy from maven home conf
export MAVEN_HOME=/opt/apache-maven-3.8.6
cp $MAVEN_HOME/conf/settings.xml ~/.m2/.

# update the file ~/.m2/settings.xml in <profiles> section with

<profile>
  <id>red-hat-enterprise-maven-repository</id>
  <repositories>
    <repository>
      <id>red-hat-enterprise-maven-repository</id>
      <url>https://maven.repository.redhat.com/ga/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>red-hat-enterprise-maven-repository</id>
      <url>https://maven.repository.redhat.com/ga/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
</profile>

# the in <activeprofiles> section with

<activeProfile>red-hat-enterprise-maven-repository</activeProfile>

```
