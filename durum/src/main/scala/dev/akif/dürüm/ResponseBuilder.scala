package dev.akif.dürüm

trait ResponseBuilder[F[_], OUT, RES] {
  def build(status: Int, out: OUT): F[RES]

  def log(out: OUT): F[String]
}
