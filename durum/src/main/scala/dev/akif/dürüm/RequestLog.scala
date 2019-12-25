package dev.akif.dürüm

import java.time.{Instant, ZoneOffset, ZonedDateTime}

case class RequestLog(method: String,
                      uri: String,
                      id: String,
                      time: Long,
                      headers: Map[String, String],
                      body: String) {
  val humanReadableTime: String = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC).withNano(0).toString

  def toLogString(isIncoming: Boolean): String = {
    val title  = s"${if (isIncoming) "Incoming" else "Outgoing"} Request"
    val prefix = if (isIncoming) "<" else ">"
    val sb     = new StringBuilder(s"$title\n")

    def append(s: String): StringBuilder     = sb.append(prefix).append(" ").append(s)
    def appendLine(s: String): StringBuilder = append(s).append("\n")

    appendLine(s"$method $uri")
    appendLine(s"Id: $id")
    appendLine(s"Time: $humanReadableTime")
    headers.foreachEntry((name, value) => appendLine(s"$name: $value"))
    if (body.nonEmpty) {
      appendLine("")
      append(body)
    }

    sb.toString()
  }
}
