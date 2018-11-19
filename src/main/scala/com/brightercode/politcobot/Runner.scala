package com.brightercode.politcobot

import com.brightercode.politcobot.ForumApiDecorators._
import com.brightercode.politcobot.discourse.ForumApiClient
import com.brightercode.politcobot.model.Topic
import com.brightercode.politcobot.util.LoopHelper
import com.brightercode.politcobot.util.Time._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory



object Runner extends App with LoopHelper {

  protected val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()

  private val dominationThreshold = config.getDouble("domination_threshold")
  private val minTopics = config.getInt("min_topics")
  private val categoryName = config.getString("category")

  private val forum = new ForumApiClient(config.getConfig("api"))

  private val category =
    forum.categoriesSync()
      .find(_.name == categoryName)
      .getOrElse(sys.error("Category not found"))

  loop(pollInterval = config.getDuration("poll_interval"),
       initialErrorBackoffInterval = config.getDuration("initial_error_backoff_interval"),
       onException = e => logger.error(e.getMessage, e)) {

    val topics = forum.topicsUnderConsideration(category)
    logger.debug(s"Polled:\n\t${topics.mkString("\n\t")}\n")

    if (topics.size >= minTopics) {
      dominationHandling(topics)
    }
  }

  /**
    * Checks if author of latest topic has also authored proportionally more than [domination_threshold] of [topics].
    * If so, and if the topic hasn't previously been bookmarked, create a warning post on the topic and bookmark the
    * topic.
    *
    * @param topics in descending date order
    */
  private def dominationHandling(topics: Seq[Topic]): Unit =
    topics.headOption match {
      case Some(latestTopic) =>
        val topicsByAuthor = topics.count(_.authorUserId == latestTopic.authorUserId)
        logger.debug(s"Latest topic: $latestTopic. Recent topics by same author: $topicsByAuthor / ${topics.size}")

        if (topicsByAuthor.toDouble / topics.size.toDouble >= dominationThreshold) {
          if (!latestTopic.topicPostBookmarked) {
            logger.info(s"Domination detected on $latestTopic. Adding bookmark and creating warning post")
            forum.bookmark(latestTopic.id)
            forum.postDominationWarning(latestTopic.id, categoryName, topicsByAuthor, topics.size)
          } else {
            logger.debug(s"Domination detected on $latestTopic. Taking no action as already bookmarked")
          }
        }
      case None => logger.debug("No topics yet")
    }
}