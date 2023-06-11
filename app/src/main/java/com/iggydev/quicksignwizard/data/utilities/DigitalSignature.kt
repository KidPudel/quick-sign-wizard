package com.iggydev.quicksignwizard.data.utilities

import android.graphics.Bitmap
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidmads.library.qrgenearator.QRGSaver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import com.iggydev.quicksignwizard.common.Constants
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey

class DigitalSignature {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    /**
     * set key generation with specifications (specific algorithms, purposes and digest)
     * setting up specifications like authorizes uses of the key, what operations are authorized, with what parameters and to what date.
     * the purpose of the keys is to be used in digital signature (sign, verify)
     */
    private fun generateKeyPair(): KeyPair {
        // get generator for keys
        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        // set specifications
        val keyPairSpecifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KeyGenParameterSpec.Builder(
                Constants.alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_WRAP_KEY
            )
        } else {
            KeyGenParameterSpec.Builder(
                Constants.alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
        }.run {
            // digest set with which the keys can be used
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            build()
        }
        // apply specifications
        keyPairGenerator.initialize(keyPairSpecifications)

        // generate key pair
        val keyPair = keyPairGenerator.genKeyPair()

        return keyPair
    }

    private fun generateDigitalSignature(data: ByteArray): ByteArray {
        // get key entry
        val keyEntry = keyStore.getEntry(Constants.alias, null) as? KeyStore.PrivateKeyEntry

        // retrieve private key
        val privateKey = keyEntry?.privateKey ?: generateKeyPair().private

        // digest a data to a fixed-sized hash value
        val messageDigestAlgorithm = MessageDigest.getInstance("SHA-256")
        val digest = messageDigestAlgorithm.digest(data)

        // sign digest with a private key
        val signatureAlgorithm = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(digest)
        }
        val digitalSignature = signatureAlgorithm.sign()

        return digitalSignature
    }

    fun generateQrCodePublicKey(dimension: Int): ImageBitmap? {

        // generate an qr code bitmap and nest
        val qrBitmap: Bitmap?
        var qrImageBitmap: ImageBitmap? = null

        val keyPairEntry = keyStore.getEntry(Constants.alias, null) as? KeyStore.PrivateKeyEntry

        // get public key
        val publicKey = keyPairEntry?.certificate?.publicKey ?: generateKeyPair().public

        // set a generator
        val qrgEncoder = QRGEncoder(publicKey.encoded.toString(), QRGContents.Type.TEXT, dimension)
            .apply {
                colorBlack = Color.White.toArgb()
                colorWhite = Color.Black.toArgb()
            }


        try {
            qrBitmap = qrgEncoder.bitmap
            qrImageBitmap = qrBitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.v("ERROR", e.message ?: "nothing to show")
        }

        return qrImageBitmap
    }

    fun generateQrCodeSignature(dimension: Int, data: ByteArray): ImageBitmap? {

        val qrBitmap: Bitmap?
        var qrImageBitmap: ImageBitmap? = null

        val digitalSignature = generateDigitalSignature(data = data)


        // generate
        val qrgEncoder = QRGEncoder(digitalSignature.toString(), null, QRGContents.Type.TEXT, dimension)
        qrgEncoder.colorBlack = Color.White.toArgb()
        qrgEncoder.colorWhite = Color.Black.toArgb()
        try {
            qrBitmap = qrgEncoder.bitmap
            qrImageBitmap = qrBitmap.asImageBitmap()
        } catch (e: Exception) {
            Log.v("ERROR", e.message ?: "nothing to show")
        }

        return qrImageBitmap
    }

}