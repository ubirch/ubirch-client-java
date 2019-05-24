#! /bin/bash

if [[ "$1"x == ""x || "$2"x == ""x ]]; then
    echo "usage: $0 <uuid> <bootstrap password>"
    exit -1
fi

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
        id=$(echo $result | jq -r .id)
        echo "ID  : $id"
        auth=$(echo $result | jq -r '(.username + ":" + .password)')
        time=$(date --utc +%FT%T.%3NZ)
        #echo "AUTH: $auth"
        curl -f --silent -XPOST -u "$auth" \
            -H 'Content-Type: application/vnd.com.nsn.cumulocity.managedObject+json' \
            -H 'Accept: application/vnd.com.nsn.cumulocity.managedObject+json' \
            --data "{\"name\":\"$1\",\"type\":\"TESTDEVICE\",\"c8y_IsDevice\":{}}" \
             https://ubirch.cumulocity.com/inventory/managedObjects | jq -r .name
        exit
    fi
    echo -n "."
    sleep 5
done