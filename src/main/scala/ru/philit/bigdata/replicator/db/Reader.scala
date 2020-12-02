package ru.philit.bigdata.replicator.db

import cats.effect._
import cats.implicits._
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import fs2.Stream
import ru.philit.bigdata.replicator.Record
import ru.philit.bigdata.replicator.config.WorkflowConfig
import ru.philit.bigdata.replicator.db.Reader.{flattenRowsStreamConstructor, getTablesListSql}

final class Reader(tnx: HikariTransactor[IO]) {
  private[db] def getTablesList(prefix: String): Stream[IO, String] =
    getTablesListSql(prefix).stream.transact(tnx)

  private[db] def flattenRows(tableName: String, system: String): Stream[IO, Record] =
    flattenRowsStreamConstructor(tableName)
      .transact(tnx)
      .map { case (key, value, num) =>
        Record(
          system = system,
          dictTechName = tableName,
          attrTechName = key.getOrElse("null"),
          recordNum = num,
          value = value.getOrElse("null")
        )
      }

  def getRecords(conf: WorkflowConfig): Stream[IO, Record] =
    getTablesList(conf.tablePrefix).flatMap(flattenRows(_, conf.system))
}



object Reader {
  def apply(tnx: HikariTransactor[IO]): Reader = new Reader(tnx)

  private[db] val getTablesListSql = (prefix: String) =>
    sql""" select table_name from information_schema.tables where table_name like ${prefix}"""
      .query[String]

  private[db] def flattenRowsStreamConstructor(tableName: String) = {
    val sql =
      s"""
              select r.key, r.value, js_num.num
               from (
                select js.data, row_number() over (order by js.data ->> 'value') as num
                from(select row_to_json(t, false) as "data" from $tableName t) js
               ) js_num
               join json_each_text(js_num.data) r on true
    """
    HC.stream[(Option[String], Option[String], Int)](sql, ().pure[PreparedStatementIO], 512)
  }

}
