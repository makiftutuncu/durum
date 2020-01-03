package dev.akif.durum

import scala.concurrent.{ExecutionContext, Future}

object future {
  /**
   * An implicit conversion to provide an [[dev.akif.durum.Effect]] implementation of [[scala.concurrent.Future]]
   *
   * @param ec An implicit instance of a [[scala.concurrent.ExecutionContext]]
   *
   * @return An effect of Future
   */
  implicit def FutureEffect(implicit ec: ExecutionContext): Effect[Future, Throwable] =
    new Effect[Future, Throwable] {
      override def pure[A](a: A): Future[A] =
        Future.successful(a)

      override def error[A](t: Throwable): Future[A] =
        Future.failed(t)

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
}
