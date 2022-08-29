package com.ubirch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ubirch.crypto.GeneratorKeyFactory;
import com.ubirch.crypto.PrivKey;
import com.ubirch.crypto.PubKey;
import com.ubirch.crypto.utils.Curve;
import com.ubirch.protocol.Protocol;
import com.ubirch.protocol.ProtocolMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

abstract class AbtractUbirchClient {

    static Optional<PubKey> createServerKey(byte[] serverKeyBytes) {
        try {
            return Optional.of(GeneratorKeyFactory.getPubKey(serverKeyBytes, Curve.Ed25519));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("missing or broken SERVER_PUBKEY (base64)");
            return Optional.empty();
        }
    }

    static Optional<PrivKey> createClientKey(byte[] clientKeyBytes) {
        try {
            return Optional.of(GeneratorKeyFactory.getPrivKey(clientKeyBytes, Curve.Ed25519));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("missing or broken CLIENT_KEY (base64)");
            return Optional.empty();
        }
    }

    static byte[] sendUPP(String ENV, UUID clientUUID, UsernamePasswordCredentials credentials, HttpClient client, SimpleProtocolImpl protocol, int temp) throws NoSuchAlgorithmException, IOException, SignatureException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        long ts = System.currentTimeMillis();

        // send message to cumulocity
        String c8yMessage = +temp + "," + df.format(ts);
        // ===========================================================================================
        System.out.println("Sending UBIRCH Protocol Packet (UPP) ...");
        // send UPP to ubirch
        byte[] hash = MessageDigest.getInstance("SHA-512").digest((c8yMessage + "," + clientUUID.toString()).getBytes());

        System.out.println("current hash: " + Base64.getEncoder().encodeToString(hash));

        ProtocolMessage pm = new ProtocolMessage(ProtocolMessage.SIGNED, clientUUID, 0x00, hash);
        byte[] upp = protocol.encodeSign(pm, Protocol.Format.MSGPACK);
        System.out.println("REQUEST: UPP(" + Hex.encodeHexString(upp) + ")");

        HttpPost postRequest = new HttpPost("https://niomon." + ENV + ".ubirch.com");
        // we need to force authentication here, httpclient4 will not send it by it's own
        String auth = Base64.getEncoder()
                .encodeToString((credentials.getUserName() + ":" + credentials.getPassword()).getBytes());
        postRequest.setHeader("Authorization", "Basic " + auth);
        postRequest.setEntity(new ByteArrayEntity(upp));
        HttpResponse response = client.execute(postRequest);

        System.out.println(response.getStatusLine().getStatusCode());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.getEntity().writeTo(baos);
        byte[] responseUPP = baos.toByteArray();
        System.out.println("RESPONSE: UPP(" + Hex.encodeHexString(responseUPP) + ")");

        System.out.println("Decoded and verified server response:");
        System.out.println(protocol.decodeVerify(responseUPP));

        return responseUPP;
    }

    static void registerClientPubKey(String ENV, UUID clientUUID, PrivKey clientKey, SimpleDateFormat df, HttpClient client, SimpleProtocolImpl protocol) throws DecoderException, SignatureException, InvalidKeyException, IOException {
        // register this key
        Map<String, Object> info = new HashMap<>();
        long now = System.currentTimeMillis();
        info.put("algorithm", "ECC_ED25519");
        info.put("created", df.format(now));
        info.put("hwDeviceId", clientUUID.toString());
        info.put("pubKey", Hex.encodeHexString(clientKey.getRawPublicKey())); // will be changed to bytes
        info.put("pubKeyId", Hex.encodeHexString(clientKey.getRawPublicKey())); // will be changed to bytes
        info.put("validNotAfter", df.format(now + 31557600000L));
        info.put("validNotBefore", df.format(now));

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        byte[] infoPacket = mapper.writeValueAsString(info).getBytes(StandardCharsets.UTF_8);
        byte[] signature = protocol.sign(clientUUID, infoPacket);
        Map<String, Object> registration = new HashMap<>();
        registration.put("pubKeyInfo", info);
        registration.put("signature", signature);

        System.out.println(mapper.writeValueAsString(registration));

        HttpPost regRequest = new HttpPost("https://key." + ENV + ".ubirch.com/api/keyService/v1/pubkey");
        regRequest.setHeader("Content-Type", "application/json");
        regRequest.setEntity(new StringEntity(mapper.writeValueAsString(registration)));
        HttpResponse regResponse = client.execute(regRequest);

        System.out.println("REGISTER: " + regResponse.getStatusLine().getStatusCode());
    }

    static MqttClient getC8yClient(UUID clientUUID, MqttConnectOptions options) throws MqttException {
        MqttClient client;
        client = new MqttClient("tcp://mqtt.cumulocity.com", clientUUID.toString(), null);
        client.connect(options);
        // register a new device (change clientUUID.toString() to something you'd like to see)
        client.publish("s/us", ("100," + clientUUID.toString() + ",c8y_MQTTDevice").getBytes(), 2, false);
        // set device's hardware information (change to something you'd like to see)
        client.publish("s/us", ("110," + clientUUID.toString() + ",ubirch java example client,v1.0").getBytes(), 2, false);
        return client;
    }

}
