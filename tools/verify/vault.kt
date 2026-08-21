// Mirrors SecureVault's blob format + decrypt path using JCE AES/GCM.
// Android Keystore isn't available off-device, so we substitute a plain AES key;
// the FORMAT and failure handling are what we're validating.
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.Base64

object B64 { fun enc(b: ByteArray): String = Base64.getEncoder().encodeToString(b)
             fun dec(s: String): ByteArray = Base64.getDecoder().decode(s) }

class Vault(private val key: SecretKey) {
    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        val ct = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return B64.enc(c.iv) + ":" + B64.enc(ct)
    }
    fun decrypt(blob: String): String? = runCatching {
        val sep = blob.indexOf(':')
        if (sep <= 0) return null
        val iv = B64.dec(blob.substring(0, sep))
        val ct = B64.dec(blob.substring(sep + 1))
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        String(c.doFinal(ct), Charsets.UTF_8)
    }.getOrNull()
}

fun main() {
    val kg = KeyGenerator.getInstance("AES"); kg.init(256)
    val v = Vault(kg.generateKey())
    val other = Vault(kg.generateKey())

    val key = "sk-proj-abc123XYZ_secret-key-value"
    val blob = v.encrypt(key)
    println("blob format ok      : ${Regex("^[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$").matches(blob)}")
    println("plaintext absent    : ${!blob.contains("sk-proj")}")
    println("roundtrip           : ${v.decrypt(blob) == key}")
    println("unicode roundtrip   : ${v.decrypt(v.encrypt("मेरी चाबी 🔑")) == "मेरी चाबी 🔑"}")
    println("wrong key -> null   : ${other.decrypt(blob) == null}")
    println("tampered -> null    : ${v.decrypt(blob.dropLast(4) + "AAAA") == null}")
    println("garbage -> null     : ${v.decrypt("not-a-blob") == null}")
    println("no separator -> null: ${v.decrypt("abcdef") == null}")
    println("empty -> null       : ${v.decrypt("") == null}")
    // distinct IV per call
    println("IV differs per call : ${v.encrypt(key) != v.encrypt(key)}")
}
