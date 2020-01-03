package dev.akif.durum

/**
 * Abstraction for functional effects that can fail, used in [[dev.akif.durum.Durum]]
 *
 * @tparam F Type constructor for the effect
 * @tparam E Type of the error with which this effect can fail
 *
 * @see [[dev.akif.durum.future#FutureEffect]]
 * @see [[dev.akif.durum.syntax.EffectOps]]
 */
trait Effect[F[+_], E] {
  /** An effect of Unit value, doing nothing */
  val unit: F[Unit] = pure[Unit](())

  /**
   * Creates an effect with a value
   *
   * @param a A value
   *
   * @tparam A Type of the value
   *
   * @return An effect such that once processed, given value is immediately available
   */
  def pure[A](a: A): F[A]

  /**
   * Creates a failed effect with an error
   *
   * @param e An error
   *
   * @tparam A Type of the value of the effect
   *
   * @return A failed effect
   */
  def error[A](e: E): F[A]

  /**
   * Converts given effect into a new one by using its value
   *
   * @param f An effect
   * @param m A function to convert value of given effect into a new one
   *
   * @tparam A Type of the value of given effect
   * @tparam B Type of the value of produced effect
   *
   * @return A new effect produced by applying the value of given effect to given function
   */
  def map[A, B](f: F[A])(m: A => B): F[B]

  /**
   * Converts given effect into a new one by using its value
   *
   * @param f   An effect
   * @param fm  A function to build a new effect using the value of given effect
   *
   * @tparam A Type of the value of given effect
   * @tparam B Type of the value of produced effect
   *
   * @return A new effect produced by applying the value of given effect to given function
   */
  def flatMap[A, B](f: F[A])(fm: A => F[B]): F[B]

  /**
   * Uses the value of an effect without producing a value
   *
   * @param f   An effect
   * @param fe  A function to use the value of given effect
   *
   * @tparam A Type of the value of given effect
   * @tparam U Type of the value produced by using the value of given effect, which will be ignored as it is not returned
   */
  def foreach[A, U](f: F[A])(fe: A => U): Unit

  /**
   * Converts an effect into a new one by either handling its error or using its value
   *
   * @param f           An effect
   * @param handleError A function to build a new effect using the error of given effect
   * @param fm          A function to build a new effect using the value of given effect
   *
   * @tparam A Type of the value of given effect
   * @tparam B Type of the value of produced effect
   *
   * @return A new effect produced by either handling the error or using the value of given effect
   *
   * @see [[dev.akif.durum.Effect#mapError]]
   * @see [[dev.akif.durum.Effect#flatMap]]
   */
  def fold[A, B](f: F[A])(handleError: E => F[B], fm: A => F[B]): F[B]

  /**
   * Converts an effect into a new one by handling its error
   *
   * @param f           An effect
   * @param handleError A function to build a new effect using the error of given effect
   *
   * @tparam A  Type of the value of given effect
   * @tparam AA Type of the value of produced effect
   *
   * @return A new effect produced by handling the error of given effect
   */
  def mapError[A, AA >: A](f: F[A])(handleError: E => F[AA]): F[AA]
}
