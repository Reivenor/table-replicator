
name := "table_replicator_catz"

organization := "ru.phil-it"

version := "0.1"

scalaVersion := "2.13.4"

lazy val project = Project("table-replicator", file("./"))
  .enablePlugins(AssemblyPlugin)
  .settings(
    version := version.value,
    scalaVersion := scalaVersion.value,
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies := Dependencies.all,
    mainClass := Some("ru.philit.bigdata.replicator.Main"),
    assemblyJarName := s"${name.value}-${version.value}.jar",
    assemblyOutputPath in assembly := file(s"./build/${assemblyJarName.value}"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    fork in run := true
  )