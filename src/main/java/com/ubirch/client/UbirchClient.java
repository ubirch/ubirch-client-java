package com.ubirch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ubirch.crypto.GeneratorKeyFactory;
import com.ubirch.crypto.PrivKey;
import com.ubirch.crypto.PubKey;
import com.ubirch.crypto.utils.Curve;
import com.ubirch.protocol.Protocol;
import com.ubirch.protocol.ProtocolException;
import com.ubirch.protocol.ProtocolMessage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UbirchClient {

    public static void main(String[] args) {
        System.out.println("Simple UBIRCH Client");
        Map<String, String> envVars = System.getenv();

        // ===== COLLECT DATA REQUIRED ===========================================================
        String ENV = envVars.getOrDefault("UBIRCH_ENV", "dev");
        UUID serverUUID = UUID.fromString(envVars.getOrDefault("SERVER_UUID", "9d3c78ff-22f3-4441-a5d1-85c636d486ff"));
        UUID clientUUID = UUID.fromString(envVars.get("CLIENT_UUID"));
        byte[] serverKeyBytes = Base64.getDecoder().decode(envVars.getOrDefault("SERVER_PUBKEY", "okA7krya3TZbPNEv8SDQIGR/hOppg/mLxMh+D0vozWY="));
        byte[] clientKeyBytes = Base64.getDecoder().decode(envVars.get("CLIENT_KEY"));
        String authUser = envVars.get("AUTH_USER");
        String authPass = envVars.get("AUTH_PASS");

        // ===== DECODE AND SET UP KEYS =========================================================
        // Keys should be created and stored in a KeyStore for optimal security

        PubKey serverKey = null;    // server public key for verification of responses
        try {
            serverKey = GeneratorKeyFactory.getPubKey(serverKeyBytes, Curve.Ed25519);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("missing or broken SERVER_PUBKEY (base64)");
            System.exit(-1);
        }

        PrivKey clientKey = null;   // client signing key for signing messages
        try {
            clientKey = GeneratorKeyFactory.getPrivKey(clientKeyBytes, Curve.Ed25519);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("missing or broken CLIENT_KEY (base64)");
            System.exit(-1);
        }

        // region CUMULOCITY
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName("ubirch/" + authUser);
        options.setPassword(authPass.toCharArray());
        // endregion CUMULOCITY

        // create an ISO8601 DateFormat (for Cumulocity)
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        // create cumulocity client and ubirch protocol
        try {
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authUser, authPass);
            provider.setCredentials(AuthScope.ANY, credentials);
            final HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();

            // this needs to be fixed, the json key reg requires the json to be signed w/o hashing
            SimpleProtocolImpl protocol = new SimpleProtocolImpl(clientUUID, clientKey, serverUUID, serverKey);

            System.out.println("Registering public key ...");
            // register this key
            Map<String, Object> info = new HashMap<>();
            long now = System.currentTimeMillis();
            info.put("algorithm", "ECC_ED25519");
            info.put("created", df.format(now));
            info.put("hwDeviceId", clientUUID.toString());
            info.put("pubKey", Hex.decodeHex(clientKey.getRawPublicKey())); // will be changed to bytes
            info.put("pubKeyId", Hex.decodeHex(clientKey.getRawPublicKey())); // will be changed to bytes
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

            // ===========================================================================================
            // region CUMULOCITY
            final MqttClient c8yClient = getC8yClient(clientUUID, options);

            System.out.println("Sending measurement data ...");
            // create a new data value and send it
            int temp = (int) (Math.random() * 10 + 10);
            long ts = System.currentTimeMillis();

            // send message to cumulocity
            String c8yMessage = +temp + "," + df.format(ts);
            System.out.println("Sending temperature measurement (" + temp + "º) ...");
            c8yClient.publish("s/us", new MqttMessage(("211," + c8yMessage).getBytes()));
            // endregion CUMULOCITY

            // ===========================================================================================
            System.out.println("Sending UBIRCH Protocol Packet (UPP) ...");
            // send UPP to ubirch
            byte[] hash = MessageDigest.getInstance("SHA-512").digest((c8yMessage + "," + clientUUID.toString()).getBytes());
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

        } catch (MqttException | NoSuchAlgorithmException e) {
            System.err.println("error sending or connecting: " + e.getMessage());
            System.exit(-1);
        } catch (ProtocolException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
            System.err.println("UPP encoding or signature error: " + e.getMessage());
            System.exit(-1);
        } catch (IOException | DecoderException e) {
            System.err.println("can't contact ubirch servers: " + e.getMessage());
            System.exit(-1);
        }

        System.exit(0);
    }

    // region CUMULOCITY
    private static MqttClient getC8yClient(UUID clientUUID, MqttConnectOptions options) throws MqttException {
        MqttClient client;
        client = new MqttClient("tcp://mqtt.cumulocity.com", clientUUID.toString(), null);
        client.connect(options);
        // register a new device (change clientUUID.toString() to something you'd like to see)
        client.publish("s/us", ("100," + clientUUID.toString() + ",c8y_MQTTDevice").getBytes(), 2, false);
        // set device's hardware information (change to something you'd like to see)
        client.publish("s/us", ("110," + clientUUID.toString() + ",ubirch java example client,v1.0").getBytes(), 2, false);
        return client;
    }
    // endregion CUMULOCITY

    private UbirchClient() {
    }
}
