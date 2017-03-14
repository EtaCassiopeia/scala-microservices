name := MyBuild.NamePrefix + "root"

version := "0.0.1"

scalaVersion := "2.11.8"

lazy val common = project.
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.json)

lazy val fileUploaderService = (project in file("file-uploader-service")).
  dependsOn(common).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.webDependencies).
  enablePlugins(PlayScala)

lazy val fileTransformerService = (project in file("file-transformer-service")).
  dependsOn(common).
  settings(Common.settings: _*).
  settings(libraryDependencies ++= Dependencies.transformerDependencies)

lazy val root = (project in file(".")).
  aggregate(common, fileUploaderService, fileTransformerService)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)
