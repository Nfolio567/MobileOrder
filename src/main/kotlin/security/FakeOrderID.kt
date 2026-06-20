package security

import java.security.SecureRandom

object FakeOrderID {
  private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
  private val secureRandom = SecureRandom()

  fun generate(length: Int): String {
    val generatedID = StringBuilder()

    for (i in 0..<length) {
      generatedID.append(CHARS[secureRandom.nextInt(CHARS.length)])
    }

    return generatedID.toString()
  }
}