package dev.akif.durum

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HttpLogSpec extends AnyWordSpec with Matchers {
  "Logging an incoming request" should {
    "return String representation of request data" in {
      val now = 0L
      val requestLog = RequestLog("test", now, "GET", "/", Map("foo" -> "bar"), "test", failed = false)

      val expected =
        """Incoming Request
          |< GET /
          |< Id: test
          |< Time: 1970-01-01T00:00Z
          |< foo: bar
          |<
          |< test""".stripMargin

      val actual = requestLog.toLog(LogType.IncomingRequest)

      actual shouldBe expected
    }
  }

  "Logging an outgoing response" should {
    "return String representation of response data" in {
      val responseLog = ResponseLog("test", 0L, "GET", "/", Map("foo" -> "bar"), "test", failed = false, 200, 5L)

      val expected =
        """Outgoing Response
          |> GET /
          |> Id: test
          |> Status: 200
          |> Took: 5 ms
          |> foo: bar
          |>
          |> test""".stripMargin

      val actual = responseLog.toLog(LogType.OutgoingResponse)

      actual shouldBe expected
    }
  }
}
