package ru.philit.bigdata.replicator

import java.time.LocalDateTime

import cats.effect._
import cron4s.Cron
import doobie._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.philit.bigdata.replicator.config.Config
import ru.philit.bigdata.replicator.db.{Reader, Writer}
import ru.philit.bigdata.replicator.cron.Fs2Cron.awakeEveryCron

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  implicit          val cs               = IO.contextShift(ExecutionContexts.synchronous)
  override implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  override def run(args: List[String]): IO[ExitCode] = {
    val config = Config.load()

    val programm = for {
      res <- db.createResource(config.sink, config.source)
      _ <- res.use { case (source, sink) =>
        for {
          logger <- Slf4jLogger.create[IO]
          _ <- logger.debug(s"Main -> Started ${LocalDateTime.now().toString}")
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
            res._2.map(v => s"Main -> Update function finished with result $v")
              .getOrElse("Main -> Update function disabled")
          )
        } yield ()
      }
    } yield ()

    val scheduled = awakeEveryCron[IO](Cron.unsafeParse(config.wf.cron)) >> fs2.Stream.eval(programm)

    for {
      _ <- scheduled.compile.drain
    } yield ExitCode.Error
  }
}