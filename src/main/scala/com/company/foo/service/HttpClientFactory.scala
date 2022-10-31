package com.company.foo.service

import cats.effect.{Async, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

/**
 * Factory producing HTTP clients.
 */
class HttpClientFactory() {
  /**
   * Produces http4s Ember client.
   */
  def build[F[_] : Async]: Resource[F, Client[F]] = EmberClientBuilder.default[F].build
}
