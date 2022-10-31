package com.company.foo.utils

import com.company.foo.service.PostsService.ResultsOrError

/**
 * Helper class used for formatting processing results.
 */
object ResultsFormatter {

  /**
   * Formats processing result into more human-readable string.
   *
   * @param result to format
   * @return formatted string
   */
  def formatResult(result: ResultsOrError): String = {
    result match {
      case Left(error) => s"Processing failed with error: $error"
      case Right(value) =>
        s"""Processed files count:  ${value.size.toString}
           |Failed files count:     ${value.count(_.isLeft)}
           |Success files count:    ${value.count(_.isRight)}
           |
           |Result listing:
           |${
          value.map {
            case Left(localError) => s"FAILED:  $localError"
            case Right(createdFilePath) => s"SUCCESS: $createdFilePath"
          }.mkString("\n")
        }
           |""".stripMargin
    }
  }
}
