package com.company.foo

import cats.effect.{ExitCode, IO, IOApp}
import com.company.foo.config.AppConfig
import com.company.foo.service.{HttpClientFactory, PostsService}
import com.company.foo.utils.ResultsFormatter
import com.typesafe.scalalogging.LazyLogging

/**
 * Application fetches array-wrapped jsons from an HTTP endpoint
 * and persists them in the local directory in seperate files.
 *
 * See [[com.company.foo.config.AppConfig]] for details about available configuration properties
 * and how to apply them externally.
 */
object Main extends IOApp with LazyLogging {

  /**
   * Application entrypoint.
   *
   * @return application IO
   */
  override def run(args: List[String]): IO[ExitCode] = (for {
    _ <- IO(logger.info("Starting application"))
    config <- IO(AppConfig.create())
    _ <- IO(logger.info(s"Application started with config: $config"))
    httpClientFactory <- IO(new HttpClientFactory())
    postsService <- IO(PostsService.create(
      httpClientFactory,
      config.url,
      config.outputDirectory,
      config.overrideFiles
    ))
    res <- postsService.fetchAndPersist[IO]
  } yield res).redeemWith(
    error => IO(
      logger.error(s"Application failed with: $error")
    ).as(ExitCode.Error),
    result => IO(
      logger.info(
        s"Application finished with following results:\n " +
          s"${ResultsFormatter.formatResult(result)}"
      )
    ).as(
      result match {
        case Right(_) => ExitCode.Success
        case Left(_) => ExitCode.Error
      }
    )
  )

}
