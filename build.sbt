import com.typesafe.sbt.packager.docker._

name := """extra-exam"""
organization := "us.keatley"

version := "1.0.5"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.6.3"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

Docker / maintainer := "jckeatley@gmail.com"
Docker / packageName := "extra-exam"
Docker / version := version.value
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
Docker / mappings := {
  val dockerMappings = (Docker / mappings).value
  dockerMappings filter {
    case (file, name) => ! name.startsWith("/opt/docker/conf/")
  }
}
dockerExposedPorts := Seq(8080)
dockerBaseImage := "azul/zulu-openjdk:17-latest"
dockerUpdateLatest := true
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "us.keatley.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "us.keatley.binders._"
