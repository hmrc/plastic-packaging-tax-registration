import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "8.4.0"
  val hmrcMongoVersion = "1.7.0"

  val compile = Seq("uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
                    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
                    "io.circe"          %% "circe-parser"       % "0.14.5",
                    "io.circe"          %% "circe-json-schema"  % "0.2.0",
                    "org.json"           % "json"               % "20231013"
  )

  val test = Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
                 "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test,
                 "org.scalatest"          %% "scalatest"               % "3.2.15"         % Test,
                 "org.playframework"      %% "play-test"               % current          % Test,
                 "org.mockito"             % "mockito-core"            % "5.2.0"          % Test,
                 "org.mockito"            %% "mockito-scala"           % "1.17.12"        % Test,
                 "org.scalatestplus"      %% "scalatestplus-mockito"   % "1.0.0-M2"       % Test,
                 "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"          % Test,
                 "org.mockito"            %% "mockito-scala-scalatest" % "1.17.14"        % Test
  )

}
