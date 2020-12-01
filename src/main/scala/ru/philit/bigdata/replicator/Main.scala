package ru.philit.bigdata.replicator

import cats.effect._
import cats.implicits._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari.HikariTransactor
import doobie._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import ru.philit.bigdata.replicator.config.{Config, DbConfig}
import ru.philit.bigdata.replicator.db.{Reader, Writer}

object Main extends IOApp {
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  def makeTransactor[F[_]](config: DbConfig)(implicit F: Async[F], cs: ContextShift[F]): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32) // our connect EC
      te <- Blocker[F]   // our transaction EC
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
        count <- sinkWriter.persistRecords(
          config.wf.targetTable,
          sourceReader.getRecords(config.wf)
        )
        _ <- logger.debug(s"Main -> Write $count records")
      } yield ExitCode.Success
    }
  }
}