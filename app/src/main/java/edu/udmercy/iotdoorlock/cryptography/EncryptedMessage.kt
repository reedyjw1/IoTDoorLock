package edu.udmercy.iotdoorlock.cryptography

data class EncryptedMessage(
    val cipherText: ByteArray,
    val encodedParams: ByteArray
) {
    // Checks if the cipher text and ecoded params match another cipher text and params (useful for validating if the implementation works)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedMessage

        if (!cipherText.contentEquals(other.cipherText)) return false
        if (!encodedParams.contentEquals(other.encodedParams)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cipherText.contentHashCode()
        result = 31 * result + encodedParams.contentHashCode()
        return result
    }
}