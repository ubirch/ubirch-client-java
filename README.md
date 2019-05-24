# UBIRCH Client Java

A Java example how to create a device, register keys and send data.

**WORK IN PROGRESS**

## Creating a new device in Cumulocity

> If you'd like to try, you don't have to do this part. Contact us with a UUID and we will provide you with a set of credentials to access the UBIRCH API. 

1. create a UUID (`uuidgen`)
2. in Cumulocity, register a new device using that UUID
3. run the script `create_device_credentials.sh <PWD> <UUID>` using the bootstrap password
4. accept the device in Cumulocity and wait for the script to exit
5. write down the device `username` and `password`

## Running the Client

Set up the environment variables required:

* `UBIRCH_ENV` - (**`dev`**, `demo`, `prod`)
* `AUTH_USER` - the user name from above
* `AUTH_PASS` - the password from above
* `CLIENT_UUID` - the uuid created above
* `CLIENT_KEY` - base64 encoded signing key (Ed25519)
----
* `SERVER_UUID` - (**optional**) the corresponding backend 
* `SERVER_PUBKEY` - (**optional**) base64 encoded public key
UUID
 
Run `UbirchClient` which sends one message to Cumulocity and a UPP of the data to
the configured UBIRCH backend.

```
java -jar target/ubirch-client-java-1.0.0-SNAPSHOT-jar-with-dependencies.jar

```