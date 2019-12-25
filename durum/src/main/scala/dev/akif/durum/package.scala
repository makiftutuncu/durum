package dev.akif

import scala.concurrent.{ExecutionContext, Future}

package object durum {
  implicit def futureEffect(implicit ec: ExecutionContext): Effect[Future] =
    new Effect[Future] {
      override val unit: Future[Unit] =
        Future.unit

      override def pure[A](a: A): Future[A] =
        Future.successful(a)

      override def map[A, B](f: Future[A])(m: A => B): Future[B] =
        f.map(m)

      override def flatMap[A, B](f: Future[A])(fm: A => Future[B]): Future[B] =
        f.flatMap(fm)

      override def foreach[A, U](f: Future[A])(fe: A => U): Unit = f.foreach(fe)

      override def fold[A, B](f: Future[A])(handleError: Throwable => Future[B], fm: A => Future[B]): Future[B] =
        f.flatMap(fm).recoverWith {
          case t: Throwable =>
            handleError(t)
        }

      override def mapError[A, AA >: A](f: Future[A])(handleError: Throwable => Future[AA]): Future[AA] =
        f.recoverWith {
          case t: Throwable =>
            handleError(t)
        }
    }

  implicit class EffectOps[F[+_], A](private val f: F[A])(implicit F: Effect[F]) {
    @inline def map[B](m: A => B): F[B] = F.map(f)(m)

    @inline def flatMap[B](fm: A => F[B]): F[B] = F.flatMap(f)(fm)

    @inline def foreach[U](fe: A => U): Unit = F.foreach(f)(fe)

    @inline def fold[B](handleError: Throwable => F[B], fm: A => F[B]): F[B] = F.fold(f)(handleError, fm)

    @inline def mapError[AA >: A](handleError: Throwable => F[AA]): F[AA] = F.mapError[A, AA](f)(handleError)
  }
}
