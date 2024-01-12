import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.15.0"
  val hmrcMongoVersion = "1.2.0"

  val compile = Seq("uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
                    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
                    "com.typesafe.play" %% "play-json-joda"     % "2.9.4",
                    "io.circe" %% "circe-parser" % "0.14.5",
                    "io.circe" %% "circe-json-schema" % "0.2.0",
                    "org.json" % "json" % "20231013"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % Test,
    "org.scalatest"          %% "scalatest"               % "3.2.5"    % Test,
    "com.typesafe.play"      %% "play-test"               % current    % Test,
    "org.mockito"             % "mockito-core"            % "3.9.0"    % Test,
    "org.mockito"            %% "mockito-scala"           % "1.17.12"  % Test,
    "org.scalatestplus"      %% "scalatestplus-mockito"   % "1.0.0-M2" % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.36.8"   % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"    % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"           % "2.26.3"   % "test, it",
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.14"  % Test
  )

}
