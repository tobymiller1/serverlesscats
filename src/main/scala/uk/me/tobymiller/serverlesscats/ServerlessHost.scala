package uk.me.tobymiller.serverlesscats

import cats._
import cats.data._
import cats.implicits._
import java.io.OutputStream
import java.io.InputStream
import com.amazonaws.services.lambda.runtime.Context
import io.circe._, io.circe.parser._
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.language.postfixOps
import cats.effect.IO
import java.io.IOException
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import com.amazonaws.services.lambda.runtime.RequestStreamHandler

class ServerlessHost(handler: Handler) extends RequestStreamHandler {
  def handleRequest(is: InputStream, os: OutputStream, context: Context): Unit = {
    val reader = new BufferedReader(new InputStreamReader(is))
    val writer = new BufferedWriter(new OutputStreamWriter(os))
    val result = for {
      inString <- Either.catchOnly[IOException](Iterator continually reader.readLine takeWhile (_ != null) mkString)
      json <- parse(inString)
      jsonIo = IO.pure(json)
      handled = handler.handle(jsonIo)
      outString = handled.unsafeRunSync().toString()
      _ <- Either.catchOnly[IOException](writer.write(outString))
      _ <- Either.catchOnly[IOException](writer.flush())
      written <- Either.catchOnly[IOException](writer.close())
    } yield written
    result.left.foreach(e => throw e)
  }
}