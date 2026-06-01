/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import android.util.Base64
import com.alibaba.fastjson2.JSONObject
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingApplicationPackageName
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher

object ChaoxingDeviceInfoHelper {
    private const val DEVICE_INFO_PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC79d8Ot0hCbxxSISC6x8SCwTBspFSzlLKHJUYqoFNu1TSRaw4hEYkOnvEaL1VyoxV6HXcDrzwYvaFZaZaPQPFnfCHZy5dQwxcmifgSHqS+oKXw40Ys4cVIqnU5d90S7EWSRdBglX489jlqVaNcQSkDx2TYmC+DbAq9FV/BU09ISQIDAQAB"
    private const val RSA_PLAIN_BLOCK_SIZE = 117

    fun buildEncryptedDeviceInfo(context: Context): String =
        encryptByRsa(buildDeviceInfo(context).toJSONString().toByteArray(Charsets.UTF_8))

    fun decryptClientId(clientId: String): JSONObject? =
        runCatching {
            val encrypted = Base64.decode(clientId, Base64.DEFAULT)
            val publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(
                    X509EncodedKeySpec(
                        Base64.decode(DEVICE_INFO_PUBLIC_KEY, Base64.DEFAULT)
                    )
                ) as RSAPublicKey
            val blockSize = (publicKey.modulus.bitLength() + 7) / 8
            val output = ByteArrayOutputStream()
            for (offset in encrypted.indices step blockSize) {
                val block = BigInteger(
                    1,
                    encrypted.copyOfRange(offset, offset + blockSize)
                ).modPow(publicKey.publicExponent, publicKey.modulus)
                    .toByteArray()
                    .toFixedBlock(blockSize)
                require(block.size > 2 && block[0] == 0.toByte() && block[1] == 1.toByte())
                val separator = block.indexOf(0.toByte(), 2)
                require(separator > 2)
                output.write(block, separator + 1, block.size - separator - 1)
            }
            JSONObject.parseObject(output.toString(Charsets.UTF_8.name()))
        }.getOrNull()

    @SuppressLint("HardwareIds")
    private fun buildDeviceInfo(context: Context): JSONObject {
        val packageInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                chaoxingApplicationPackageName,
                PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(chaoxingApplicationPackageName, packageInfoFlags)
        }
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val deviceUniqueId =
            sha256("${chaoxingApplicationPackageName}:$androidId:${Build.FINGERPRINT}")
        val metrics = context.resources.displayMetrics

        return JSONObject()
            .fluentPut("deviceUniqueId", deviceUniqueId)
            .fluentPut("cdid", deviceUniqueId)
            .fluentPut("device_id", deviceUniqueId)
            .fluentPut("android_id", androidId)
            .fluentPut("mediaDrmId", getMediaDrmId())
            .fluentPut("oaid", "")
            .fluentPut("platform", "android")
            .fluentPut("os_name", "android")
            .fluentPut("os_ver", Build.VERSION.RELEASE.orEmpty())
            .fluentPut("os_lang", Locale.getDefault().toLanguageTag())
            .fluentPut("brand", Build.BRAND.orEmpty())
            .fluentPut("board", Build.BOARD.orEmpty())
            .fluentPut("hardware", Build.HARDWARE.orEmpty())
            .fluentPut("model", Build.MODEL.orEmpty())
            .fluentPut("cpu_ar", Build.SUPPORTED_ABIS.joinToString(","))
            .fluentPut("app_name", context.packageName)
            .fluentPut("app_ver", packageInfo.versionName.orEmpty())
            .fluentPut(
                "versionCode",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                }
            )
            .fluentPut("signatures", getSignatureDigest(packageInfo))
            .fluentPut("resolution", "${metrics.widthPixels}*${metrics.heightPixels}")
            .fluentPut("dpi", metrics.density.toString())
//            .fluentPut("densityDpi", metrics.densityDpi.toString())
            .fluentPut("time_stamp", System.currentTimeMillis())
    }

    private fun encryptByRsa(plain: ByteArray): String {
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(DEVICE_INFO_PUBLIC_KEY, Base64.DEFAULT))
        )
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val output = ByteArrayOutputStream()
        for (offset in plain.indices step RSA_PLAIN_BLOCK_SIZE) {
            val size = minOf(RSA_PLAIN_BLOCK_SIZE, plain.size - offset)
            output.write(cipher.doFinal(plain, offset, size))
        }
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun getMediaDrmId(): String =
        runCatching {
            val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            val mediaDrm = MediaDrm(widevineUuid)
            try {
                Base64.encodeToString(
                    mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID),
                    Base64.NO_WRAP
                )
            } finally {
                @Suppress("DEPRECATION")
                mediaDrm.release()
            }
        }.getOrDefault("")

    private fun getSignatureDigest(packageInfo: PackageInfo): String {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return ""
        return signatures.joinToString(",") { signature ->
            sha256(signature.toByteArray())
        }
    }

    private fun ByteArray.toFixedBlock(size: Int): ByteArray =
        when {
            this.size == size -> this
            this.size == size + 1 && this[0] == 0.toByte() -> copyOfRange(1, this.size)
            this.size < size -> ByteArray(size - this.size) + this
            else -> error("Invalid RSA block size")
        }

    private fun ByteArray.indexOf(value: Byte, startIndex: Int): Int {
        for (index in startIndex until size) {
            if (this[index] == value) return index
        }
        return -1
    }

    private fun sha256(value: String): String =
        sha256(value.toByteArray(Charsets.UTF_8))

    private fun sha256(value: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(value)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
