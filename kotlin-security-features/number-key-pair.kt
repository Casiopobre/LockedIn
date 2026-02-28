import java.security.KeyPairGenerator
import java.security.MessageDigest
import com.google.i18n.phonenumbers.PhoneNumberUtil

// --- Dependencies (build.gradle.kts) ---
// implementation("com.googlecode.libphonenumber:libphonenumber:8.13.+")

// Normalize phone number to E.164
fun normalizeToE164(rawNumber: String, defaultRegion: String = "US"): String {
    val phoneUtil = PhoneNumberUtil.getInstance()
    val parsed = phoneUtil.parse(rawNumber, defaultRegion)

    require(phoneUtil.isValidNumber(parsed)) { "Invalid phone number: $rawNumber" }

    return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
    // e.g. "+14155552671"
}

// Hash with SHA-256 + public salt
const val PUBLIC_SALT = "my-public-app-salt-v1" // baked into the code

fun hashPhoneNumber(e164Number: String): String {
    val input = PUBLIC_SALT + e164Number          // salt prefix
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return hashBytes.joinToString("") { "%02x".format(it) }
}

// Generate a key pair (EC P-256)
fun generateKeyPair(): java.security.KeyPair {
    val keyGen = KeyPairGenerator.getInstance("EC") // Elliptic Curve
    keyGen.initialize(
        java.security.spec.ECGenParameterSpec("secp256r1"), // P-256
        java.security.SecureRandom()
    )
    return keyGen.generateKeyPair()
}

// Main
fun main() {
    // Normalize
    val raw = "+1 (415) 555-2671"
    val e164 = normalizeToE164(raw, defaultRegion = "US")
    println("E.164       : $e164")               // +14155552671

    // Hash
    val hash = hashPhoneNumber(e164)
    println("SHA-256 hash: $hash")

    // Key pair
    val keyPair = generateKeyPair()
    val pubKeyHex = keyPair.public.encoded.joinToString("") { "%02x".format(it) }
    val privKeyHex = keyPair.private.encoded.joinToString("") { "%02x".format(it) }
    println("Public key  : $pubKeyHex")
    println("Private key : $privKeyHex")          // store this securely!
}
