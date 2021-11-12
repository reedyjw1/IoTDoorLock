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

class DHKeyExchange(private val startingKey: ByteArray? = null) {

    var pubKey: PublicKey
    var privateKey: PrivateKey
    private lateinit var sharedSecret: SecretKey
    private var keyAgreement: KeyAgreement

    companion object {
        private const val TAG = "DHKeyExchange"
    }

    init {

        if (startingKey == null) {
            val keypairGen: KeyPairGenerator = KeyPairGenerator.getInstance("DH")
            keypairGen.initialize(2048)
            val keypair: KeyPair = keypairGen.generateKeyPair()
            keyAgreement = KeyAgreement.getInstance("DH")
            keyAgreement.init(keypair.private)

            pubKey = keypair.public
            privateKey = keypair.private
        } else {
            val keyFactory = KeyFactory.getInstance("DH")
            val x509KeySpec = X509EncodedKeySpec(startingKey)
            val startingPublicKey = keyFactory.generatePublic(x509KeySpec)

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

    fun setReceivedPublicKey(pb: ByteArray, length: Int? = null) {
        val keyFactory = KeyFactory.getInstance("DH")
        val x509EncodedKeySpec = X509EncodedKeySpec(pb)
        val receivedPubKey = keyFactory.generatePublic(x509EncodedKeySpec)
        keyAgreement.doPhase(receivedPubKey, true)
        generateSharedSecret()
    }

    private fun generateSharedSecret() {
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
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, sharedSecret)
        val cipherText = cipher.doFinal(msg.toByteArray())
        val encodedParams = cipher.parameters.encoded
        return EncryptedMessage(cipherText, encodedParams)
    }

    fun decrypt(encryptedMessage: EncryptedMessage): String {
        val aesParams = AlgorithmParameters.getInstance("AES")
        aesParams.init(encryptedMessage.encodedParams)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, sharedSecret, aesParams)
        val recovered = cipher.doFinal(encryptedMessage.cipherText)
        return String(recovered, Charsets.UTF_8)
    }

}