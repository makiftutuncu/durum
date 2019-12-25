package dev.akif.durum

trait Effect[F[+_]] {
  val unit: F[Unit]

  def pure[A](a: A): F[A]

  def map[A, B](f: F[A])(m: A => B): F[B]

  def flatMap[A, B](f: F[A])(fm: A => F[B]): F[B]

  def foreach[A, U](f: F[A])(fe: A => U): Unit

  def fold[A, B](f: F[A])(handleError: Throwable => F[B], fm: A => F[B]): F[B]

  def mapError[A, AA >: A](f: F[A])(handleError: Throwable => F[AA]): F[AA]
}
