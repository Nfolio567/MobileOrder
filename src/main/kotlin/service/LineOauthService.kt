package one.nfolio.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationEnvironment
import one.nfolio.dto.line.VerifyToken

class LineOauthService(
  private val client: HttpClient,
  private val environment: ApplicationEnvironment,
  apiVersion: String,
) {
  private val lineAPIRequestURL = "https://api.line.me/oauth2/v$apiVersion"

  suspend fun verifyIDToken(token: String?): VerifyToken? { // tokenがnullの場合はそのままnullを返す。呼び出し元でnullチェック
    if (token == null) return null

    val res =
      client.submitForm(
        url = "$lineAPIRequestURL/verify",
        formParameters =
          Parameters.build {
            append("id_token", token)
            append("client_id", environment.config.property("LIFF.id").getString())
          },
      )

    environment.log.info("{}", res.bodyAsText())

    val resBody = if (res.status == HttpStatusCode.OK) res.body<VerifyToken>() else null

    return resBody
  }
}
