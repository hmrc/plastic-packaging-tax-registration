import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.DefaultBuildSettings

val appName = "plastic-packaging-tax-registration"

PlayKeys.devSettings := Seq("play.server.http.port" -> "8502")

val silencerVersion = "1.7.14"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala,SbtDistributablesPlugin)
  .settings(
    libraryDependencySchemes ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // ***************
    // Use the silencer plugin to suppress warnings
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin(
        "com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full
      ),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(scoverageSettings)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List("<empty>",
                                   "Reverse.*",
                                   "domain\\..*",
                                   "models\\..*",
                                   "metrics\\..*",
                                   ".*(BuildInfo|Routes|Options).*"
  ).mkString(";"),
  coverageMinimumStmtTotal := 90.00,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(
    microservice % "test->test"
  ) // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
