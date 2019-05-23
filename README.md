# UBIRCH Client Java

A Java example how to create a device, register keys and send data.

**WORK IN PROGRESS**

## Creating a new device in Cumulocity

1. create a UUID (`uuidgen`)
2. in Cumulocity, register a new device using that UUID
3. run the script `create_device_credentials.sh <PWD> <UUID>` using the bootstrap password
4. accept the device in Cumulocity and wait for the script to exit
5. write down the device `username` and `password`

## Running the Client

Set up the environment variables required:


* `ENV` - (`dev`, `demo`, `prod`)
* `SERVER_UUID` - the corresponding backend UUID
* `CLIENT_UUID` - the uuid created above
* `SERVER_PUBKEY` - base64 encoded public key
* `CLIENT_KEY` - base64 encoded signing key (Ed25519)
* `C8Y_USER` - the user name from above
* `C8Y_PASS` - the password from above
 
Run `UbirchClient` which sends one message to Cumulocity and a UPP of the data to
the configured UBIRCH backend.