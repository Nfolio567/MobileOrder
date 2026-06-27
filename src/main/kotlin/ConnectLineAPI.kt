package one.nfolio

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import one.nfolio.dto.line.VerifyToken

class ConnectLineAPI(private val client: HttpClient, private val environment: ApplicationEnvironment, apiVersion: String) {
  private val lineAPIRequestURL = "https://api.line.me/oauth2/v$apiVersion"

  suspend fun verifyIDToken(token: String?): VerifyToken? { // tokenがnullの場合はそのままnullを返す。呼び出し元でnullチェック
    if (token == null) return null

    val res = client.submitForm(
      url = "$lineAPIRequestURL/verify",
      formParameters = Parameters.build {
        append("id_token", token)
        append("client_id", environment.config.property("LIFF.id").getString())
      }
    )

    val resBody = if (res.status == HttpStatusCode.OK) res.body<VerifyToken>() else null

    return resBody
  }
}