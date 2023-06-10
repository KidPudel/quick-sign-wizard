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
import com.iggydev.quicksignwizard.common.Constants
import java.io.File
import java.security.KeyPair
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
    private fun generateKeyPair(): KeyPair {
        // get generator for keys
        val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
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
            build()
        }
        // apply specifications
        keyPairGenerator.initialize(keyPairSpecifications)

        // generate key pair
        val keyPair = keyPairGenerator.genKeyPair()

        return keyPair
    }

    private fun generateDigitalSignature(): ByteArray {

        // get key entry
        val keyEntry = keyStore.getEntry(Constants.alias, null) as? KeyStore.PrivateKeyEntry

        // retrieve private key
        val privateKey = keyEntry?.privateKey ?: generateKeyPair().private

        // digest a data to a fixed-sized hash value
        val testFile = File("C:/Users/ikupc/dev/store/glep.txt")
        val fileByteArray = testFile.readBytes()

        val messageDigestAlgorithm = MessageDigest.getInstance("SHA-256")
        val digest = messageDigestAlgorithm.digest(fileByteArray)

        // sign digest with a private key
        val signatureAlgorithm = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(digest)
        }
        val digitalSignature = signatureAlgorithm.sign()

        return digitalSignature
    }

    private fun generateQrCodePublicKey(): ImageBitmap {
        val keyPairEntry = keyStore.getEntry(Constants.alias, null) as? KeyStore.PrivateKeyEntry

        // get public key
        val publicKey = keyPairEntry?.certificate?.publicKey ?: generateKeyPair().public

        // set a generator
        val qrgEncoder = QRGEncoder(publicKey.encoded.toString(), QRGContents.Type.TEXT, 255)



        // generate an qr code bitmap and nest
        var qrBitmap: Bitmap? = null
        var qrImageBitmap: ImageBitmap? = null

        qrgEncoder.colorBlack = Color.Red.toArgb()
        qrgEncoder.colorWhite = Color.Blue.toArgb()
        try {
            qrBitmap = qrgEncoder.bitmap
            qrImageBitmap = qrBitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.v("ERROR", e.message ?: "nothing to show")
        }

        QRGSaver().save("C:/Users/ikupc/dev/store", "qr_public_glep", qrBitmap, QRGContents.ImageType.IMAGE_PNG)

        TODO(reason = "generate qr code with public key")
    }

    private fun generateQrCodeSignature() {

        var qrBitmap: Bitmap? = null
        var qrImageBitmap: ImageBitmap? = null

        val digitalSignature = generateDigitalSignature()


        // generate
        val qrgEncoder = QRGEncoder(digitalSignature.toString(), null, QRGContents.Type.TEXT, 255)
        qrgEncoder.colorBlack = Color.Red.toArgb()
        qrgEncoder.colorWhite = Color.Blue.toArgb()
        try {
            qrBitmap = qrgEncoder.bitmap
            qrImageBitmap = qrBitmap.asImageBitmap()
        } catch (e: Exception) {
            Log.v("ERROR", e.message ?: "nothing to show")
        }

        // save
        QRGSaver().apply {
            save("C:/Users/ikupc/dev/store", "qr_glep", qrBitmap, QRGContents.ImageType.IMAGE_PNG)
        }


        // TODO 1. add database
        // TODO 2. save qr code image in a database
        // TODO 3. test generation
        // TODO 4. list qr codes
    }

}