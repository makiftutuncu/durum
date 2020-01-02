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
    "return error when auth fails" in {
      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = testDurum.basicAction(testRequest) { ctx =>
        Success(testResponse)
      }

      actual shouldBe expected
    }

    "return error when action fails" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = testDurum.basicAction(validRequest) { ctx =>
        Failure(testError)
      }

      actual shouldBe expected
    }

    "perform action and return response" in {
      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse)

      val actual = testDurum.basicAction(validRequest) { ctx =>
        Success(testResponse)
      }

      actual shouldBe expected
    }
  }

  "Action with input" should {
    "return error when converting input fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Boolean] = requestBuilder[Boolean](_ => Failure(inputFailedError))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "input-failed"))

      val actual = testDurum.actionWithInput[Boolean](testRequest) { ctx =>
        Success(testResponse)
      }

      actual shouldBe expected
    }

    "return error when auth fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Boolean] = requestBuilder[Boolean](s => Try(s.toBoolean))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "auth-failed"))

      val actual = testDurum.actionWithInput[Boolean](testRequest.copy(body = "true")) { ctx =>
        Success(testResponse)
      }

      actual shouldBe expected
    }

    "return error when action fails" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Boolean] = requestBuilder[Boolean](s => Try(s.toBoolean))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(TestResponse(500, Map(Durum.idHeaderName -> testId), "test"))

      val actual = testDurum.actionWithInput[Boolean](validRequest.copy(body = "true")) { ctx =>
        Failure(testError)
      }

      actual shouldBe expected
    }

    "perform action and return response" in {
      implicit val rb: RequestBuilder[Try, TestRequest[String], Boolean] = requestBuilder[Boolean](s => Try(s.toBoolean))

      val validRequest = testRequest.copy(headers = Map("Authorization" -> "test:test"))

      val expected = Success(testResponse)

      val actual = testDurum.actionWithInput[Boolean](validRequest.copy(body = "true")) { ctx =>
        Success(testResponse)
      }

      actual shouldBe expected
    }
  }
}
