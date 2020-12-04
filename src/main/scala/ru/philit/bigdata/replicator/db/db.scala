package ru.philit.bigdata.replicator

import cats.effect.{Async, Blocker, ContextShift, IO, Resource, Sync}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import ru.philit.bigdata.replicator.config.DbConfig

package object db {
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

  def createResource(sink: DbConfig, source: DbConfig)(implicit cs: ContextShift[IO]) = IO.pure {
    for {
      source <- makeTransactor[IO](source)
      sink <- makeTransactor[IO](sink)
    } yield (source, sink)
  }

}
