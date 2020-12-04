import sbt._

object Dependencies {
  private val doobieVersion     = "0.9.0"
  private val pureconfigVersion = "0.12.3"

  val pureconfig   = "com.github.pureconfig" %% "pureconfig" % pureconfigVersion
  val doobieCore   = "org.tpolecat" %% "doobie-core" % doobieVersion
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % doobieVersion
  val cats4j       = "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
  val logback      = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val postgresql   = "org.postgresql" % "postgresql" % "42.1.4"

  val fs2Cron = "eu.timepit" %% "fs2-cron-core" % "0.2.2"

  val all: Seq[ModuleID] = Seq(
    pureconfig,
    doobieCore,
    doobieHikari,
    cats4j,
    logback,
    postgresql,
    fs2Cron
  )

}
