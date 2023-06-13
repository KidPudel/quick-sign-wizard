package com.iggydev.quicksignwizard.data.utilities

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.iggydev.quicksignwizard.common.Constants
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class DigitalSignatureTest {
    @Test
    fun decodedCertificateEqualsEncoded() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }


        val keyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        // set specifications
        val keyPairSpecifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KeyGenParameterSpec.Builder(
                "test_certificate",
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
        keyPairGenerator.genKeyPair()


        // get certificate key
        val certificate = keyStore.getCertificate("test_certificate")

        // THX to PRCreeper
        // convert public key for transmission, encoded with X.509 and then encoded to String
        val encodedCertificate = certificate?.encoded.let { Base64.encodeToString(it, Base64.NO_WRAP) }

        // decode data
        val decodedData = Base64.decode(encodedCertificate, Base64.NO_WRAP)

        // decode certificate
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val inputStream = ByteArrayInputStream(decodedData)
        val restoredCertificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

        println("initial: $certificate restored: $restoredCertificate")
        assertThat(restoredCertificate.toString()).isEqualTo(certificate.toString())

    }
}