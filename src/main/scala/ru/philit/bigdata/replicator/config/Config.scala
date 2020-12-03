package ru.philit.bigdata.replicator.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._

object Config {
  def load(): ServiceConfig = ConfigSource.default.loadOrThrow[ServiceConfig]
}

case class DbConfig(url: String, user: String, password: String, driver: String)

case class WorkflowConfig(
                           targetTable: String,
                           tablePrefix: String,
                           system: String,
                           useUpdateFunction: Boolean
                         )

case class ServiceConfig(source: DbConfig, sink: DbConfig, wf: WorkflowConfig)

