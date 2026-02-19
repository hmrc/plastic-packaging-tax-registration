import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "10.5.0"
  val hmrcMongoVersion = "2.12.0"

  val compile = Seq("uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
                    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
                    "io.circe"          %% "circe-parser"       % "0.14.15",
                    "com.networknt" % "json-schema-validator" % "1.4.0",
                    "org.json"           % "json"               % "20231013"
  )

  val test = Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion,
                 "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
                 "org.scalatest"          %% "scalatest"               % "3.2.15"        ,
                 "org.playframework"      %% "play-test"               % current         ,
                 "org.mockito"             % "mockito-core"            % "5.2.0"         ,
                 "org.scalatestplus"      %% "mockito-5-12"            % "3.2.19.0",  
                 "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.2"        ,
                 "org.playframework"      %% "play-json-joda"          % "3.0.5"
  ).map(_ % "test")

}
