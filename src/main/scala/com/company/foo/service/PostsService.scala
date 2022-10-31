package com.company.foo.service

import cats.Applicative
import cats.data.EitherT
import cats.effect._
import cats.syntax.all._
import com.company.foo.service.PostsService._
import com.typesafe.scalalogging.LazyLogging
import fs2.Stream
import io.circe.jawn.CirceSupportParser
import io.circe.{Decoder, Json}
import org.http4s._
import org.http4s.ember.client._
import org.typelevel.jawn.Facade
import org.typelevel.jawn.fs2._

import java.io.PrintWriter
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

/**
 * Provides functionality for fetching jsons from the HTTP server.
 *
 * Used server endpoint has to:
 * - be HTTP GET endpoint,
 * - respond with json array containing json objects,
 * - each object has to contain "id" field (JSON Number type)
 */
object PostsService {
  val idFieldName = "id"
  val fileExtension = ".json"

  type PersistedFileName = String
  type FileResultOrError = Either[Throwable, PersistedFileName]
  type ResultsOrError = Either[Throwable, Seq[FileResultOrError]]
  type FilePathOrError = Either[Throwable, String]

  /**
   * Factory method to create [[com.company.foo.service.PostsService]] class instance.
   *
   * @param httpClientFactory factory producing http clients.
   *
   * See [[com.company.foo.config.AppConfig]] for other parameters description.
   * @return created service.
   */
  def create(httpClientFactory: HttpClientFactory,
             urlString: String,
             outputDirectory: String,
             overrideFiles: Boolean): PostsService = {
    Uri.fromString(urlString) match {
      case Right(uri) =>
        new PostsService(
          httpClientFactory,
          uri,
          outputDirectory,
          overrideFiles
        )
      case _ =>
        throw new IllegalArgumentException("Provided incorrect URI")
    }
  }
}

/**
 * @param httpClientFactory factory producing http clients.
 * See [[com.company.foo.config.AppConfig]] for other parameters description.
 */
class PostsService private(private[this] val httpClientFactory: HttpClientFactory,
                           private[this] val uri: Uri,
                           private[this] val outputDirectory: String,
                           private[this] val overrideFiles: Boolean) extends LazyLogging {
  private[this] implicit val circleJsonFacade: Facade[Json] = new CirceSupportParser(
    None,
    allowDuplicateKeys = false
  ).facade

  /**
   * Fetches jsons from HTTP GET endpoint and persists them in the local filesystem as separate files.
   *
   * As server API doesn't provide any pagination functionality, this method utilizes streaming approach
   * for scalability (avoiding memory issues).
   *
   * @return wrapped sequence of created file paths or errors.
   */
  def fetchAndPersist[F[_] : Async]: F[ResultsOrError] = {
    Stream
      .resource(httpClientFactory.build[F])
      .flatMap(_.stream(Request[F](Method.GET, uri)).flatMap(_.body.chunks.unwrapJsonArray))
      .evalMap(persistJson[F])
      .compile
      .fold(Seq[FileResultOrError]()) {
        case (resultList, computationResult) => resultList :+ computationResult
      }
      .attempt
  }

  private[this] def persistJson[F[_] : MonadCancelThrow](json: Json): F[FileResultOrError] = {
    (for {
      outputDirPath <- EitherT(createOutputDirIfNotExists(outputDirectory))
      jsonId <- EitherT(getFieldValue[F, Long](json, idFieldName))
      outputFilePath <- EitherT(createOutputFilePath[F, Long](jsonId, outputDirPath))
      res <- EitherT(writeToFile[F](json, outputFilePath))
    } yield res).value
  }

  private[this] def getFieldValue[F[_] : Applicative, T: Decoder](json: Json,
                                                                  fieldName: String): F[Either[Throwable, T]] =
    Applicative[F].unit.map { _ =>
      logger.info("Getting ID from json")
      logger.debug(s"Json body: $json")
      json.findAllByKey(fieldName).map(_.as[T]).headOption match {
        case Some(res@Right(id)) =>
          logger.info(s"Json id: $id")
          res
        case Some(error@Left(_)) =>
          logger.info("Cannot parse id field")
          error
        case None =>
          logger.info("Id field does not exist")
          new RuntimeException("Id field not available").asLeft
      }
    }

  private[this] def createOutputDirIfNotExists[F[_] : Applicative](outputDirStr: String): F[Either[Throwable, Path]] =
    Applicative[F].unit.map { _ =>
      Try {
        val outputPath = Path.of(outputDirStr).toAbsolutePath
        if (!Files.exists(outputPath)) {
          logger.info(s"Directory: $outputPath does not exist - creating.")
          Files.createDirectory(outputPath)
        }
        outputPath
      }.toEither
    }

  private[this] def createOutputFilePath[F[_] : Applicative, T](jsonId: T,
                                                                outputPath: Path): F[FilePathOrError] =
    Applicative[F].unit.map { _ =>
      val outputFilePath = Paths.get(outputPath.toString, jsonId.toString + fileExtension)
      if (overrideFiles || !Files.exists(outputFilePath)) {
        outputFilePath.toString.asRight
      } else {
        logger.info("Cannot create file - already exists and overriding is disabled")
        new RuntimeException(s"File $jsonId already exists and overriding is disabled").asLeft
      }
    }

  private[this] def writeToFile[F[_] : MonadCancelThrow](json: Json,
                                                         outputFilePath: String): F[FileResultOrError] = {
    Resource.make(new PrintWriter(outputFilePath).pure[F])(writer => Applicative[F].unit.map { _ =>
      logger.info("Closing file writer")
      writer.close()
      logger.info("File writer closed")
    }).use(writer => Applicative[F].unit.map { _ =>
      logger.info(s"Writing to file: $outputFilePath")
      writer.write(json.toString())
      writer.flush()
      logger.info(s"Writing finished")
      outputFilePath
    }).attempt
  }
}