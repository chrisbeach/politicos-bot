package com.brightercode.politcobot.model

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

object Category {
  implicit val categoryReads: Reads[Category] =
    (
      (JsPath \ "id").read[Int] and
        (JsPath \ "name").read[String] and
        (JsPath \ "slug").read[String]
      )(Category.apply _)
}

case class Category(id: Int, name: String, slug: String) {
  override def toString: String = s"$name ($id) [$slug]"
}

