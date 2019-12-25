package dev.akif.durum

import java.util.UUID

abstract class Ctx[REQ, +B, A](val id: String,
                               val request: REQ,
                               val headers: Map[String, String],
                               val body: B,
                               val auth: A,
                               val time: Long)

object Ctx {
  val idHeaderName: String = "X-Id"

  def getOrCreateId(headers: Map[String, String]): String = headers.getOrElse(idHeaderName, UUID.randomUUID.toString)
}
