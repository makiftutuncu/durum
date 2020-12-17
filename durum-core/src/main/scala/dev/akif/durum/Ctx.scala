package dev.akif.durum

/**
 * Abstract model for an HTTP request context
 *
 * @tparam REQ  Type of the request object
 * @tparam B    Type of the input data built from request
 * @tparam A    Type of the authorization data built from request
 */
trait Ctx[REQ, +B, A] {
  /** An identifier for the request, this will be used to correlate request and response in logs */
  val id: String

  /** Time when the request is received, see [[System#currentTimeMillis]] */
  val time: Long

  /** HTTP request object itself as the consumers of this context might need it */
  val request: REQ

  /** Headers of the request as a Map */
  val headers: Map[String, String]

  /** Input data built from the request */
  val body: B

  /** Authorization data built from the request */
  val auth: A
}
