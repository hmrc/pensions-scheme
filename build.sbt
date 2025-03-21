import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

lazy val microservice = Project(AppDependencies.appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
    retrieveManaged := true,
    PlayKeys.devSettings += "play.server.http.port" -> "8203",
    scalacOptions += "-Xlint:-missing-interpolator,_",
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-feature"
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;.*FeatureSwitchModule.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := true,
    RoutesKeys.routesImport ++= Seq(
      "models.SchemeReferenceNumber",
      "models.enumeration.SchemeJourneyType"
    )
  )
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf",
    Test / scalacOptions += "-feature"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo,
  ))
  .settings(majorVersion := 0)
  .settings(scalaVersion := "2.13.16")