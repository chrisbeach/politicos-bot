name := "politicos-bot"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.12",
  "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.12",
  "com.typesafe.play" %% "play-ws-standalone-xml" % "1.1.12",
  "com.typesafe" % "config" % "1.3.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

