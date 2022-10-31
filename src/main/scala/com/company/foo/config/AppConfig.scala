package com.company.foo.config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

/**
 * Application configuration wrapper.
 *
 * External config file can be applied with arg:
 * -Dconfig.file=./my-external-application.properties
 *
 * Configuration properties:
 * input.url - HTTP GET endpoint providing jsons to process.
 * output.dir - local path to the output directory.
 * processing.override-files - true to make application override existing json files.
 */
object AppConfig extends LazyLogging {

  /**
   * Factory method for [[com.company.foo.config.AppConfig]] class instance.
   *
   * Configuration file must comply with format acceptable by [[com.typesafe.config.Config]].
   *
   * @return application configuration.
   */
  def create(): AppConfig = {
    val config = ConfigFactory.load
    AppConfig(
      url = config.getString("input.url"),
      outputDirectory = config.getString("output.dir"),
      overrideFiles = config.getBoolean("processing.override-files")
    )
  }
}

case class AppConfig(url: String,
                     outputDirectory: String,
                     overrideFiles: Boolean)
