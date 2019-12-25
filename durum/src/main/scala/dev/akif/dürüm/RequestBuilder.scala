package dev.akif.dürüm

trait RequestBuilder[F[_], REQ, IN] {
  def build(req: REQ): F[IN]

  def log(req: REQ): F[String]
}
