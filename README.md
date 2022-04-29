## Install the Validator in the Mule Runtime

The corresponding JAR should be added under the lib folder within the mule-agent-plugin, which is contained in the server-plugins folder of the Mule instance.

For example, $MULE_HOME/server-plugins/mule-agent-plugin/lib/mule-agent-jar-signature-validator.jar.

## Export the Master Password environment variable

```
export AGENT_VAR_master_password=myPassword
```

## Encrypt the Validator secrets

```
$MULE_HOME/bin/amc_setup --encrypt my-truststore-password

Mule Agent Installer
-----------------------------


	INFO: The encrypted value to paste on the mule-agent.yml file is: '![PBEWITHSHA1ANDDESEDE,wFE1D5V4DMb0uG77mzU+gibrlmnj3Kzb]'
```

## Mule Agent JAR Signature Validator Configuration

In the following configuration, we are going to implement rules for the hello validator.

File: $MULE_HOME/conf/mule-agent.yml

```
  mule.agent.artifact.validator.service:
    enabled: true
    validators:
    - type: jarSignatureValidator
      name: defaultJarSignatureValidator
      enabled: true
      args:
        truststore: signers-truststore.jks
        truststoreType: JKS
        truststorePassword: '![PBEWITHSHA1ANDDESEDE,U/nA0wV714oNi9LHoAkDtiwg22ngG6b7yFTtg7fk2d4=]'
```

### Log4j Configuration

File: $MULE_HOME/conf/log4j2.xml

```
 <AsyncLogger name="org.example.agent" level="INFO"/>
```

### Test the Jar Signature Validator

#### Use Case: Deploy an unsigned application

Deploy an application that has an invalid name.

Command

```
curl -X PUT 'http://localhost:9999/mule/applications/app-01' \
-H 'Content-Type: application/json' \
-d '{
  "url": "file:/tmp/app-unsigned.jar",
  "configuration": {
    "mule.agent.application.properties.service": {
      "applicationName": "app-01",
      "properties": {
          "business": "account"
      }
    }
  }
}'
```

Response

```
{
    "type": "class com.example.agent.exceptions.SignatureVerificationFailedException",
    "message": "Artifact is unsigned. (signatures missing or not parsable)"
}
```

Logs

```
INFO  2022-04-28 09:42:32,412 [qtp1517795516-100] [processor: ; event: ] com.mulesoft.agent.services.application.AgentApplicationService: Deploying the app-02 application from URL file:/tmp/signedjar/app-unsigned.jar
INFO  2022-04-28 09:42:32,431 [qtp1517795516-100] [processor: ; event: ] com.example.agent.MuleAgentJarSignatureValidator: Loading Jar Signers Truststore from '/private/tmp/trash/server-01/conf/signers-truststore.jks'
ERROR 2022-04-28 09:42:32,452 [qtp1517795516-100] [processor: ; event: ] com.example.agent.MuleAgentJarSignatureValidator: Artifact is unsigned. (signatures missing or not parsable)
ERROR 2022-04-28 09:42:32,460 [qtp1517795516-100] [processor: ; event: ] com.mulesoft.agent.external.handlers.deployment.ApplicationsRequestHandler: Error performing the deployment of app-02. Cause: SignatureVerificationFailedException: Artifact is unsigned. (signatures missing or not parsable)
```

#### Use Case: Deploy an application signed by a trusted signer

Deploy an application that has an invalid name.

Command

```
curl -X PUT 'http://localhost:9999/mule/applications/app-02' \
-H 'Content-Type: application/json' \
-d '{
  "url": "file:/tmp/signedjar/app-signed-by-trust-signer.jar",
  "configuration": {
    "mule.agent.application.properties.service": {
      "applicationName": "app-02",
      "properties": {
          "business": "finance"
      }
    }
  }
}'
```

Response

```
{
    "application": {
        "name": "app-02"
    },
    "status": "Deployment attempt started"
}
```

Logs

```
INFO  2022-04-28 09:47:01,479 [qtp1517795516-108] [processor: ; event: ] com.mulesoft.agent.services.application.AgentApplicationService: Deploying the app-02 application from URL file:/tmp/signedjar/app-signed-by-trust-signer.jar
INFO  2022-04-28 09:47:01,493 [qtp1517795516-108] [processor: ; event: ] com.example.agent.MuleAgentJarSignatureValidator: Loading Jar Signers Truststore from '/private/tmp/trash/server-01/conf/signers-truststore.jks'
INFO  2022-04-28 09:47:01,559 [qtp1517795516-108] [processor: ; event: ] com.mulesoft.agent.services.artifactvalidator.AgentApplicationValidatorService: Application validation success for application: 'app-02'.
```

#### Use Case: Deploy an application signed by an untrusted signer

Deploy an application that has an invalid name.

Command

```
curl -X PUT 'http://localhost:9999/mule/applications/app-03' \
-H 'Content-Type: application/json' \
-d '{
  "url": "file:/tmp/signedjar/app-signed-by-untrust-signer.jar",
  "configuration": {
    "mule.agent.application.properties.service": {
      "applicationName": "app-03",
      "properties": {
          "business": "finance"
      }
    }
  }
}'
```

Response

```
{
    "type": "class com.example.agent.exceptions.SignatureVerificationFailedException",
    "message": "Signature validation failed."
}
```

Logs

```
INFO  2022-04-28 09:52:04,678 [qtp1517795516-37] [processor: ; event: ] com.mulesoft.agent.services.application.AgentApplicationService: Deploying the app-03 application from URL file:/tmp/signedjar/app-signed-by-untrusted-signer.jar
INFO  2022-04-28 09:52:04,687 [qtp1517795516-37] [processor: ; event: ] com.example.agent.MuleAgentJarSignatureValidator: Loading Jar Signers Truststore from '/private/tmp/trash/server-01/conf/signers-truststore.jks'
ERROR 2022-04-28 09:52:04,734 [qtp1517795516-37] [processor: ; event: ] com.example.agent.MuleAgentJarSignatureValidator: Signature validation failed.
ERROR 2022-04-28 09:52:04,734 [qtp1517795516-37] [processor: ; event: ] com.mulesoft.agent.external.handlers.deployment.ApplicationsRequestHandler: Error performing the deployment of app-03. Cause: SignatureVerificationFailedException: Signature validation failed.
```