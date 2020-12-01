package ru.philit.bigdata.replicator.db

import cats.effect.{ContextShift, IO}
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import fs2.{Chunk, Stream}
import ru.philit.bigdata.replicator.Record
import ru.philit.bigdata.replicator.Record._
import ru.philit.bigdata.replicator.db.Writer.insertRecordSql

final class Writer(xa: HikariTransactor[IO]) {
  private[db] def persist(tableName: String, records: Chunk[Record]): IO[Int] = {
    val sql = insertRecordSql(tableName)
      Update[Record](sql)
        .updateMany(records)
        .transact(xa)
  }

  def persistRecords(tableName: String, stream: Stream[IO, Record])(implicit cs: ContextShift[IO]): IO[Int] =
    stream.chunks.parEvalMap(8) { chunk =>
      persist(tableName, chunk)
    }.compile.toList.map(_.sum)
}

  object Writer {
    def apply(tnx: HikariTransactor[IO]): Writer = new Writer(tnx)

    private[db] val insertRecordSql = (tableName: String) =>
      s"insert into $tableName (system, dict_name, dict_tech_name, attr_name, attr_tech_name, record_num, value) values (?, ?, ?, ?, ?, ?, ?)"
  }


