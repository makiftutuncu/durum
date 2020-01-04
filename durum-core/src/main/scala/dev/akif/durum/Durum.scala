package dev.akif.durum

import java.util.UUID

import dev.akif.durum.syntax._

/**
 * Dürüm (Turkish for wraps, as in food) is an HTTP wrapper for Scala.
 *
 * It helps generalize the behavior of handling an HTTP request in a server application.
 * Dürüm provides abstractions for common operations like logging, timing, authorization etc.
 * It has no external dependency and expects you to wire-in concrete types for HTTP request and response objects.
 *
 * @param F Implicit instance of the effect
 *
 * @tparam F    Type of the effect, see [[dev.akif.durum.Effect]]
 * @tparam E    Type of the error with which the effect can fail
 * @tparam REQ  Type of HTTP request Dürüm accpets
 * @tparam RES  Type of HTTP response Dürüm produces
 * @tparam AUTH Type of authorization data Dürüm builds from request
 * @tparam CTX  Type of context object Dürüm builds for request
 */
abstract class Durum[F[+_], E, REQ, RES, AUTH, CTX[BODY] <: Ctx[REQ, BODY, AUTH]](implicit F: Effect[F, E]) {
  /**
   * Instance of an [[dev.akif.durum.OutputBuilder]] for effect errors so that failed responses can be built
   */
  val errorOutputBuilder: OutputBuilder[F, E, RES]

  /**
   * Gets headers of an HTTP request as a Map
   *
   * @param request An HTTP request
   *
   * @return Headers of the request as a Map
   */
  def getHeadersOfRequest(request: REQ): Map[String, String]

  /**
   * Gets method of an HTTP request (GET, POST etc.)
   *
   * @param request An HTTP request
   *
   * @return Method of the request
   */
  def getMethodOfRequest(request: REQ): String

  /**
   * <p>Gets URI of an HTTP request</p>
   *
   * <p>For example, URI of "https://example.com:8090/test?foo=bar" is "/test?foo=bar"</p>
   *
   * @param request An HTTP request
   *
   * @return URI of the request
   */
  def getURIOfRequest(request: REQ): String

  /**
   * Builds authorization data from an HTTP request
   *
   * @param request An HTTP request
   *
   * @return Authorization data built from the request inside the effect
   */
  def buildAuth(request: REQ): F[AUTH]

  /**
   * Builds an HTTP request context
   *
   * @param id      An identifier for the request
   * @param time    Time when the request is received
   * @param request HTTP request object itself
   * @param headers Headers of the request as a Map
   * @param in      Input data built from the request
   * @param auth    Authorization data built from the request
   *
   * @tparam IN Type of the input data built from request
   *
   * @return The context that's built
   */
  def buildContext[IN](id: String,
                       time: Long,
                       request: REQ,
                       headers: Map[String, String],
                       in: IN,
                       auth: AUTH): CTX[IN]

  /**
   * Gets status code of an HTTP response (200 for OK etc.)
   *
   * @param response An HTTP response
   *
   * @return Status code of the response
   */
  def getStatusOfResponse(response: RES): Int

  /**
   * Gets status code of the HTTP response (200 for OK etc.) to be built for an error
   *
   * @param e An error
   *
   * @return Status code of the response to be built for the error
   */
  def getStatusOfError(e: E): Int

  /**
   * Adds a header to an HTTP response
   *
   * @param response  An HTTP response
   * @param header    A header as a String [[scala.Tuple2]]
   *
   * @return HTTP response with given header
   */
  def responseWithHeader(response: RES, header: (String, String)): RES

  /**
   * Gets headers of an HTTP response as a Map
   *
   * @param response An HTTP response
   *
   * @return Headers of the response as a Map
   */
  def getHeadersOfResponse(response: RES): Map[String, String]

  /**
   * Logs request with given data
   *
   * @param log Data to log about request
   */
  def logRequest(log: RequestLog): Unit

  /**
   * Logs response with given data
   *
   * @param log Data to log about response
   */
  def logResponse(log: ResponseLog): Unit

  /**
   * Gets existing id from [[dev.akif.durum.Durum#idHeaderName]] header or creates a new id value
   *
   * @param headers HTTP headers from which to get id
   *
   * @return Existing or created request id
   */
  def getOrCreateId(headers: Map[String, String]): String = headers.getOrElse(Durum.idHeaderName, UUID.randomUUID.toString)

  /**
   * Wraps a basic user action where there is no conversion required/done on input of request and output of response
   *
   * @param request An HTTP request
   * @param action  User action to wrap
   *
   * @return HTTP response produced inside the effect
   */
  def wrap(request: REQ)(action: CTX[Unit] => F[RES]): F[RES] =
    wrapImplementation[Unit, RES](
      request,
      _   => F.unit,
      _   => F.pure(""),
      ctx => action(ctx),
      res => F.pure(res),
      _   => F.pure("")
    )

  /**
   * Wraps a user action where input of request is converted but there is no conversion required/done on output of response
   *
   * @param request An HTTP request
   * @param action  User action to wrap
   *
   * @return HTTP response produced inside the effect
   */
  def wrapWithInput[IN](request: REQ)(action: CTX[IN] => F[RES])(implicit inputBuilder: InputBuilder[F, REQ, IN]): F[RES] =
    wrapImplementation[IN, RES](
      request,
      inputBuilder.build,
      inputBuilder.log,
      ctx => action(ctx),
      res => F.pure(res),
      _   => F.pure("")
    )

