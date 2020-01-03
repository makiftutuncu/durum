package dev.akif.durum

/**
 * Typeclass defining building the input data from a request
 *
 * @tparam F    Type of effect in which the input is built
 * @tparam REQ  Type of request from which the input is built
 * @tparam IN   Type of the input to be built
 */
trait InputBuilder[F[_], REQ, IN] {
  /**
   * Builds the input data from request
   *
   * @param req Request from which the input data is built
   *
   * @return Input data built from given request in given effect
   */
  def build(req: REQ): F[IN]

  /**
   * Builds the input data from request as a String to be logged
   *
   * @param req Request from which the input data is built
   *
   * @return Input data built from given request as a String in given effect
   */
  def log(req: REQ): F[String]
}
