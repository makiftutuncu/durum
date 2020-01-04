package dev.akif.durum

import dev.akif.durum.TestData._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.{Failure, Success, Try}

class DurumSpec extends AnyWordSpec with Matchers {
  lazy val testId: String                     = "test-id"
  lazy val testDurum: TestDurum               = new TestDurum
  lazy val testRequest: TestRequest[String]   = TestRequest("GET", "/", Map("foo" -> "bar"), "test")
  lazy val testResponse: TestResponse[String] = TestResponse(200, Map("test" -> "test", Durum.idHeaderName -> testId), "test")

  "Getting headers of a request" should {
    "return headers" in {
      val expected = Map("foo" -> "bar")

      val actual = testDurum.getHeadersOfRequest(testRequest)

      actual shouldBe expected
    }
  }

  "Getting method of a request" should {
    "return method" in {
      val expected = "GET"

      val actual = testDurum.getMethodOfRequest(testRequest)

      actual shouldBe expected
    }
  }

  "Getting URI of a request" should {
    "return URI" in {
      val expected = "/"

      val actual = testDurum.getURIOfRequest(testRequest)

      actual shouldBe expected
    }
  }

  "Building authorization data from a request" should {
    "fail when there is an error" in {
      val expected = Failure(authFailedError)

      val actual = testDurum.buildAuth(testRequest)

      actual shouldBe expected
    }

    "return authorization data" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestAuth("test", "test"))

      val actual = testDurum.buildAuth(validRequest)

      actual shouldBe expected
    }
  }

  "Building context" should {
    "return a new context" in {
      val now = System.currentTimeMillis

      val expected = TestCtx("test", now, testRequest, testRequest.headers, testRequest.body, TestAuth("test", "test"))

      val actual = testDurum.buildContext("test", now, testRequest, testRequest.headers, testRequest.body, TestAuth("test", "test"))

      actual shouldBe expected
    }
  }

  "Getting status of a response" should {
    "return status" in {
      val expected = 200

      val actual = testDurum.getStatusOfResponse(testResponse)

      actual shouldBe expected
    }
  }

  "Getting status of an error" should {
    "return status" in {
      val expected = Durum.failedStatus

      val actual = testDurum.getStatusOfError(testError)

      actual shouldBe expected
    }
  }

  "Getting response with header" should {
    "return response with given header added" in {
      val initialResponse = testResponse.copy(headers = Map.empty)

      val expected = testResponse.copy(headers = Map("test" -> "test"))

      val actual = testDurum.responseWithHeader(initialResponse, "test" -> "test")

      actual shouldBe expected
    }
  }

  "Wrapping" should {
    def failingController(request: TestRequest[String], error: Throwable): Try[TestResponse[String]] =
      testDurum.wrap(request) { ctx =>
        Failure(error)
      }

    def controller(request: TestRequest[String]): Try[TestResponse[String]] =
      testDurum.wrap(request) { ctx =>
        Success(testResponse)
      }

    "return error when auth fails" in {
      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = controller(testRequest)

      actual shouldBe expected
    }

    "return error when controller fails" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingController(validRequest, testError)

      actual shouldBe expected
    }

    "perform action and return response" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse)

      val actual = controller(validRequest)

      actual shouldBe expected
    }
  }

  "Wrapping with input" should {
    def failingController(request: TestRequest[String], error: Throwable)(implicit ib: InputBuilder[Try, TestRequest[String], Int]): Try[TestResponse[String]] =
      testDurum.wrapWithInput[Int](request) { ctx =>
        Failure(error)
      }

    def controller(request: TestRequest[String])(implicit ib: InputBuilder[Try, TestRequest[String], Int]): Try[TestResponse[String]] =
      testDurum.wrapWithInput[Int](request) { ctx =>
        Success(testResponse)
      }

    "return error when converting input fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int] = inputBuilder[Int](_ => Failure(inputFailedError))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "input-failed"))

      val actual = controller(testRequest)

      actual shouldBe expected
    }

    "return error when auth fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int] = inputBuilder[Int](s => Success(s.toInt))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = controller(testRequest.copy(body = "5"))

      actual shouldBe expected
    }

    "return error when controller fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int] = inputBuilder[Int](s => Success(s.toInt))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingController(validRequest, testError)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int] = inputBuilder[Int](s => Success(s.toInt))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(testResponse)

      val actual = controller(validRequest)

      actual shouldBe expected
    }
  }

  "Action with output" should {
    def failingController(request: TestRequest[String], error: Throwable)(implicit ob: OutputBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.wrapWithOutput[Boolean](request) { ctx =>
        Failure(error)
      }

    def controller(request: TestRequest[String])(implicit ob: OutputBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.wrapWithOutput[Boolean](request) { ctx =>
        Success(true)
      }

    "return error when auth fails" in {
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Try(b.toString))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = controller(testRequest)

      actual shouldBe expected
    }

    "return error when controller fails" in {
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Try(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingController(validRequest, testError)

      actual shouldBe expected
    }

    "return error when converting output fails" in {
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, _ => Failure(outputFailedError))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "output-failed"))

      val actual = controller(validRequest)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Try(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse.copy(body = "true", headers = Map(Durum.idHeaderName -> testId)))

      val actual = controller(validRequest)

      actual shouldBe expected
    }
  }

  "Action with input and output" should {
    def failingController(request: TestRequest[String], error: Throwable)(implicit ib: InputBuilder[Try, TestRequest[String], Int], ob: OutputBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.wrapWithInputAndOutput[Int, Boolean](request) { ctx =>
        Failure(error)
      }

    def controller(request: TestRequest[String])(implicit ib: InputBuilder[Try, TestRequest[String], Int], ob: OutputBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.wrapWithInputAndOutput[Int, Boolean](request) { ctx =>
        Success(ctx.body >= 0)
      }

    "return error when converting input fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int]       = inputBuilder[Int](_ => Failure(inputFailedError))
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Success(b.toString))

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "input-failed"))

      val actual = controller(testRequest)

      actual shouldBe expected
    }

    "return error when auth fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int]       = inputBuilder[Int](s => Success(s.toInt))
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(body = "5")

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = controller(validRequest)

      actual shouldBe expected
    }

    "return error when controller fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int]       = inputBuilder[Int](s => Success(s.toInt))
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingController(validRequest, testError)

      actual shouldBe expected
    }

    "return error when converting output fails" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int]       = inputBuilder[Int](s => Success(s.toInt))
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, _ => Failure(outputFailedError))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(TestResponse(Durum.failedStatus, Map(Durum.idHeaderName -> testId), "output-failed"))

      val actual = controller(validRequest)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val ib: InputBuilder[Try, TestRequest[String], Int]       = inputBuilder[Int](s => Success(s.toInt))
      implicit val ob: OutputBuilder[Try, Boolean, TestResponse[String]] = outputBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(testResponse.copy(body = "true", headers = Map(Durum.idHeaderName -> testId)))

      val actual = controller(validRequest)

      actual shouldBe expected
    }
  }
}
