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
import com.umeng.commonsdk.UMConfigure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingApplicationPackageName
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

object ChaoxingDeviceInfoHelper {
    private const val DEVICE_INFO_PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC79d8Ot0hCbxxSISC6x8SCwTBspFSzlLKHJUYqoFNu1TSRaw4hEYkOnvEaL1VyoxV6HXcDrzwYvaFZaZaPQPFnfCHZy5dQwxcmifgSHqS+oKXw40Ys4cVIqnU5d90S7EWSRdBglX489jlqVaNcQSkDx2TYmC+DbAq9FV/BU09ISQIDAQAB"
    private const val RSA_PLAIN_BLOCK_SIZE = 117
    private const val DEVICE_FLAG_INFO_KEY = "QrCbNY@MuK1X8HGw"
    private val INVALID_UNIQUE_ID_REGEX = Regex("^0{16,64}$")

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
                requirePredictable(block.size > 2 && block[0] == 0.toByte() && block[1] == 1.toByte())
                val separator = block.indexOf(0.toByte(), 2)
                requirePredictable(separator > 2)
                output.write(block, separator + 1, block.size - separator - 1)
            }
            JSONObject.parseObject(output.toString(Charsets.UTF_8.name()))
        }.getOrNull()

    @SuppressLint("GetInstance")
    suspend fun getLocalMachineDeviceCode(context: Context): String {
        val uniqueId = getLocalMachineUniqueId(context)
        if (uniqueId.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(DEVICE_FLAG_INFO_KEY.toByteArray(Charsets.UTF_8), "AES")
        )
        return Base64.encodeToString(
            cipher.doFinal(uniqueId.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    suspend fun getCachedLocalMachineDeviceCode(context: Context): String {
        context.chaoxingDataStore.data.first().loginSession.deviceCode.takeIf { it.isNotEmpty() }
            ?.let { return it }
        val generatedDeviceCode = getLocalMachineDeviceCode(context)
        if (generatedDeviceCode.isEmpty()) return ""
        return context.chaoxingDataStore.updateData { currentDataStore ->
            if (currentDataStore.loginSession.deviceCode.isNotEmpty()) {
                currentDataStore
            } else {
                currentDataStore.toBuilder().setLoginSession(
                    currentDataStore.loginSession.toBuilder()
                        .setDeviceCode(generatedDeviceCode)
                        .setIsNotRandomizedDeviceCode(true)
                        .build()
                ).build()
            }
        }.loginSession.deviceCode
    }

    fun randomizedDeviceCode(): String {
        val rawData = MessageDigest.getInstance("SHA-256").digest(
            (UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString()
                .replace("-", "")).toByteArray()
        )
        return Base64.encodeToString(rawData + rawData, Base64.NO_WRAP)
    }

    private suspend fun getLocalMachineUniqueId(context: Context): String {
        val uniqueId = runCatching {
            withTimeout(1.seconds) {
                suspendCancellableCoroutine { continuation ->
                    UMConfigure.getOaid(context.applicationContext) { deviceId ->
                        if (continuation.isActive) continuation.resume(deviceId.orEmpty())
                    }
                }
            }
        }.getOrElse {
            return ""
        }
        return if (uniqueId.isNotBlank() &&
            !INVALID_UNIQUE_ID_REGEX.matches(uniqueId.replace("-", ""))
        ) {
            uniqueId
        } else {
            ""
        }
    }

    @SuppressLint("HardwareIds")
    private fun buildDeviceInfo(context: Context): JSONObject {
        val packageInfo = runCatching {
            val packageInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    chaoxingApplicationPackageName,
                    PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    chaoxingApplicationPackageName,
                    packageInfoFlags
                )
            }
        }.getOrNull()
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
            .fluentPut("app_ver", packageInfo?.versionName ?: "6.7.5")
            .fluentPut(
                "versionCode",
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode?.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toString()
                }) ?: "10941"
            )
            .fluentPut(
                "signatures",
                packageInfo?.let { getSignatureDigest(it) }
                    ?: "1e27068798b6697821abbeb44a17da5483c4fb7fad7b9ce7890465ed04d0cbe0"
            )
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
            MediaDrm(widevineUuid).use { mediaDrm ->
                Base64.encodeToString(
                    mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID),
                    Base64.NO_WRAP
                )
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
