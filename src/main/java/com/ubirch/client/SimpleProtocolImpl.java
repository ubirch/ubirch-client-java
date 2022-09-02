package com.ubirch.client;

import com.ubirch.crypto.PrivKey;
import com.ubirch.crypto.PubKey;
import com.ubirch.protocol.Protocol;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.*;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class SimpleProtocolImpl extends Protocol {
    private UUID serverUUID;
    private UUID clientUUID;
    private PubKey serverKey;
    private PrivKey clientKey;
    private byte[] lastSignature = null;

    /**
     * Initialize this simple protocol implementation. It only knows about a single signing key
     * and the server key to verify responses.
     *
     * @param clientKey this clients key bytes
     * @param serverKey this clients verifying key
     */
    public SimpleProtocolImpl(UUID clientUUID, PrivKey clientKey,
                              UUID serverUUID, PubKey serverKey) {
        this.serverKey = serverKey;
        this.clientKey = clientKey;
        this.serverUUID = serverUUID;
        this.clientUUID = clientUUID;
    }

    /**
     * Get the last stored signature. Here, after a restart an empty signature is produced.
     * If the last signature needs to survive a restart, it needs to be saved securely.
     *
     * @param uuid the uuid to get the signature for (ignored)
     * @return the last signature created by the protocol
     */
    @Override
    protected byte[] getLastSignature(UUID uuid) {
        if (lastSignature == null) {
            return new byte[64];
        } else {
            return lastSignature;
        }
    }

    /**
     * This method is here, because of a little glitch in our backend that requires us to send
     * key registration messages as JSON, which does not use the message hash as the base for the
     * signature operation, but signs the rendered json directly.
     *
     * todo: fix the key server to accept UPPv2 and remove this
     */
    public byte[] sign(UUID uuid, byte[] data) throws SignatureException, InvalidKeyException {
        if (uuid.equals(clientUUID)) {
            lastSignature = clientKey.sign(data);
            // here would be a good place to store the last generated signature before returning
            return lastSignature;
        }
        throw new InvalidKeyException(String.format("unknown uuid: %s", uuid.toString()));
    }

    @Override
    public byte[] sign(UUID uuid, byte[] data, int offset, int len) throws SignatureException, InvalidKeyException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512", BouncyCastleProvider.PROVIDER_NAME);
            digest.update(data, offset, len);
            return sign(uuid, digest.digest());
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public boolean verify(UUID uuid, byte[] data, int offset, int len, byte[] signature) throws SignatureException, InvalidKeyException {
        if (uuid.equals(serverUUID)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                digest.update(data, offset, len);
                return serverKey.verify(digest.digest(), signature);
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new SignatureException(e);
            }
        }
        throw new InvalidKeyException(String.format("unable to verify, unknown uuid: %s", uuid.toString()));
    }
}
