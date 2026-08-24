package one.nfolio

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.tryGet(
  path: String,
  body: suspend RoutingContext.() -> Unit,
) {
  get(path) {
    try {
      body()
    } catch (e: Exception) {
      errorResponse(e)
    }
  }
}

fun Route.tryPost(
  path: String,
  body: suspend RoutingContext.() -> Unit,
) {
  post(path) {
    try {
      body()
    } catch (e: Exception) {
      errorResponse(e)
    }
  }
}

fun Route.tryDelete(
  path: String,
  body: suspend RoutingContext.() -> Unit,
) {
  delete(path) {
    try {
      body()
    } catch (e: Exception) {
      errorResponse(e)
    }
  }
}

suspend fun RoutingContext.errorResponse(e: Exception) {
  call.application.log.warn("{} error", call.request.path(), e)
  call.respond(HttpStatusCode.InternalServerError)
}

suspend fun RoutingContext.errorResponse(
  e: Exception,
  responseMessage: Pair<String, String>,
) {
  call.application.log.warn("{} error", call.request.path(), e)
  call.respond(
    HttpStatusCode.InternalServerError,
    mapOf(responseMessage),
  )
}
