package com.iggydev.quicksignwizard.data.repositories

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
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature

class GenerationRepository {
    /**
     * set key generation with specifications (specific algorithms, purposes and digest)
     * setting up specifications like authorizes uses of the key, what operations are authorized, with what parameters and to what date.
     * the purpose of the keys is to be used in digital signature (sign, verify)
     */
    private fun generateDigitalSignature(): ByteArray {
        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")


        val keyPairSpec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KeyGenParameterSpec.Builder(
                Constants.alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_WRAP_KEY
            ).run {
                setDigests(KeyProperties.DIGEST_SHA256)
                build()
            }
        } else {
            KeyGenParameterSpec.Builder(
                Constants.alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).run {
                setDigests(KeyProperties.DIGEST_SHA256)
                build()
            }
        }

        // use specifications
        keyPairGenerator.initialize(keyPairSpec)

        // get keys
        val keyPair = keyPairGenerator.genKeyPair()

        // get file
        val testFile = File("C:/Users/ikupc/dev/store/glep.txt")
        val fileContent = testFile.readBytes()

        // digest it
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(fileContent)

        // embed private key in hash using elliptic curve
        val signature = Signature.getInstance("SHA256withECDSA")
        val digitalSignature = signature.apply {
            initSign(keyPair.private)
            update(digest)
        }

        // get bytes after signing
        val digitalSignatureByteArray = digitalSignature.sign()

        return digitalSignatureByteArray
    }

    private fun generateQrCode() {

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
    }

}