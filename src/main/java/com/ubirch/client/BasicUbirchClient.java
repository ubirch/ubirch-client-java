package com.ubirch.client;

import com.ubirch.crypto.PrivKey;
import com.ubirch.crypto.PubKey;
import com.ubirch.protocol.ProtocolException;
import org.apache.commons.codec.DecoderException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BasicUbirchClient extends AbtractUbirchClient {

    public static void main(String[] args) {
        System.out.println("Simple UBIRCH Client");
        Map<String, String> envVars = System.getenv();

        // ===== COLLECT DATA REQUIRED ===========================================================
        String ENV = envVars.getOrDefault("UBIRCH_ENV", "dev");
        UUID serverUUID = UUID.fromString(envVars.getOrDefault("SERVER_UUID", "9d3c78ff-22f3-4441-a5d1-85c636d486ff"));
        UUID clientUUID = UUID.fromString(envVars.get("CLIENT_UUID"));
        byte[] serverKeyBytes = Base64.getDecoder().decode(envVars.getOrDefault("SERVER_PUBKEY", "okA7krya3TZbPNEv8SDQIGR/hOppg/mLxMh+D0vozWY="));
        byte[] clientKeyBytes = Base64.getDecoder().decode(envVars.get("CLIENT_KEY"));
        String authUser = "device_" + clientUUID.toString();
        String authPass = envVars.get("AUTH_PASS");

        // ===== DECODE AND SET UP KEYS =========================================================
        // Keys should be created and stored in a KeyStore for optimal security
        Optional<PubKey> serverKey = createServerKey(serverKeyBytes);    // server public key for verification of responses
        Optional<PrivKey> clientKey = createClientKey(clientKeyBytes);   // client signing key for signing messages

        if (serverKey.isPresent() && clientKey.isPresent()) {
            // create an ISO8601 DateFormat (for Cumulocity)
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));

            // create cumulocity client and ubirch protocol
            try {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(authUser, authPass);

                CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY, credentials);

                final HttpClient client = HttpClientBuilder.create()
                        .setDefaultCredentialsProvider(provider)
                        .build();

                // this needs to be fixed, the json key reg requires the json to be signed w/o hashing
                SimpleProtocolImpl protocol = new SimpleProtocolImpl(clientUUID, clientKey.get(), serverUUID, serverKey.get());

                System.out.println("Registering public key ...");
                registerClientPubKey(ENV, clientUUID, clientKey.get(), df, client, protocol);

                // ===========================================================================================
                System.out.println("Sending measurement data ...");
                // create a new data value and send it
                int temp = (int) (Math.random() * 10 + 10);
                sendUPP(ENV, clientUUID, credentials, client, protocol, temp);

            } catch (ProtocolException | SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                System.err.println("UPP encoding or signature error: " + e.getMessage());
                System.exit(-1);
            } catch (IOException | DecoderException e) {
                System.err.println("can't contact ubirch servers: " + e.getMessage());
                System.exit(-1);
            }

            System.exit(0);
        } else {
            System.exit(-1);
        }
    }

    private BasicUbirchClient() {
    }
}
