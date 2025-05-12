import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "8.5.0"
  val hmrcMongoVersion = "2.6.0"

  val compile = Seq("uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
                    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
                    "io.circe"          %% "circe-parser"       % "0.14.5",
                    "io.circe"          %% "circe-json-schema"  % "0.2.0",
                    "org.json"           % "json"               % "20231013"
  )

  val test = Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion,
                 "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
                 "org.scalatest"          %% "scalatest"               % "3.2.15"        ,
                 "org.playframework"      %% "play-test"               % current         ,
                 "org.mockito"             % "mockito-core"            % "5.2.0"         ,
                 "org.mockito"            %% "mockito-scala"           % "1.17.12"       ,
                 "org.scalatestplus"      %% "scalatestplus-mockito"   % "1.0.0-M2"      ,
                 "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"        ,
                 "org.mockito"            %% "mockito-scala-scalatest" % "1.17.14"
  ).map(_ % "test")

}
