package com.chromia.cli.util

import com.google.gson.Gson
import net.postchain.crypto.KeyPair
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


object ChromiaKeyPairStorage {
    private val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    private val cipher = Cipher.getInstance("AES")
    private val gson = Gson()

    //TODO: This is not as secure as it should be. It should be a random input vector for each new encryption
    private val inputVector = getSecureRand()

    fun encryptAndPersist(keyPair: KeyPair, password: String, file: File) {
        val randomSalt = getSecureRand()
        val secretKey = getDerivedKey(password, randomSalt)

        //TODO: Verify correct length in GCMParameterSpec. PS: We have now removed GCMParameterSpec
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedKeyPair = cipher.doFinal(gson.toJson(keyPair).toByteArray())

        //TODO: This doesn't seem secure. One will always get a hold on the salt size
        val outputStream = DataOutputStream(FileOutputStream(file))
        outputStream.writeInt(randomSalt.size)
        outputStream.write(randomSalt)
        outputStream.writeInt(encryptedKeyPair.size)
        outputStream.write(encryptedKeyPair)
        outputStream.close()
    }

    fun loadAndDecrypt(password: String, file: File): KeyPair? {
        val inputStream = DataInputStream(FileInputStream(file))
        val saltSize = inputStream.readInt()
        val salt = ByteArray(saltSize)
        inputStream.readFully(salt)
        val encryptedSize = inputStream.readInt()
        val encryptedBytes = ByteArray(encryptedSize)
        inputStream.readFully(encryptedBytes)
        inputStream.close()

        val secretKey = getDerivedKey(password, salt)

        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        val decryptedData = String(decryptedBytes)

        return gson.fromJson(decryptedData, KeyPair::class.java)
    }


    private fun getDerivedKey(pwd: String, salt: ByteArray): SecretKey {
        //TODO: Verify correct set of values
        val pBKDF2Iterations = 300000
        val keyLength = 256

        val key = factory.generateSecret(PBEKeySpec(pwd.toCharArray(), salt, pBKDF2Iterations, keyLength))
        return SecretKeySpec(key.encoded, "AES")
    }

    private fun getSecureRand(): ByteArray {
        val rnd = ByteArray(256)
        SecureRandom().nextBytes(rnd)
        return rnd
    }
}

