package com.brightercode.politcobot.discourse

import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import com.brightercode.politcobot.model.Category._
import com.brightercode.politcobot.model.Post._
import com.brightercode.politcobot.model.Topic._
import com.brightercode.politcobot.model.{Category, Post, Topic}
import com.brightercode.politcobot.util.Time._
import com.typesafe.config.Config
import play.api.libs.json.{JsValue, _}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSRequest
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}


class ForumApiClient(val config: Config) {

  val timeout: FiniteDuration = config.getDuration("timeout")
  private implicit val system: ActorSystem = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val wsClient = StandaloneAhcWSClient()
  private val urlBase = config.getString("base")
  private val apiQueryParams = Map(
    "api_key" -> config.getString("key"),
    "api_username" -> config.getString("username")
  )

  def categoriesSync(): Seq[Category] = Await.result(categories(), timeout)

  def categories(): Future[Seq[Category]] =
    httpGet(s"categories.json") { json => {
      json \ "category_list" \ "categories" match {
        case JsDefined(array: JsArray) => array.value.map { _.validate[Category].get }
        case _ => sys.error("Couldn't read categories")
      }
    }}

  def latestTopics(categorySlug: String,
                   page: Int,
                   order: Option[Topic.Order] = None): Future[Seq[Topic]] =
    httpGet(s"/c/$categorySlug.json", extraParams = orderParam(order)) { json => {
      json \ "topic_list" \ "topics" match {
        case JsDefined(array: JsArray) => array.value.map { _.validate[Topic].get }
        case _ => sys.error(s"Couldn't read topics from ${Json.prettyPrint(json)}")
      }
    }}

  private def orderParam(maybeOrder: Option[Topic.Order]) =
    maybeOrder match {
      case Some(Created) => Map("order" -> "created")
      case None => Map.empty[String, String]
    }

  def topic(id: Int): Future[Topic] =
    httpGet(s"/t/$id.json") { json => {
      json.validate[Topic].get
    }}


  def createPost(post: Post): Future[StandaloneWSRequest#Self#Response] =
    url("/posts.json").post(Json.toJson(post))

  def bookmark(topicId: Int): Future[StandaloneWSRequest#Self#Response] =
    url(s"/t/$topicId/bookmark").put(Json.obj())


  private def url(path: String,
                  extraParams: Map[String, String] = Map.empty) =
    wsClient.url(s"$urlBase/$path")
      .withQueryStringParameters((extraParams ++ apiQueryParams).toSeq: _*)

  private def httpGet[T](path: String,
                         extraParams: Map[String, String] = Map.empty)
                        (operation: JsValue => T): Future[T] =
    url(path, extraParams).get()
      .map { response => operation(response.body[JsValue]) }

  def shutdown(): Future[Terminated] = {
    wsClient.close()
    system.terminate()
  }
}
