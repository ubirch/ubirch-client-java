#!/bin/bash

function usage {
    echo "$0 {dev|demo|prod} {hash}"
}

ENV=$1
if [[ -z "$ENV" ]];then 
	usage
	exit 1
else
	shift
fi

HASH=$1
if [[ -z "$ENV" ]];then 
	usage
	exit 1
else
	shift
fi

URL="https://verify.$ENV.ubirch.com/api/verify"
CURL="/usr/bin/curl"

echo "try to verify on $ENV this data: $HASH"
#echo "using $URL"

RESULT=`curl -s -X POST -H "accept: application/json" -H "Content-Type: text/plain" -d "$HASH" "$URL"`

if [[ -z "$RESULT" ]]; then
	echo "validation failt"
else
	echo "$RESULT"|python -mjson.tool
fi

