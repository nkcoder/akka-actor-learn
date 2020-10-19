name := "akka-actor-learn"

version := "0.1"

scalaVersion := "2.13.3"

val akkaVersion = "2.6.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

// scala style
lazy val compileScalaStyle = taskKey[Unit]("compileScalaStyle")
compileScalaStyle := scalastyle.in(Compile).toTask("").value
(compile in Compile) := ((compile in Compile) dependsOn compileScalaStyle).value

// scapegoat
scapegoatVersion in ThisBuild := "1.3.11"




