# How to send data to ubirch

## Requirements

You have to set these evn vars:

    CLIENT_UUID={a valid device id}
    UBIRCH_ENV=dev
    CLIENT_KEY={a valid device private key}
    AUTH_PASS={a valid device auth password}
    
## Sending Data

Using proper env vars call SendDataUbirchClient. It should send 10 messages:

    Simple UBIRCH Client
    Sending measurement data ...
    Sending UBIRCH Protocol Packet (UPP) ...
    current hash: b0KfOu8fK/W2TlrbTb2j0K9pXWnEpZPE6b5b2AcmYXtMlFIXm/HAyByMTYcBrLCrelqQIOl8X7yIhe8+WoUG0Q==
    REQUEST: UPP(9522c4104eb5dca076394321976a505954699fa600c4406f429f3aef1f2bf5b64e5adb4dbda3d0af695d69c4a593c4e9be5bd80726617b4c9452179bf1c0c81c8c4d8701acb0ab7a5a9020e97c5fbc8885ef3e5a8506d1c440e1cdb4e4eff86806f4b12843eefdb09acdae927d983dadd4dea25cb48695a9beda24f743f3c45ed092159ea8d2bf8a072ed9d733e6287777d6439bb85245150a)
    200
    RESPONSE: UPP(9522c4109d3c78ff22f34441a5d185c636d486ff0081a76d657373616765bf796f7572207265717565737420686173206265656e207375626d6974746564c4406af8ad7375ae7723fffe19adc2a0e6c6c021c6e0b27b9bfe2f3e332d640496c4ad9b9d70f3bb90f38e916c0da3eecdfe317279999d5fe15e4857ce21db27fe08)
    Decoded and verified server response:
    ProtocolMessage(v=0x22,9d3c78ff-22f3-4441-a5d1-85c636d486ff,hint=0x00,p={"message":"your request has been submitted"},d=lSLEEJ08eP8i80RBpdGFxjbUhv8AgadtZXNzYWdlv3lvdXIgcmVxdWVzdCBoYXMgYmVlbiBzdWJtaXR0ZWQ=,s=avitc3WudyP//hmtwqDmxsAhxuCye5v+Lz4zLWQElsStm51w87uQ846RbA2j7s3+MXJ5mZ1f4V5IV84h2yf+CA==)
    Sending UBIRCH Protocol Packet (UPP) ...
    current hash: Hmu64UE8aaOVUmqeByXKFJ2NBIzi1Y+Rx1cyRgz6pje3CjPF3imnda/Um0Qs34pKWIWFEKU3sVDi+ZjTAyhCLQ==
    REQUEST: UPP(9522c4104eb5dca076394321976a505954699fa600c4401e6bbae1413c69a395526a9e0725ca149d8d048ce2d58f91c75732460cfaa637b70a33c5de29a775afd49b442cdf8a4a58858510a537b150e2f998d30328422dc44019c2b45dcaad7b45b357e418e9479949e4104739aad49d48f1e7f20d01f53a45bf9a3290cf935dd44b131a19cb5835dbb1500527f70e47acc6114c6f47f44c0b)
    200
    ...
    
## Verify Data

With a hash of a message you can verify proper processing (look for "current hash: b0KfO...) using the ubv.sh script.

Example:

    >> ./ubv.sh dev h81LH/NR/UujwokTLT/L0qHaHCj5RKV5ytUsPN7D57EzWs/3TRw64oAP9A13f11MZ7yBWAlLxEWkP9G6VhGsKQ==
    try to verify on dev this data: h81LH/NR/UujwokTLT/L0qHaHCj5RKV5ytUsPN7D57EzWs/3TRw64oAP9A13f11MZ7yBWAlLxEWkP9G6VhGsKQ==
    {
        "seal": "lSLEEE613KB2OUMhl2pQWVRpn6YAxECHzUsf81H9S6PCiRMtP8vSodocKPlEpXnK1Sw83sPnsTNaz/dNHDrigA/0DXd/XUxnvIFYCUvERaQ/0bpWEawpxECPvljyEWG0Sx0uJQfspf0HWfJffHPiWbemD+q75OPh9zRvNHg09PYpZ2/sUczEETyht3+CH4jUiFA73gdLfVwH",
        "chain": null,
        "anchors": [
            {
                "status": "added",
                "txid": "18e3c7244fc22c69308b09ffaf2b3b77a7aa45993eba1a6f98e2ae6fc7e04cfe",
                "message": "5d9a6a41ca5fb48546bb31a092e6995a6e34885afbe30c09a1d7a6e875c0ca03d29ec7642d4f75bdd5d2053e918af6da3b891c9f00ff9b8f38a2de4e50f92267",
                "blockchain": "ethereum",
                "network_info": "Rinkeby Testnet Network",
                "network_type": "testnet",
                "created": "2019-05-27T10:43:24.816808"
            },
            {
                "status": "added",
                "txid": "ZOEVKOMAAZDUVORIHYLAAIYPCXUBGVN9JTGGMZFUHWSQDSULDDRYTCTYEXJKPIFWLSX99UREISEVZ9999",
                "message": "5d9a6a41ca5fb48546bb31a092e6995a6e34885afbe30c09a1d7a6e875c0ca03d29ec7642d4f75bdd5d2053e918af6da3b891c9f00ff9b8f38a2de4e50f92267",
                "blockchain": "iota",
                "network_info": "IOTA Testnet Network",
                "network_type": "testnet",
                "created": "2019-05-27T10:43:02.355090"
            }
        ]
    }

Immediatly after sending data to the ubirch backend you should get a seal, after about one minute a least an IOTA anchor shpuld appear. 

