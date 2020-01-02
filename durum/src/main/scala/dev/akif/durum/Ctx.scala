package dev.akif.durum

abstract class Ctx[REQ, +B, A](val id: String,
                               val request: REQ,
                               val headers: Map[String, String],
                               val body: B,
                               val auth: A,
                               val time: Long)
