
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.*
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "pensions-scheme"

ThisBuild / scalaVersion := "3.6.4"
ThisBuild / majorVersion := 0
ThisBuild / scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=.*redundantly:s",
  "-feature"
)

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    CodeCoverageSettings.settings,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test(),
    retrieveManaged := true,
    PlayKeys.devSettings += "play.server.http.port" -> "8203"
  )
  .settings(
    Test / parallelExecution := true,
    RoutesKeys.routesImport ++= Seq(
      "models.SchemeReferenceNumber",
      "models.enumeration.SchemeJourneyType"
    )
  )
  .settings(
    Test / fork := true,
    Test / javaOptions += "-Dconfig.file=conf/test.application.conf",
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo,
  ))
