package com.iggydev.quicksignwizard.data.utilities

import android.graphics.Bitmap
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.iggydev.quicksignwizard.common.Constants
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature

class DigitalSignature {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    /**
     * set key generation with specifications (specific algorithms, purposes and digest)
     * setting up specifications like authorizes uses of the key, what operations are authorized, with what parameters and to what date.
     * the purpose of the keys is to be used in digital signature (sign, verify)
     */
    private fun generateKeyPair() {
        // get generator for keys
        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        // set specifications
        val keyPairSpecifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KeyGenParameterSpec.Builder(
                Constants.signerAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_WRAP_KEY
            )
        } else {
            KeyGenParameterSpec.Builder(
                Constants.signerAlias,
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
        keyPairGenerator.genKeyPair()
    }

    private fun generateDigitalSignature(data: ByteArray): ByteArray {

        if (!keyStore.isKeyEntry(Constants.signerAlias)) {
            generateKeyPair()
        }

        // get key entry
        val keyEntry = keyStore.getEntry(Constants.signerAlias, null) as? KeyStore.PrivateKeyEntry

        // retrieve private key
        val privateKey = keyEntry?.privateKey

        // digest a data to a fixed-sized hash value
        val messageDigestAlgorithm = MessageDigest.getInstance("SHA-256")
        val digest = messageDigestAlgorithm.digest(data)

        // sign digest with a private key
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(digest)
        }
        val digitalSignature = signature.sign()

        return digitalSignature
    }

    fun generateQrCodeCertificate(dimension: Int): ImageBitmap? {
        // generate an qr code bitmap and nest
        val qrBitmap: Bitmap?
        var qrImageBitmap: ImageBitmap? = null

        if (!keyStore.isKeyEntry(Constants.signerAlias)) {
            generateKeyPair()
        }
        // get certificate key
        val certificate = keyStore.getCertificate(Constants.signerAlias)

        // THX to PRCreeper
        // convert certificate for transmission, encoded with X.509 and then encoded to String
        val encodedCertificate =
            certificate.encoded.let { Base64.encodeToString(it, Base64.NO_WRAP) }

        val qrgEncoder = QRGEncoder(
            encodedCertificate,
            QRGContents.Type.TEXT,
            dimension
        ).apply {
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

        // encode for transmission with X.509 with string
        val encodedDigitalSignature =
            digitalSignature.let { Base64.encodeToString(it, Base64.NO_WRAP) }


        // generate qr code with signature encoded
        val qrgEncoder =
            QRGEncoder(
                encodedDigitalSignature,
                null,
                QRGContents.Type.TEXT,
                dimension
            )
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