package one.nfolio.security

import java.security.MessageDigest

object GenerateHash {
  fun sha256(input: String): String {
    val bytes = input.toByteArray()

    val sha2 = MessageDigest.getInstance("SHA256")
    val digest = sha2.digest(bytes)

    return digest.joinToString("") { "%02x".format(it) }
  }
}