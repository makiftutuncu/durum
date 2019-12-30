package dev.akif.durum

abstract class Durum[F[+_], REQ, RES, AUTH, CTX[BODY] <: Ctx[REQ, BODY, AUTH]](implicit F: Effect[F]) {
  def getHeadersOfRequest(request: REQ): Map[String, String]

  def getMethodOfRequest(request: REQ): String

  def getURIOfRequest(request: REQ): String

  def buildAuth(request: REQ): F[AUTH]

  def buildContext[IN](id: String,
                       request: REQ,
                       headers: Map[String, String],
                       in: IN,
                       auth: AUTH,
                       time: Long): CTX[IN]

  def getStatusOfResponse(response: RES): Int

  def buildFailedResponse(throwable: Throwable): F[RES]

  def buildFailedResponseAsString(throwable: Throwable): F[String]

  def responseWithHeader(response: RES, header: (String, String)): RES

  def getHeadersOfResponse(response: RES): Map[String, String]

  def logRequest(log: RequestLog, failed: Boolean): String

  def logResponse(log: ResponseLog, failed: Boolean): String

  def basicAction(request: REQ)(action: CTX[Unit] => F[RES]): F[RES] =
    actionImplementation[Unit, RES](
      request,
      _   => F.unit,
      _   => F.pure(""),
      ctx => action(ctx),
      res => F.pure(res),
      _   => F.pure("")
    )

  def actionWithInput[IN](request: REQ)(action: CTX[IN] => F[RES])(implicit requestBuilder: RequestBuilder[F, REQ, IN]): F[RES] =
    actionImplementation[IN, RES](
      request,
      requestBuilder.build,
      requestBuilder.log,
      ctx => action(ctx),
      res => F.pure(res),
      _   => F.pure("")
    )

  def actionWithOutput[OUT](request: REQ, responseStatus: Int = 200)(action: CTX[Unit] => F[OUT])(implicit responseBuilder: ResponseBuilder[F, OUT, RES]): F[RES] =
    actionImplementation[Unit, OUT](
      request,
      _   => F.unit,
      _   => F.pure(""),
      ctx => action(ctx),
      out => responseBuilder.build(responseStatus, out),
      responseBuilder.log
    )

  def actionWithInputAndOutput[IN, OUT](request: REQ, responseStatus: Int = 200)(action: CTX[IN] => F[OUT])(implicit requestBuilder: RequestBuilder[F, REQ, IN],
                                                                                                                     responseBuilder: ResponseBuilder[F, OUT, RES]): F[RES] =
    actionImplementation[IN, OUT](
      request,
      requestBuilder.build,
      requestBuilder.log,
      ctx => action(ctx),
      out => responseBuilder.build(responseStatus, out),
      responseBuilder.log
    )

  protected def actionImplementation[IN, OUT](request: REQ,
                                              getRequestBody: REQ => F[IN],
                                              getRequestBodyAsString: REQ => F[String],
                                              action: CTX[IN] => F[OUT],
                                              buildResponse: OUT => F[RES],
                                              getResponseBodyAsString: OUT => F[String]): F[RES] = {
    val requestHeaders = getHeadersOfRequest(request)
    val requestId      = Ctx.getOrCreateId(requestHeaders)
    val requestTime    = System.currentTimeMillis
    val requestMethod  = getMethodOfRequest(request)
    val requestURI     = getURIOfRequest(request)

    val requestBodyProcessorErrorHandler: Throwable => F[(Either[Throwable, OUT], RES)] = { bodyProcessingError: Throwable =>
      for {
        requestBodyAsString <- getRequestBodyAsString(request)
        failedResponse      <- buildFailedResponse(bodyProcessingError)
      } yield {
        val log = RequestLog(requestMethod, requestURI, requestId, requestTime, requestHeaders, requestBodyAsString)
        logRequest(log, failed = true)
        Left(bodyProcessingError) -> failedResponse
      }
    }

    val requestBodyHandler: IN => F[(Either[Throwable, OUT], RES)] = { in: IN =>
        for {
          requestBodyAsString <- getRequestBodyAsString(request)
          log                  = RequestLog(requestMethod, requestURI, requestId, requestTime, requestHeaders, requestBodyAsString)
          _                    = logRequest(log, failed = false)
          auth                <- buildAuth(request)
          ctx                  = buildContext[IN](requestId, request, requestHeaders, in, auth, requestTime)
          out                 <- action(ctx)
          response            <- buildResponse(out)
        } yield {
          Right(out) -> response
        }
      }

    val requestF: F[(Either[Throwable, OUT], RES)] =
      getRequestBody(request).fold(
        bodyProcessingError => requestBodyProcessorErrorHandler(bodyProcessingError),
        in                  => requestBodyHandler(in)
      )

    val responseF: F[(Either[Throwable, OUT], RES)] =
      requestF.mapError { requestProcessingError: Throwable =>
        buildFailedResponse(requestProcessingError).map { failedResponse =>
          Left(requestProcessingError) -> failedResponse
        }
      }

    val responseAsStringM: Either[Throwable, OUT] => F[(String, Boolean)] = _.fold(
      error => buildFailedResponseAsString(error).map(s => s -> true),
      out   => getResponseBodyAsString(out).map(s => s -> false)
    )

    for {
      resultDataAndResponse          <- responseF
      (errorOrOut, response)          = resultDataAndResponse
      finalResponse                   = responseWithHeader(response, Ctx.idHeaderName -> requestId)
      responseHeaders                 = getHeadersOfResponse(finalResponse)
      responseBodyAndFailed          <- responseAsStringM(errorOrOut)
      (responseBodyAsString, failed)  = responseBodyAndFailed
      responseStatus                  = getStatusOfResponse(finalResponse)
    } yield {
      val log = ResponseLog(responseStatus, requestMethod, requestURI, requestId, requestTime, System.currentTimeMillis, responseHeaders, responseBodyAsString)
      logResponse(log, failed)
      finalResponse
    }
  }
}
