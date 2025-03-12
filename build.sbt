import com.typesafe.sbt.packager.docker.*
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

Global / scalaJSStage := FullOptStage
ThisBuild / organization := "us.keatley"
ThisBuild / scalaVersion := "3.6.3"
ThisBuild / version := "1.0.6"

lazy val common = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("common"))
  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val client = project
  .in(file("client"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.8.0",
    scalaJSUseMainModuleInitializer := true
  )
  .dependsOn(common.js)

lazy val server = project
  .in(file("server"))
  .enablePlugins(PlayScala, SbtWeb)
  .settings(
    name := "ham-quiz",
    scalaJSProjects := Seq(client),
    Assets / pipelineStages := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      guice,
      "com.vmunier" %% "scalajs-scripts" % "1.3.0",
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test
    ),
    maintainer := "jckeatley@gmail.com",
    Docker / packageName := "ham-quiz",
    Docker / version := version.value,
    Docker / daemonUserUid := None,
    Docker / daemonUser := "daemon",
    Docker / mappings := {
      val dockerMappings = (Docker / mappings).value
      dockerMappings filter {
        case (file, name) => !name.startsWith("/opt/docker/conf/")
      }
    },
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "azul/zulu-openjdk:17-latest",
    dockerUpdateLatest := true,
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    dockerPermissionStrategy := DockerPermissionStrategy.CopyChown
  )
  .dependsOn(common.jvm)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "us.keatley.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "us.keatley.binders._"
