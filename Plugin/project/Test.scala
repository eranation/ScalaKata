import sbt._
import Def.Initialize
import Keys._
import Attributed.data

import java.net.URL
import java.io.File

import spray.revolver.Actions
import spray.revolver.RevolverPlugin.Revolver

object EvalBuild extends Build {

	private lazy val Kata = config("kata")
	private lazy val Backend = config("backend")
	
	val openBrowser = TaskKey[Unit]("open-browser", "task to open browser to kata url")
	val serverReady = TaskKey[Unit]("server-ready", "wait for kata server to be ready")
	val readyPort = SettingKey[Int]("ready-port", "port to send ready command")
	val kataUrl = SettingKey[URL]("kata-url", "url to scala kata")
	val startArgs = TaskKey[Seq[String]]("start-args",
    	"The arguments to be passed to the applications main method when being started")

	lazy val test = Project(
		id = "test",
		base = file("."),
		settings = kataSettings
	)

	lazy val kataSettings = 
		Project.defaultSettings ++
		addCommandAlias("kstart", ";backend:reStart ;backend:openBrowser") ++
		addCommandAlias("kstop", "backend:reStop") ++
		addCommandAlias("krestart", ";backend:reStop ;backend:reStart") ++
		inConfig(Backend)(
			Classpaths.ivyBaseSettings ++
			Classpaths.jvmBaseSettings ++ 
			Defaults.compileBase ++ 
			Defaults.configSettings ++
			Revolver.settings ++
			Seq(
				offline := true,
				mainClass in Revolver.reStart := Some("com.scalakata.backend.Boot"),
				fullClasspath in Revolver.reStart <<= fullClasspath,
				Revolver.reStart <<= InputTask(Actions.startArgsParser) { args =>
					(
						streams,
						Revolver.reLogTag,
						thisProjectRef,
						Revolver.reForkOptions,
						mainClass in Revolver.reStart,
						fullClasspath in Revolver.reStart,
						startArgs in Revolver.reStart,
						args
					).map(Actions.restartApp)
					 .dependsOn(products in Compile)
				},
				kataUrl := new URL("http://localhost:8080"),
				readyPort := 8081,
				serverReady := {
					val socket = new java.net.ServerSocket(readyPort.value)
					socket.accept()
					()
				},
				openBrowser := { 
					serverReady.value
					s"google-chrome ${kataUrl.value.toString}"! 
				},
				libraryDependencies ++= Seq(
					"com.scalakata" % s"backend_${scalaBinaryVersion.value}" % "0.1-20140711T153920",
					"com.scalakata" % s"eval_${scalaBinaryVersion.value}" % "0.1.0-20140710T224721",
					"com.scalakata" % "frontend" % "0.1-20140711T152757"
				)
			)
		) ++
		inConfig(Kata)(
			Classpaths.ivyBaseSettings ++
			Classpaths.jvmBaseSettings ++
			Defaults.configSettings ++
			Defaults.compileBase ++
			Seq(
				offline := true,
				scalaVersion := "2.11.2-SNAPSHOT",
				scalacOptions += "-Yrangepos",
				libraryDependencies ++= Seq(
					"com.scalakata" % s"macro_${scalaBinaryVersion.value}" % "0.1.0-20140710T224721",
					"org.scala-lang" % "scala-compiler" % scalaVersion.value,
					compilerPlugin("org.scalamacros" % s"paradise_${scalaVersion.value}" % "2.1.0-SNAPSHOT")
				)
			)
		) ++
		Seq(
			resolvers ++= Seq(
				Resolver.sonatypeRepo("releases"),
				Resolver.sonatypeRepo("snapshots")
			),
			startArgs in (Backend, Revolver.reStart) := Seq(
				(readyPort in Backend).value.toString,
				((fullClasspath in Compile).value ++ (dependencyClasspath in Kata).value).
					map(_.data).
					map(_.getAbsoluteFile).
					mkString(File.pathSeparator),
				(kataUrl in Backend).value.getHost,
				(kataUrl in Backend).value.getPort.toString
			) ++ (scalacOptions in Kata).value,
			scalaVersion := "2.11.1",
			scalaVersion in Backend := (scalaVersion in Kata).value
		)
}