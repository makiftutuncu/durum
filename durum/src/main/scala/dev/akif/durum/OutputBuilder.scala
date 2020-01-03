package dev.akif.durum

/**
 * Typeclass defining building a response from the output data
 *
 * @tparam F    Type of effect in which the response is built
 * @tparam OUT  Type of output data from which the response is built
 * @tparam RES  Type of the response to be built
 */
trait OutputBuilder[F[_], OUT, RES] {
  /**
   * Builds the response from output data
   *
   * @param status  HTTP status code of the response to build
   * @param out     Output data from which the response is built
   *
   * @return Response built from given output data in given effect
   */
  def build(status: Int, out: OUT): F[RES]

  /**
   * Builds the response from output data as a String to be logged
   *
   * @param out Output data from which the response is built
   *
   * @return Response built from given output data as a String in given effect
   */
  def log(out: OUT): F[String]
}
