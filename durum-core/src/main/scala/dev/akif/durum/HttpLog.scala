package dev.akif.durum

/**
 * Log object for common HTTP data
 *
 * @see [[dev.akif.durum.RequestLog]]
 * @see [[dev.akif.durum.ResponseLog]]
 */
trait HttpLog {
  /** Id of request, used to correlate request and response in the logs, see [[dev.akif.durum.Durum#idHeaderName]] */
  val id: String

  /** Time when the request is received, see [[System#currentTimeMillis]] */
  val time: Long

  /** Method of the request */
  val method: String

  /** URI of the request */
  val uri: String

  /** Headers (from request or response) as a Map */
  val headers: Map[String, String]

  /** Body as a String (input from request or output from response) */
  val body: String

  /** Whether or not this is a failure log (you might want to log with a different level depending on this) */
  val failed: Boolean

  /** Extra data to log */
  protected val logData: Map[String, String]

  /**
   * Converts this HTTP log into String with a default, multi-line format
   *
   * @param logType Type of this HTTP log
   *
   * @return This HTTP log as a String with a default, multi-line format
   */
  def toLog(logType: LogType): String = {
    val title  = s"${if (logType.incoming) "Incoming" else "Outgoing"} ${logType.name}"
    val sb     = new StringBuilder(s"$title\n")

    def append(s: String): StringBuilder = sb.append(logType.prefix).append(s)

    def appendLine(s: String): StringBuilder = append(s).append("\n")

    appendLine(s" $method $uri")
    appendLine(s" Id: $id")
    logData.foreachEntry((name, value) => appendLine(s" $name: $value"))
    headers.foreachEntry((name, value) => appendLine(s" $name: $value"))
    if (body.nonEmpty) {
      appendLine("")
      append(s" $body")
    }

    sb.toString()
  }
}
