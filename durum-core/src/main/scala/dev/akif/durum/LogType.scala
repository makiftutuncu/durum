package dev.akif.durum

/**
 * Type of HTTP request/response to log
 *
 * @param incoming  Whether or not this was an incoming request/response
 * @param name      "Request" or "Response"
 * @param prefix    Prefix to show at the beginning of each line in the default formatting of [[dev.akif.durum.HttpLog]]
 */
sealed abstract class LogType(val incoming: Boolean,
                              val name: String,
                              val prefix: String)

object LogType {
  /** Incoming HTTP request (to be handled) */
  case object IncomingRequest  extends LogType(true, "Request", "<")

  /** Outgoing HTTP response (produced by handling an incoming request) */
  case object OutgoingResponse extends LogType(false, "Response", ">")

  /** Outgoing HTTP request (sent by a client to an external service) */
  case object OutgoingRequest  extends LogType(false, "Request", ">>")

  /** Incoming HTTP response (received by the client after sending a request to an external service) */
  case object IncomingResponse extends LogType(true, "Response", "<<")
}
