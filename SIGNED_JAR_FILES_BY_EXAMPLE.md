## Signed Jar File by Example

### 1. Generate the trusted code signer keystore (Self signed certificate)

The following command generates a new keystore file: trusted-code-signer.jks

```
keytool \
-keystore trusted-code-signer.jks \
-keypass mulesoft \
-storepass mulesoft \
-genkey \
-keyalg RSA \
-keysize 2048 \
-keypass mulesoft \
-noprompt \
-alias trusted-code-signer-alias \
-dname "CN=Trusted Code Signer, OU=Security, O=ACME, L=San Francisco, S=CA, C=US"
```

### 2. Generate the untrusted code signer keystore

The following command create a new keystore file: untrusted-code-signer.jks

```
keytool \
-keystore untrusted-code-signer.jks \
-keypass mulesoft \
-storepass mulesoft \
-genkey \
-keyalg RSA \
-keysize 2048 \
-keypass mulesoft \
-noprompt \
-alias untrusted-code-signer-alias \
-dname "CN=Untrusted Code Signer, OU=Security, O=ACME, L=San Francisco, S=CA, C=US"
```

### 3. Create the signers truststore file


#### 3.1 Export the self signed certificate from trusted-code-signer.jks

Output file: trusted-code-signer.pem

```
keytool \
-export \
-alias trusted-code-signer-alias \
-keystore trusted-code-signer.jks \
-storepass mulesoft \
-storetype JKS \
-rfc \
-file trusted-code-signer.pem
```

#### 3.2 Import the exported certificate into the signer truststore

Output file: signers-truststore.jks

```
keytool \
-importcert \
-file trusted-code-signer.pem \
-keystore signers-truststore.jks \
-alias code-signer-alias \
-storepass mulesoft \
-storetype JKS
```

### 4. Sign the mule application file with jarsigner tool

The following command generates signs the file `app-unsigned.jar` using the keystore `trusted-code-signer.jks`

Output File: app-signed-by-trust-signer.jar

```
jarsigner app-unsigned.jar \
-keystore trusted-code-signer.jks \
-storepass mulesoft \
-storetype JKS \
-signedjar app-signed-by-trust-signer.jar \
trusted-code-signer-alias
```

### 5. Jar file verification

The following command validates the file `app-signed-by-trust-signer.jar` was signed with one of the certificates in the truststore `signers-truststore.jks`

```
jarsigner \
-verify \
-verbose \
-keystore signers-truststore.jks \
app-signed-by-trust-signer.jar
```