package com.brightercode.politcobot

import com.brightercode.politcobot.discourse.ForumApiClient
import com.brightercode.politcobot.model.Topic.Created
import com.brightercode.politcobot.model.{Category, Post, Topic}

import scala.collection.mutable
import scala.concurrent.Await

object ForumApiDecorators {
  implicit class ForumApiDecorator(forum: ForumApiClient) {

    private val postedOnTopicIds: mutable.Set[Int] = mutable.Set.empty

    def postDominationWarning(topicId: Int, category: Category, topicsByAuthor: Int, totalTopics: Int): Boolean = {
      if (postedOnTopicIds.contains(topicId)) {
        false
      } else {
        postedOnTopicIds += topicId
        val post = Post(topicId, raw =
          s":robot: It has not escaped my attention that you created " +
            s"$topicsByAuthor of the last $totalTopics topics in #${category.slug}.\n\n" +
            s"Please give others an opportunity to set the agenda.")
        forum.createPost(post)
        true
      }
    }

    /**
      * Ignore pinned topics (e.g. the "about this category" topic)
      */
    def topicsUnderConsideration(category: Category): Seq[Topic] =
      Await.result(forum.latestTopics(category.slug, page = 0, order = Some(Created)), forum.timeout)
        .filterNot(_.pinned)
  }
}
