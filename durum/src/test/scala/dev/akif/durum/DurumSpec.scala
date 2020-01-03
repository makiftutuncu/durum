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

      val expected = TestCtx("test", testRequest, testRequest.headers, testRequest.body, TestAuth("test", "test"), now)

      val actual = testDurum.buildContext("test", testRequest, testRequest.headers, testRequest.body, TestAuth("test", "test"), now)

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

  "Building a failed response" should {
    "return response with failure details" in {
      val expected = Success(TestResponse(500, Map.empty, "test"))

      val actual = testDurum.buildFailedResponse(testError)

      actual shouldBe expected
    }
  }

  "Building a failed response as string" should {
    "return response with failure details" in {
      val expected = Success("test")

      val actual = testDurum.buildFailedResponseAsString(testError)

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

  "Logging a request" should {
    "return String representation of request data" in {
      val now = 0L
      val requestLog = RequestLog("GET", "/", "test", now, Map("foo" -> "bar"), "test")

      val expected =
        """Incoming Request
          |< GET /
          |< Id: test
          |< Time: 1970-01-01T00:00Z
          |< foo: bar
          |<
          |< test""".stripMargin

      val actual = testDurum.logRequest(requestLog, failed = false)

      actual shouldBe expected
    }
  }

  "Logging a response" should {
    "return String representation of response data" in {
      val requestLog = ResponseLog(200, "GET", "/", "test", 0L, 5L, Map("foo" -> "bar"), "test")

      val expected =
        """Outgoing Response
          |> 200 GET /
          |> Id: test
          |> Took: 5 ms
          |> foo: bar
          |>
          |> test""".stripMargin

      val actual = testDurum.logResponse(requestLog, failed = false)

      actual shouldBe expected
    }
  }

  "Basic action" should {
    def failingAction(request: TestRequest[String], error: Throwable): Try[TestResponse[String]] =
      testDurum.basicAction(request) { ctx =>
        Failure(error)
      }

    def action(request: TestRequest[String]): Try[TestResponse[String]] =
      testDurum.basicAction(request) { ctx =>
        Success(testResponse)
      }

    "return error when auth fails" in {
      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = action(testRequest)

      actual shouldBe expected
    }

    "return error when action fails" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingAction(validRequest, testError)

      actual shouldBe expected
    }

    "perform action and return response" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse)

      val actual = action(validRequest)

      actual shouldBe expected
    }
  }

  "Action with input" should {
    def failingAction(request: TestRequest[String], error: Throwable)(implicit reqb: RequestBuilder[Try, TestRequest[String], Int]): Try[TestResponse[String]] =
      testDurum.actionWithInput[Int](request) { ctx =>
        Failure(error)
      }

    def action(request: TestRequest[String])(implicit reqb: RequestBuilder[Try, TestRequest[String], Int]): Try[TestResponse[String]] =
      testDurum.actionWithInput[Int](request) { ctx =>
        Success(testResponse)
      }

    "return error when converting input fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Int] = requestBuilder[Int](_ => Failure(inputFailedError))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "input-failed"))

      val actual = action(testRequest)

      actual shouldBe expected
    }

    "return error when auth fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Int] = requestBuilder[Int](s => Success(s.toInt))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = action(testRequest.copy(body = "5"))

      actual shouldBe expected
    }

    "return error when action fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Int] = requestBuilder[Int](s => Success(s.toInt))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingAction(validRequest.copy(body = "5"), testError)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Int] = requestBuilder[Int](s => Success(s.toInt))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse)

      val actual = action(validRequest.copy(body = "5"))

      actual shouldBe expected
    }
  }

  "Action with output" should {
    def failingAction(request: TestRequest[String], error: Throwable)(implicit resb: ResponseBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.actionWithOutput[Boolean](request) { ctx =>
        Failure(error)
      }

    def action(request: TestRequest[String])(implicit resb: ResponseBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.actionWithOutput[Boolean](request) { ctx =>
        Success(true)
      }

    "return error when auth fails" in {
      implicit val rb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Try(b.toString))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = action(testRequest)

      actual shouldBe expected
    }

    "return error when action fails" in {
      implicit val rb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Try(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingAction(validRequest, testError)

      actual shouldBe expected
    }

    "return error when converting output fails" in {
      implicit val rb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, _ => Failure(outputFailedError))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "output-failed"))

      val actual = action(validRequest)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val rb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Try(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse.copy(body = "true", headers = Map(Durum.idHeaderName -> testId)))

      val actual = action(validRequest)

      actual shouldBe expected
    }
  }

  "Action with input and output" should {
    def failingAction(request: TestRequest[String], error: Throwable)(implicit reqb: RequestBuilder[Try, TestRequest[String], Int], resb: ResponseBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.actionWithInputAndOutput[Int, Boolean](request) { ctx =>
        Failure(error)
      }

    def action(request: TestRequest[String])(implicit reqb: RequestBuilder[Try, TestRequest[String], Int], resb: ResponseBuilder[Try, Boolean, TestResponse[String]]): Try[TestResponse[String]] =
      testDurum.actionWithInputAndOutput[Int, Boolean](request) { ctx =>
        Success(ctx.body >= 0)
      }

    "return error when converting input fails" in {
      implicit val reqb: RequestBuilder[Try, TestRequest[String], Int]       = requestBuilder[Int](_ => Failure(inputFailedError))
      implicit val resb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Success(b.toString))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "input-failed"))

      val actual = action(testRequest)

      actual shouldBe expected
    }

    "return error when auth fails" in {
      implicit val reqb: RequestBuilder[Try, TestRequest[String], Int]       = requestBuilder[Int](s => Success(s.toInt))
      implicit val resb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(body = "5")

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = action(validRequest)

      actual shouldBe expected
    }

    "return error when action fails" in {
      implicit val reqb: RequestBuilder[Try, TestRequest[String], Int]       = requestBuilder[Int](s => Success(s.toInt))
      implicit val resb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = failingAction(validRequest, testError)

      actual shouldBe expected
    }

    "return error when converting output fails" in {
      implicit val reqb: RequestBuilder[Try, TestRequest[String], Int]       = requestBuilder[Int](s => Success(s.toInt))
      implicit val resb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, _ => Failure(outputFailedError))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "output-failed"))

      val actual = action(validRequest)

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val reqb: RequestBuilder[Try, TestRequest[String], Int]       = requestBuilder[Int](s => Success(s.toInt))
      implicit val resb: ResponseBuilder[Try, Boolean, TestResponse[String]] = responseBuilder[Boolean](200, b => Success(b.toString))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"), body = "5")

      val expected = Success(testResponse.copy(body = "true", headers = Map(Durum.idHeaderName -> testId)))

      val actual = action(validRequest)

      actual shouldBe expected
    }
  }
}
