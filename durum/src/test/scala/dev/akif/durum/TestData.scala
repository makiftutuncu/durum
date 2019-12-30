package dev.akif.durum

import scala.util.{Failure, Success, Try}

object TestData {
  implicit val tryEffect: Effect[Try] =
    new Effect[Try] {
      override def pure[A](a: A): Try[A] = Success(a)

      override def map[A, B](f: Try[A])(m: A => B): Try[B] = f.map(m)

      override def flatMap[A, B](f: Try[A])(fm: A => Try[B]): Try[B] = f.flatMap(fm)

      override def foreach[A, U](f: Try[A])(fe: A => U): Unit = f.foreach(fe)

      override def fold[A, B](f: Try[A])(handleError: Throwable => Try[B], fm: A => Try[B]): Try[B] =
        f match {
          case Success(a) => fm(a)
          case Failure(t) => handleError(t)
        }

      override def mapError[A, AA >: A](f: Try[A])(handleError: Throwable => Try[AA]): Try[AA] =
        f match {
          case s @ Success(_) => s
          case Failure(t)     => handleError(t)
        }
    }

  case class TestRequest[B](method: String, uri: String, headers: Map[String, String], body: B)

  case class TestResponse[B](status: Int, headers: Map[String, String], body: B)

  case class TestAuth(username: String, password: String)

  case class TestCtx[B](override val id: String,
                        override val request: TestRequest[String],
                        override val headers: Map[String, String],
                        override val body: B,
                        override val auth: TestAuth,
                        override val time: Long) extends Ctx[TestRequest[String], B, TestAuth](id, request, headers, body, auth, time)

  class TestDurum extends Durum[Try, TestRequest[String], TestResponse[String], TestAuth, TestCtx] {
    override def getHeadersOfRequest(request: TestRequest[String]): Map[String, String] = request.headers

    override def getMethodOfRequest(request: TestRequest[String]): String = request.method

    override def getURIOfRequest(request: TestRequest[String]): String = request.uri

    override def buildAuth(request: TestRequest[String]): Try[TestAuth] =
      Try {
        val a = request.headers.getOrElse("Authorization", "").split(":")
        TestAuth(a(0), a(1))
      }

    override def buildContext[IN](id: String, request: TestRequest[String], headers: Map[String, String], in: IN, auth: TestAuth, time: Long): TestCtx[IN] =
      TestCtx[IN](id, request, headers, in, auth, time)

    override def getStatusOfResponse(response: TestResponse[String]): Int = response.status

    override def buildFailedResponse(throwable: Throwable): Try[TestResponse[String]] = Success(TestResponse(500, Map.empty, throwable.getMessage))

    override def buildFailedResponseAsString(throwable: Throwable): Try[String] = Success(throwable.getMessage)

    override def responseWithHeader(response: TestResponse[String], header: (String, String)): TestResponse[String] = response.copy(headers = response.headers + header)

    override def getHeadersOfResponse(response: TestResponse[String]): Map[String, String] = response.headers

    override def logRequest(log: RequestLog, failed: Boolean): String = log.toLogString(true)

    override def logResponse(log: ResponseLog, failed: Boolean): String = log.toLogString(false)
  }
}
