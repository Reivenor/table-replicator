package ru.philit.bigdata.replicator

import cats.effect._
import doobie._
import doobie.hikari.HikariTransactor
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.philit.bigdata.replicator.config.{Config, DbConfig}
import ru.philit.bigdata.replicator.db.{Reader, Writer}

object Main extends IOApp {
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  def makeTransactor[F[_]](config: DbConfig)(implicit F: Async[F], cs: ContextShift[F]): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](10) // our connect EC
      te <- Blocker[F] // our transaction EC
      xa <- HikariTransactor.newHikariTransactor[F](
        config.driver,
        config.url,
        config.user,
        config.password,
        ce,
        te
      )
    } yield xa

  override def run(args: List[String]): IO[ExitCode] = {
    val config    = Config.load()
    val resources = for {
      source <- makeTransactor[IO](config.source)
      sink <- makeTransactor[IO](config.sink)
    } yield (source, sink)

    resources.use { case (source, sink) =>
      for {
        logger <- Slf4jLogger.create[IO]
        _ <- logger.debug("Main -> Started...")
        sourceReader <- IO.pure(new Reader(source))
        sinkWriter <- IO.pure(new Writer(sink))
        _ <- logger.debug(s"Main -> Removing outdated records from ${config.wf.targetTable}")
        _ <- sinkWriter.clearTargetTable(config.wf.targetTable)
        _ <- logger.debug("Main -> Starting migration")
        res <- sinkWriter.persistRecords(
          config.wf.targetTable,
          sourceReader.getRecords(config.wf),
          config.wf.useUpdateFunction
        )
        _ <- logger.debug(s"Main -> Written ${res._1} records")
        _ <- logger.debug(
          res._2.map( v => s"Main -> Update function finished with result $v")
          .getOrElse("Main -> Update function disabled")
        )
      } yield ExitCode.Success
    }
  }
}