package dev.akif.durum

/**
 * Abstract model for an HTTP request context
 *
 * @param id      An identifier for the request, this will be used to correlate request and response in logs
 * @param time    Time when the request is received, see [[System#currentTimeMillis]]
 * @param request HTTP request object itself as the consumers of this context might need it
 * @param headers Headers of the request as a Map
 * @param body    Input data built from the request
 * @param auth    Authorization data built from the request
 *
 * @tparam REQ  Type of the request object
 * @tparam B    Type of the input data built from request
 * @tparam A    Type of the authorization data built from request
 */
abstract class Ctx[REQ, +B, A](val id: String,
                               val time: Long,
                               val request: REQ,
                               val headers: Map[String, String],
                               val body: B,
                               val auth: A)
