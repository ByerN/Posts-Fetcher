package com.company.foo

import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO, Resource}
import com.company.foo.service.{HttpClientFactory, PostsService}
import fs2.Stream
import org.http4s.Response
import org.http4s.client.Client
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec

class PostsServiceSpec extends AnyFunSpec with MockFactory {

  describe("PostsService") {
    val clientFactoryMock = mock[HttpClientFactory]
    val clientMock = mock[Client[IO]]

    describe("when properly configured") {
      val postsService = PostsService.create(
        clientFactoryMock,
        "http://example.com",
        "./test-data",
        true
      )

      it("should process empty JSON array") {
        (clientFactoryMock.build[IO](_: Async[IO])).expects(Async[IO]).returns(Resource.pure(clientMock))
        (clientMock.stream _).expects(*).returns(Stream(Response(body = Stream.emits[IO, Byte](
          """
            |[
            |]
            |""".stripMargin.toSeq.map(_.toByte)
        ))))
        val result = postsService.fetchAndPersist[IO].unsafeRunSync()
        assert(result.isRight)
        assert(result.map(_.isEmpty) === Right(true))
      }

      it("should process JSON array with JSON objects") {
        (clientFactoryMock.build[IO](_: Async[IO])).expects(Async[IO]).returns(Resource.pure(clientMock))
        (clientMock.stream _).expects(*).returns(Stream(Response(body = Stream.emits[IO, Byte](
          """
            |[
            | {
            |   "id": 1,
            |   "otherField": "abc"
            | },
            | {
            |   "id": 2,
            |   "otherField": "abc"
            | },
            | {
            |   "id": 3,
            |   "otherField": "abc"
            | }
            |]
            |""".stripMargin.toSeq.map(_.toByte)
        ))))
        val result = postsService.fetchAndPersist[IO].unsafeRunSync()
        assert(result.isRight)
        assert(result.map(_.size) === Right(3))
      }

      it("should process only JSON objects with id") {
        (clientFactoryMock.build[IO](_: Async[IO])).expects(Async[IO]).returns(Resource.pure(clientMock))
        (clientMock.stream _).expects(*).returns(Stream(Response(body = Stream.emits[IO, Byte](
          """
            |[
            | {
            |   "id": 1,
            |   "otherField": "abc"
            | },
            | {
            | },
            | {
            |   "id": 3,
            |   "otherField": "abc"
            | }
            |]
            |""".stripMargin.toSeq.map(_.toByte)
        ))))
        val result = postsService.fetchAndPersist[IO].unsafeRunSync()
        assert(result.isRight)
        assert(result.map(_.size) === Right(3))
        assert(result.map(_.count(_.isRight)) === Right(2))
        assert(result.map(_.count(_.isLeft)) === Right(1))
      }
    }
  }


}
