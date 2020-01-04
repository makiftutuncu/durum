package dev.akif.durum

object syntax {
  /**
   * <p>Implicit extensions to an effect, inlining its methods, so that
   * they can be called as simple instance methods and the effect can be used in a for-comprehension</p>
   *
   * For example, instead of writing
   *
   * {{{
   *   val x: F[Int] = ???
   *   val y: F[Boolean] = ???
   *
   *   val z: F[String] =
   *    F.flatMap(x) { i =>
   *      F.map(y) { b =>
   *        b.toString
   *      }
   *    }
   * }}}
   *
   * one can write
   *
   * {{{
   *   val x: F[Int] = ???
   *   val y: F[Boolean] = ???
   *
   *   val z: F[String] =
   *     for {
   *       i <- x
   *       b <- y
   *     } yield {
   *       b.toString
   *     }
   * }}}
   *
   * @param f An effect
   * @param F An implicit instance of the effect
   *
   * @tparam F  Type of the effect
   * @tparam E  Type of the error with which effect can fail
   * @tparam A  Type of the value of the effect
   */
  implicit class EffectOps[F[+_], E, A](private val f: F[A])(implicit F: Effect[F, E]) {
    @inline def map[B](m: A => B): F[B] = F.map(f)(m)

    @inline def flatMap[B](fm: A => F[B]): F[B] = F.flatMap(f)(fm)

    @inline def foreach[U](fe: A => U): Unit = F.foreach(f)(fe)

    @inline def fold[B](handleError: E => F[B], fm: A => F[B]): F[B] = F.fold(f)(handleError, fm)

    @inline def mapError[AA >: A](handleError: E => F[AA]): F[AA] = F.mapError[A, AA](f)(handleError)
  }
}
