package com.brightercode.politcobot.model

import play.api.libs.json.{JsObject, Json, Writes}

case class Post(topicId: Int, raw: String)

object Post {
  implicit val postWrites: Writes[Post] = new Writes[Post] {
    def writes(post: Post): JsObject =
      Json.obj(
        "topic_id" -> post.topicId,
        "raw" -> post.raw
      )
  }
}
