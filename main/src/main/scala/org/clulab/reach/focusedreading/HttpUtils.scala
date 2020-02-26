package org.clulab.reach.focusedreading

import com.typesafe.scalalogging.LazyLogging

import scala.io.Source
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{HttpPost, HttpPut}
import org.apache.http.entity.{ContentType, StringEntity}
import scala.util.{Failure, Success, Try}

object HttpUtils extends LazyLogging{

  def using[A <: { def close() }, B](resource:A)(func: A => B): B = {
    val returnValue:B = func(resource)
    resource.close()
    returnValue
  }

  private val endpoint = "http://127.0.0.1:5000"//WHConfig.HttpClient.server

  def httpPut(method:String, data:String)(implicit httpClient:HttpClient):String = {
    val request = new HttpPut(s"$endpoint/$method") // TODO: Parameterize the endpoint
    val content = new StringEntity(data, ContentType.create("text/plain", "UTF-8"))

    request.setEntity(content)

    val response = httpClient.execute(request)

    Try {
      val entity = response.getEntity
      if (entity != null) {
        using(entity.getContent){
          stream =>
            Source.fromInputStream(stream).mkString
        }
      }
      else
        ""
    } match {
      case Success(content) =>
        content
      case Failure(exception) =>
        logger.error(exception.getMessage)
        ""
    }

  }

  def saveModel(name:String)(implicit httpClient:HttpClient):String = {
    val request = new HttpPost(s"$endpoint/save?name=$name")
    val response = httpClient.execute(request)

    Try {
      val entity = response.getEntity
      if (entity != null) {
        using(entity.getContent){
          stream =>
            Source.fromInputStream(stream).mkString
        }
      }
      else
        ""
    } match {
      case Success(content) =>
        content
      case Failure(exception) =>
        logger.error(exception.getMessage)
        ""
    }

  }
}
