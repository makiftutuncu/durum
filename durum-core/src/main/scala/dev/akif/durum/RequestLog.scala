package dev.akif.durum

import java.time.{Instant, ZoneOffset, ZonedDateTime}

/**
 * HTTP log for a request
 *
 * @see [[dev.akif.durum.HttpLog]]
 */
case class RequestLog(override val id: String,
                      override val time: Long,
                      override val method: String,
                      override val uri: String,
                      override val headers: Map[String, String],
                      override val body: String,
                      override val failed: Boolean) extends HttpLog {
  override protected val logData: Map[String, String] =
    Map(
      "Time" -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneOffset.UTC).withFixedOffsetZone().withNano(0).toString
    )
}
