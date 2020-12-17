package dev.akif.durum

import scala.util.{Failure, Success, Try}

object TestData {
  case class TestRequest[B](method: String, uri: String, headers: Map[String, String], body: B)

  case class TestResponse[B](status: Int, headers: Map[String, String], body: B)

  case class TestAuth(username: String, password: String)

  case class TestCtx[B](override val id: String,
                        override val time: Long,
                        override val request: TestRequest[String],
                        override val headers: Map[String, String],
                        override val body: B,
                        override val auth: TestAuth) extends Ctx[TestRequest[String], B, TestAuth]

  class TestDurum extends Durum[Try, Throwable, TestRequest[String], TestResponse[String], TestAuth, TestCtx] {
    override val errorOutputBuilder: OutputBuilder[Try, Throwable, TestResponse[String]] = outputBuilder[Throwable](Durum.failedStatus, t => Success(t.getMessage))

    override def getHeadersOfRequest(request: TestRequest[String]): Map[String, String] = request.headers

    override def getMethodOfRequest(request: TestRequest[String]): String = request.method

    override def getURIOfRequest(request: TestRequest[String]): String = request.uri

    override def buildAuth(request: TestRequest[String]): Try[TestAuth] =
      Try {
        val a = request.headers.getOrElse("Authorization", "").split(":")
        TestAuth(a(0), a(1))
      } match {
        case Failure(_)     => Failure(authFailedError)
        case a @ Success(_) => a
      }

    override def buildContext[IN](id: String, time: Long, request: TestRequest[String], headers: Map[String, String], in: IN, auth: TestAuth): TestCtx[IN] =
      TestCtx[IN](id, time, request, headers, in, auth)

    override def getStatusOfResponse(response: TestResponse[String]): Int = response.status

    override def getStatusOfError(throwable: Throwable): Int = Durum.failedStatus

    override def responseWithHeader(response: TestResponse[String], header: (String, String)): TestResponse[String] = response.copy(headers = response.headers + header)

    override def getHeadersOfResponse(response: TestResponse[String]): Map[String, String] = response.headers

    override def logRequest(log: RequestLog): Unit = log.toLog(LogType.IncomingRequest)

    override def logResponse(log: ResponseLog): Unit = log.toLog(LogType.OutgoingResponse)

    override def getOrCreateId(headers: Map[String, String]): String = "test-id"
  }

  def inputBuilder[A](parser: String => Try[A]): InputBuilder[Try, TestRequest[String], A] =
    new InputBuilder[Try, TestRequest[String], A] {
      override def build(req: TestRequest[String]): Try[A] = parser(req.body)

      override def log(req: TestRequest[String]): Try[String] = Success(req.body)
    }

  def outputBuilder[A](status: Int, converter: A => Try[String]): OutputBuilder[Try, A, TestResponse[String]] =
    new OutputBuilder[Try, A, TestResponse[String]] {
      override def build(status: Int, a: A): Try[TestResponse[String]] = converter(a).map(s => TestResponse(status, Map.empty, s))

      override def log(a: A): Try[String] = converter(a)
    }

  implicit val tryEffect: Effect[Try, Throwable] =
    new Effect[Try, Throwable] {
      override def pure[A](a: A): Try[A] = Success(a)

      override def error[A](t: Throwable): Try[A] = Failure(t)

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

  val inputFailedError  = new RuntimeException("input-failed")
  val authFailedError   = new RuntimeException("auth-failed")
  val outputFailedError = new RuntimeException("output-failed")
  val testError         = new RuntimeException("test")
}
