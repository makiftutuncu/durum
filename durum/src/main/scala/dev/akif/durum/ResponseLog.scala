package dev.akif.durum

case class ResponseLog(status: Int,
                       method: String,
                       uri: String,
                       id: String,
                       time: Long,
                       headers: Map[String, String],
                       body: String) {
  val took: Long = System.currentTimeMillis - time

  def toLogString(isIncoming: Boolean): String = {
    val title  = s"${if (isIncoming) "Incoming" else "Outgoing"} Response"
    val prefix = if (isIncoming) "<" else ">"
    val sb     = new StringBuilder(s"$title\n")

    def append(s: String): StringBuilder     = sb.append(prefix).append(" ").append(s)
    def appendLine(s: String): StringBuilder = append(s).append("\n")

    appendLine(s"$status $method $uri")
    appendLine(s"Id: $id")
    appendLine(s"Took: $took ms")
    headers.foreachEntry((name, value) => appendLine(s"$name: $value"))
    if (body.nonEmpty) {
      appendLine("")
      append(body)
    }

    sb.toString()
  }
}
