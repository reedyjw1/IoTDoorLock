package edu.udmercy.iotdoorlock.cryptography

import android.util.Log
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.interfaces.DHPublicKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.SecretKeySpec

// Class that Handles the Diffie-Hellman Key Exchange
class DHKeyExchange(private val startingKey: ByteArray? = null) {

    // Variables that should be accessible by the programmer implementing the cryptography
    var pubKey: PublicKey
    var privateKey: PrivateKey
    private lateinit var sharedSecret: SecretKey
    private var keyAgreement: KeyAgreement

    companion object {
        private const val TAG = "DHKeyExchange"
    }

    init {
        // Checks if there is a starting key, If not begin the first step of the DH Key exchange
        if (startingKey == null) {
            // Creates a new Diffie-Hellman key pair
            val keypairGen: KeyPairGenerator = KeyPairGenerator.getInstance("DH")
            keypairGen.initialize(2048)
            val keypair: KeyPair = keypairGen.generateKeyPair()
            keyAgreement = KeyAgreement.getInstance("DH")
            keyAgreement.init(keypair.private)

            // Saves the generated private and public key
            pubKey = keypair.public
            privateKey = keypair.private
        } else {
            // If staring key exists (i.e., the other client has already created a public key and private key that needs to be
            // linked to this creation

            // Creates a KeyFactory that ges the Diffie-Hellman algorithm
            val keyFactory = KeyFactory.getInstance("DH")
            // Creates a specific key spec that is needed to build a new private and public key pair based off of an existing public key
            val x509KeySpec = X509EncodedKeySpec(startingKey)

            // Creates the public key
            val startingPublicKey = keyFactory.generatePublic(x509KeySpec)

            // Creates the private and public key from a Diffie-Hellman Key spec
            val dhParam: DHParameterSpec = (startingPublicKey as DHPublicKey).params
            val keyPairGenerator = KeyPairGenerator.getInstance("DH")
            keyPairGenerator.initialize(dhParam)
            val keypair = keyPairGenerator.generateKeyPair()
            keyAgreement = KeyAgreement.getInstance("DH")
            keyAgreement.init(keypair.private)
            pubKey = keypair.public
            privateKey = keypair.private

        }
    }

    fun setReceivedPublicKey(pb: ByteArray) {
        // Takes the other client's public key, and generates the shared secret
        val keyFactory = KeyFactory.getInstance("DH")
        val x509EncodedKeySpec = X509EncodedKeySpec(pb)
        val receivedPubKey = keyFactory.generatePublic(x509EncodedKeySpec)
        keyAgreement.doPhase(receivedPubKey, true)
        generateSharedSecret()
    }

    private fun generateSharedSecret() {
        // Tries creating the shared secret
        try {
            val sharedSecretBytes = keyAgreement.generateSecret()
            sharedSecret = SecretKeySpec(sharedSecretBytes, 0, 16, "AES")
            if (startingKey == null) {
                Log.i(TAG, "generateSharedSecret: clientSharedSecret = ${sharedSecret.encoded}")
            } else {
                Log.i(TAG, "generateSharedSecret: serverSharedSecret = ${sharedSecret.encoded}")
            }

        } catch (e: Exception) {
            Log.i(TAG, "generateSharedSecret: Error=$e")
        }
    }

    fun encrypt(msg: String): EncryptedMessage {
        // Creates a cypher that uses the shared secret for encryption
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedSecret)
        // Creates the cipher text and gets the parameters needed for the other client to decrypt
        val cipherText = cipher.doFinal(msg.toByteArray())
        val encodedParams = cipher.parameters.encoded
        return EncryptedMessage(cipherText, encodedParams)
    }

    fun decrypt(encryptedMessage: EncryptedMessage): String {
        // Gets an instance of the AES algorithm
        val aesParams = AlgorithmParameters.getInstance("AES")
        // Initializes the decryption params using the encryption params
        aesParams.init(encryptedMessage.encodedParams)
        // Creates the cipher and initializes it to be used for decrypting the message
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, sharedSecret, aesParams)
        // Decrypts the message
        val recovered = cipher.doFinal(encryptedMessage.cipherText)
        return String(recovered, Charsets.UTF_8)
    }

}