  /**
   * Wraps a user action where there is no conversion required/done on input of request and output of response is converted
   *
   * @param request         An HTTP request
   * @param responseStatus  HTTP status code to be used while building the response from output data
   * @param action          User action to wrap
   *
   * @return HTTP response produced inside the effect
   */
  def wrapWithOutput[OUT](request: REQ, responseStatus: Int = Durum.successfulStatus)(action: CTX[Unit] => F[OUT])(implicit outputBuilder: OutputBuilder[F, OUT, RES]): F[RES] =
    wrapImplementation[Unit, OUT](
      request,
      _   => F.unit,
      _   => F.pure(""),
      ctx => action(ctx),
      out => outputBuilder.build(responseStatus, out),
      outputBuilder.log
    )

  /**
   * Wraps a user action where a conversion is applied to both input of request and output of response
   *
   * @param request         An HTTP request
   * @param responseStatus  HTTP status code to be used while building the response from output data
   * @param action          User action to wrap
   *
   * @return HTTP response produced inside the effect
   */
  def wrapWithInputAndOutput[IN, OUT](request: REQ, responseStatus: Int = Durum.successfulStatus)(action: CTX[IN] => F[OUT])(implicit inputBuilder: InputBuilder[F, REQ, IN], outputBuilder: OutputBuilder[F, OUT, RES]): F[RES] =
    wrapImplementation[IN, OUT](
      request,
      inputBuilder.build,
      inputBuilder.log,
      ctx => action(ctx),
      out => outputBuilder.build(responseStatus, out),
      outputBuilder.log
    )

  protected def wrapImplementation[IN, OUT](request: REQ,
                                            getInput: REQ => F[IN],
                                            getInputAsString: REQ => F[String],
                                            action: CTX[IN] => F[OUT],
                                            buildResponse: OUT => F[RES],
                                            getOutputAsString: OUT => F[String]): F[RES] = {
    val requestHeaders = getHeadersOfRequest(request)
    val requestId      = getOrCreateId(requestHeaders)
    val requestTime    = System.currentTimeMillis
    val requestMethod  = getMethodOfRequest(request)
    val requestURI     = getURIOfRequest(request)

    val inputErrorHandler: E => F[(Either[E, OUT], RES)] = { inputError: E =>
      for {
        inputAsString  <- getInputAsString(request)
        errorStatus     = getStatusOfError(inputError)
        failedResponse <- errorOutputBuilder.build(errorStatus, inputError)
      } yield {
        val log = RequestLog(requestId, requestTime, requestMethod, requestURI, requestHeaders, inputAsString, failed = true)
        logRequest(log)
        Left(inputError) -> failedResponse
      }
    }

    val inputHandler: IN => F[(Either[E, OUT], RES)] = { in: IN =>
        for {
          inputAsString <- getInputAsString(request)
          log            = RequestLog(requestId, requestTime, requestMethod, requestURI, requestHeaders, inputAsString, failed = false)
          _              = logRequest(log)
          auth          <- buildAuth(request)
          ctx            = buildContext[IN](requestId, requestTime, request, requestHeaders, in, auth)
          out           <- action(ctx)
          response      <- buildResponse(out)
        } yield {
          Right(out) -> response
        }
      }

    val requestF: F[(Either[E, OUT], RES)] =
      getInput(request).fold(
        inputError => inputErrorHandler(inputError),
        in         => inputHandler(in)
      )

    val responseF: F[(Either[E, OUT], RES)] =
      requestF.mapError { requestError: E =>
        val errorStatus = getStatusOfError(requestError)
        errorOutputBuilder.build(errorStatus, requestError).map { failedResponse =>
          Left(requestError) -> failedResponse
        }
      }

    val responseAsStringM: Either[E, OUT] => F[(String, Boolean)] = _.fold(
      error => errorOutputBuilder.log(error).map(s => s -> true),
      out   => getOutputAsString(out).map(s => s -> false)
    )

    for {
      responseDataAndResponse   <- responseF
      (errorOrOutput, response)  = responseDataAndResponse
      finalResponse              = responseWithHeader(response, Durum.idHeaderName -> requestId)
      responseHeaders            = getHeadersOfResponse(finalResponse)
      outputAndFailed           <- responseAsStringM(errorOrOutput)
      (outputAsString, failed)   = outputAndFailed
      responseStatus             = getStatusOfResponse(finalResponse)
    } yield {
      val log = ResponseLog(requestId, requestTime, requestMethod, requestURI, responseHeaders, outputAsString, failed, responseStatus, System.currentTimeMillis)
      logResponse(log)
      finalResponse
    }
  }
}

object Durum {
  /**
   * <p>Name of the request identifier header to be used in requests and responses</p>
   *
   * <p>If this header is present in the request, it will be used. Otherwise a new id will be created.</p>
   */
  val idHeaderName: String = "X-Id"

  /** HTTP status code to use by default for a successful response */
  val successfulStatus: Int = 200

  /** HTTP status code to use by default for a failed response */
  val failedStatus: Int = 500
}
