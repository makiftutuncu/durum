package dev.akif.durum

/**
 * HTTP log for a response
 *
 * @param status  HTTP status code of the response
 * @param now     Time when the response is built, used to calculate total time of the request, see [[System#currentTimeMillis]]
 *
 * @see [[dev.akif.durum.HttpLog]]
 */
case class ResponseLog(override val id: String,
                       override val time: Long,
                       override val method: String,
                       override val uri: String,
                       override val headers: Map[String, String],
                       override val body: String,
                       override val failed: Boolean,
                       status: Int,
                       now: Long) extends HttpLog {
  override protected val logData: Map[String, String] =
    Map(
      "Status" -> status.toString,
      "Took"   -> s"${now - time} ms"
    )
}
