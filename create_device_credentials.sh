#! /bin/bash

if [[ "$1"x == ""x || "$2"x == ""x ]]; then
    echo "usage: $0 <uuid> <bootstrap password>"
    exit -1
fi

# waits until the devices has been accepted and prints the device credentials
while (true); do
    curl -f -XPOST \
        -u "devicebootstrap:$2" \
        --data "{\"id\":\"$1\"}" \
        -H 'Content-Type: application/vnd.com.nsn.cumulocity.deviceCredentials+json' \
        -H 'Accept: application/vnd.com.nsn.cumulocity.deviceCredentials+json' \
        https://ubirch.cumulocity.com/devicecontrol/deviceCredentials && exit
    sleep 5
done
