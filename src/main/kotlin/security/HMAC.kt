package security

import io.ktor.server.application.ApplicationEnvironment
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class HMAC(environment: ApplicationEnvironment) {
  val key: SecretKey = SecretKeySpec(
    environment.config.property("HMAC.secret-key").toString().toByteArray(Charsets.UTF_8),
    "HmacSHA256"
  )

  fun generateMAC(text: String): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(key)

    return mac.doFinal(text.toByteArray())
  }

  fun verify(verifiedText: String, comparedMac: ByteArray): Boolean {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(key)

    return comparedMac.contentEquals(mac.doFinal(verifiedText.toByteArray()))
  }
}