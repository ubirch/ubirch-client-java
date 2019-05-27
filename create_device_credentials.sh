#! /bin/bash

if [[ "$1"x == ""x || "$2"x == ""x ]]; then
    echo "usage: $0 <uuid> <bootstrap password>"
    exit -1
fi

read -r -d '' DEVICEREG << EOM
{
  "name": "$1",
  "type": "DEVICE",
  "c8y_IsDevice":{},
  "c8y_Hardware":{
    "revision": "v1.0",
    "serialNumber": "$1"
  }
}
EOM

echo -n "Waiting for device registration "
result=""
# waits until the devices has been accepted and prints the device credentials
while (true); do
    result=$(
        curl -f --silent -XPOST \
            -u "devicebootstrap:$2" \
            --data "{\"id\":\"$1\"}" \
            -H 'Content-Type: application/vnd.com.nsn.cumulocity.deviceCredentials+json' \
            -H 'Accept: application/vnd.com.nsn.cumulocity.deviceCredentials+json' \
            https://ubirch.cumulocity.com/devicecontrol/deviceCredentials
    )
    if [[ "$?" == "0" ]]; then
        echo "Device has been registered!"
        echo $result
        auth=$(echo $result | jq -r '(.username + ":" + .password)')
        id=$(curl --silent -XPOST -u "$auth" \
            -H 'Content-Type: application/vnd.com.nsn.cumulocity.managedObject+json' \
            -H 'Accept: application/vnd.com.nsn.cumulocity.managedObject+json' \
            --data "$DEVICEREG" \
             https://ubirch.cumulocity.com/inventory/managedObjects | jq -r .id)
        echo $id
        curl --silent -XPOST -u "$auth" \
            -H 'Content-Type: application/vnd.com.nsn.cumulocity.externalId+json' \
            -H 'Accept: application/vnd.com.nsn.cumulocity.externalId+json' \
            --data "{\"type\":\"c8y_Serial\",\"externalId\":\"$1\"}" \
             https://ubirch.cumulocity.com/identity/globalIds/$id/externalIds
        exit
    fi
    echo -n "."
    sleep 5
done