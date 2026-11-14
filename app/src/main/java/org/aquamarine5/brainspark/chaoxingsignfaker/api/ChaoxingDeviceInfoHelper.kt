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
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingClientInfo
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
    private const val UA_SCHILD_SALT = "ipL\$TkeiEmfy1gTXb2XHrdLN0a@7c^vu"
    private const val UA_CHANNEL_KALIMDOR = "@Kalimdor"
    private const val UA_CHANNEL_AZEROTH = "@Azeroth"
    private val UA_DEVICE_SEGMENT = Regex("\\(device:[^)]*\\)")
    private val UA_BUILD_MODEL_SEGMENT = Regex("(Android [^;)]*;\\s*)[^;)]*(\\s+Build/)")

    fun buildEncryptedDeviceInfo(context: Context, deviceModel: String? = null): String =
        encryptByRsa(
            buildDeviceInfo(context, deviceModel).toJSONString().toByteArray(Charsets.UTF_8)
        )

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
    private fun buildDeviceInfo(context: Context, deviceModel: String? = null): JSONObject {
        val packageInfo = runCatching {
            val packageInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    chaoxingClientInfo.packageName,
                    PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    chaoxingClientInfo.packageName,
                    packageInfoFlags
                )
            }
        }.getOrNull()
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val deviceUniqueId =
            sha256("${chaoxingClientInfo.packageName}:$androidId:${Build.FINGERPRINT}")
        val metrics = context.resources.displayMetrics
        val resolvedModel = deviceModel?.takeIf { it.isNotEmpty() }
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
            .fluentPut("model", resolvedModel ?: Build.MODEL.orEmpty())
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

    private val vivo = listOf(
        "V2241A",
        "V2241HA",
        "V2242A",
        "V2227A",
        "V2309A",
        "V2324A",
        "V2359A",
        "V2324HA",
        "V2366GA",
        "V2415A",
        "V2405A",
        "V2419A",
        "V2458A",
        "V2454A",
        "V2509A",
        "V2609A",
        "V2244A",
        "V2283A",
        "V2323A",
        "V2364A",
        "V2429A",
        "V2430A",
        "V2465A",
        "V2571A",
        "V2140A",
        "V2168A",
        "V2180A",
        "V2236A",
        "V2317A",
        "V2559A",
        "V2559BA",
        "V2278A",
        "V2271A",
        "V2279A",
        "V2313A",
        "V2354A",
        "V2343A",
        "V2361GA",
        "V2410A",
        "V2456A",
        "V2506A",
        "V2516A",
        "V2607A",
        "V2561A",
        "V2553A",
        "V2344A",
    )

    private val iqoo = listOf(
        "V2171A",
        "V2217A",
        "V2243A",
        "V2304A",
        "V2307A",
        "V2408A",
        "V2505A",
        "V2546A",
        "V2564A",
        "V2606A",
        "V2231A",
        "V2301A",
        "V2338A",
        "V2403A",
        "V2425A",
        "V2463A",
        "V2520A",
        "V2573A",
        "V2618A",
        "V2270A",
        "V2314A",
        "V2361A",
        "V2417A",
        "V2452A",
        "V2507A",
        "V2536A",
        "V2551A",
        "V2603A",
        "V2329A",
    )

    private val huawei = listOf(
        "CET-AL00",
        "CET-AL60",
        "DCO-AL00",
        "BRA-AL00",
        "ALN-AL00",
        "ALN-AL10",
        "CLS-AL00",
        "PLR-AL00",
        "PLA-AL10",
        "LNA-AL00",
        "MNA-AL00",
        "ADY-AL00",
        "HBN-AL00",
        "HBN-AL10",
        "HBP-AL00",
        "NCO-AL00",
        "GLA-AL00",
        "FOA-AL00",
        "GOA-AL80",
        "GOA-AL80U",
        "BLK-AL00",
        "ADA-AL00",
        "ADA-AL00U",
        "BLK-AL80",
        "MIS-AL00",
    )

    private val honor = listOf(
        "LGE-AN00",
        "LGE-AN10",
        "PGT-AN00",
        "PGT-AN10",
        "BVL-AN00",
        "BVL-AN16",
        "PTP-AN00",
        "PTP-AN10",
        "BKQ-AN00",
        "BKQ-AN10",
        "LDY-AN00",
        "MLY-AN00",
        "WKL-AN20",
        "FNE-AN00",
        "SDY-AN00",
        "HPB-TN00",
        "ANP-AN00",
        "AGT-AN00",
        "REA-AN00",
        "REP-AN00",
        "MAG-AN00",
        "MAA-AN00",
        "MAA-AN10",
        "ELI-AN00",
        "AMM-AN00",
        "DNN-AN00",
        "DNP-AN00",
        "MEY-AN00",
        "VKJ-AN00",
        "FAA-AN00",
        "FAS-AN00",
        "FAP-AN00",
        "PPG-AN00",
        "ADT-AN00",
        "ALI-AN00",
        "ALP-AN00",
        "BRC-AN00",
        "BRP-AN00",
        "AGI-AN00",
        "MTN-AN00",
        "BSN-AN00",
    )

    private val oppo = listOf(
        "PFEM10",
        "PGEM10",
        "PHZ110",
        "PHY110",
        "PKB110",
        "PKC110",
        "PKT110",
        "PLB110",
        "PKJ110",
        "PLJ110",
        "PLG110",
        "PME110",
        "PMA110",
        "PMW110",
        "PMX110",
        "PGBM10",
        "PHW110",
        "PJJ110",
        "PJV110",
        "PKM110",
        "PLA110",
        "PYK110",
        "PADM00",
        "PBAM00",
        "PESM10",
        "PFTM20",
        "PHJ110",
        "PFUM10",
        "PHA120",
        "PFTM10",
        "PJB110",
        "PJG110",
        "PKA110",
        "PJY110",
        "PKQ110",
        "PKP110",
        "PLS120",
        "PLL110",
        "PLN110",
        "PLT120",
        "PMT110",
        "PYE110",
        "PYC110",
        "PGJM10",
        "PGIM10",
        "PJC110",
        "PJR110",
        "PKS110",
        "PLD110",
        "PLM110",
        "PLE110",
        "PYD110",
        "PMH110",
        "PMG110",
        "PYE130",
        "PGFM10",
        "PJH110",
        "PJW110",
    )

    private val samsung = listOf(
        "SM-S9110",
        "SM-S7110",
        "SM-S9210",
        "SM-S9310",
        "SM-S9420",
        "SM-A5360",
        "SM-A5460",
        "SM-A5560",
        "SM-A5660",
        "SM-S9180",
        "SM-S9280",
        "SM-S9380",
    )

    private val xiaomi = listOf(
        "2206123SC",
        "2211133C",
        "2210132C",
        "2304FPN6DC",
        "23127PN0CC",
        "23116PN5BC",
        "2311BPN23C",
        "24031PN0DC",
        "24129PN74C",
        "2410DPN6CC",
        "25019PNF3C",
        "25042PN24C",
        "25113PN0EC",
        "25098PN5AC",
        "2509FPN0BC",
        "2512BPNDAC",
        "25128PNA1C",
        "2605EPN8EC",
        "M531DA",
        "M025EC",
        "M610BB",
        "M154FF",
        "2109119BC",
        "2209129SC",
        "23046PNC9C",
        "24053PY09C",
        "25067PYE3C",
    )

    private val redmi = listOf(
        "23077RABDC",
        "2602BRNA4C",
        "22041219C",
        "22101317C",
        "22101316C",
        "22101316UCP",
        "23054RA19C",
        "23049RAD8C",
        "2312DRAABC",
        "2312DRA50C",
        "2312CRAD3C",
        "23090RA98C",
        "24094RAD4C",
        "24090RA29C",
        "24115RA8EC",
        "25098RA98C",
        "25080RABDC",
        "2510ERA8BC",
        "25104RADAC",
        "26021RN18C",
        "2607DRA18C",
        "24069RA21C",
        "24129RT7CC",
        "25053RT47C",
        "2511FRT34C",
        "2602BRT18C",
        "22011211C",
        "21121210C",
        "22081212C",
        "23013RK75C",
        "22127RK46C",
        "22122RK93C",
        "23078RKD5C",
        "23113RKC6C",
        "23117RK66C",
        "2311DRK48C",
        "2407FRK8EC",
        "24117RK2CC",
        "24122RKC7C",
        "25060RK16C",
        "2510DRK44C",
        "25102RKBEC",
        "25102RK69C",
        "2604FRK1EC",
        "M332BF",
        "M786BC",
        "M511CD",
        "M098FE",
    )

    private val oneplus = listOf(
        "NE2210",
        "PGKM10",
        "PGZ110",
        "PGP110",
        "PHB110",
        "PHK110",
        "PHP110",
        "PJA110",
        "PJD110",
        "PJE110",
        "PJF110",
        "PJX110",
        "PJZ110",
        "PKG110",
        "PKR110",
        "PKX110",
        "PLC110",
        "PLF110",
        "PLK110",
        "PLQ110",
        "PLR110",
        "PLZ110",
        "PMB110",
        "PLU110",
        "PLY110",
        "PYS110",
        "PYR110",
        "PYB110",
        "PYF110",
        "PMS110",
    )

    private val deviceModels: List<String> =
        vivo + iqoo + huawei + honor + oppo + samsung + xiaomi + redmi + oneplus

    fun randomDeviceModel(): String = deviceModels.random()

    fun randomUniqueId(): String = UUID.randomUUID().toString().replace("-", "")

    suspend fun getOrRandomUniqueId(context: Context, phoneNumber: String): String {
        val dataStore = context.chaoxingDataStore.data.first()
        val isLoginIdentity = dataStore.loginSession.phoneNumber == phoneNumber
        val index = dataStore.otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
        val storedUniqueId = when {
            isLoginIdentity -> dataStore.loginSession.uniqueId
            index >= 0 -> dataStore.getOtherUsers(index).uniqueId
            else -> ""
        }
        if (storedUniqueId.isNotEmpty()) return storedUniqueId
        val generated = randomUniqueId()
        context.chaoxingDataStore.updateData { current ->
            if (current.loginSession.phoneNumber == phoneNumber) {
                if (current.loginSession.uniqueId.isNotEmpty()) return@updateData current
                return@updateData current.toBuilder()
                    .setLoginSession(current.loginSession.toBuilder().setUniqueId(generated))
                    .build()
            }
            val currentIndex =
                current.otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
            if (currentIndex < 0 || current.getOtherUsers(currentIndex).uniqueId.isNotEmpty())
                return@updateData current
            current.toBuilder().setOtherUsers(
                currentIndex,
                current.getOtherUsers(currentIndex).toBuilder().setUniqueId(generated)
            ).build()
        }
        return generated
    }

    suspend fun getOrRandomDeviceModel(context: Context, phoneNumber: String): String {
        val dataStore = context.chaoxingDataStore.data.first()
        if (dataStore.loginSession.phoneNumber == phoneNumber) return localDeviceModel()
        val index = dataStore.otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
        val storedDeviceModel = if (index >= 0) dataStore.getOtherUsers(index).deviceModel else ""
        if (storedDeviceModel.isNotEmpty()) return storedDeviceModel
        val generated = randomDeviceModel()
        if (index < 0) return generated
        context.chaoxingDataStore.updateData { current ->
            val currentIndex =
                current.otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
            if (currentIndex < 0 || current.getOtherUsers(currentIndex).deviceModel.isNotEmpty())
                return@updateData current
            current.toBuilder().setOtherUsers(
                currentIndex,
                current.getOtherUsers(currentIndex).toBuilder().setDeviceModel(generated)
            ).build()
        }
        return generated
    }

    fun isValidUserAgent(userAgent: String): Boolean =
        userAgent.all { it.code in 0x20..0x7E }

    fun localDeviceModel(): String =
        Build.MODEL.filter { it.code in 0x20..0x7E }.ifBlank { "Android" }

    fun systemHttpAgentTemplate(): String {
        val fallback =
            "Dalvik/2.1.0 (Linux; U; Android ${Build.VERSION.RELEASE}; {model} Build/${Build.ID})"
        val agent = System.getProperty("http.agent", "").orEmpty()
        if (agent.isBlank() || agent.any { it.code !in 0x20..0x7E }) return fallback
        val model = localDeviceModel()
        val modelSegment = Regex("(Android [^;)]*;\\s*)${Regex.escape(model)}(\\s+Build/)")
        if (!modelSegment.containsMatchIn(agent)) return fallback
        return agent.replace(modelSegment) { match ->
            match.groupValues[1] + "{model}" + match.groupValues[2]
        }
    }

    fun overrideDeviceModel(userAgent: String, model: String): String =
        userAgent
            .replace(UA_DEVICE_SEGMENT, "(device:$model)")
            .replace(UA_BUILD_MODEL_SEGMENT) { match ->
                match.groupValues[1] + model + match.groupValues[2]
            }

    fun buildUserAgent(
        httpAgent: String,
        deviceModel: String,
        packageName: String,
        productId: String,
        versionName: String,
        versionCode: String,
        apiVersion: String,
        uniqueId: String,
        isAzeroth: Boolean = false,
        language: String = "zh_CN"
    ): String {
        val channel = if (isAzeroth) UA_CHANNEL_AZEROTH else UA_CHANNEL_KALIMDOR
        val core = "(device:$deviceModel) Language/$language $packageName/" +
                "ChaoXingStudy_${productId}_${versionName}" +
                "_android_phone_${versionCode}_$apiVersion ($channel)_$uniqueId"
        val schild = MessageDigest.getInstance("MD5")
            .digest("(schild:$UA_SCHILD_SALT) $core".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "$httpAgent (schild:$schild) $core"
    }
}
