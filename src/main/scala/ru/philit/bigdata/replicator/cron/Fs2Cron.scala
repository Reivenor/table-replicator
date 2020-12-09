package ru.philit.bigdata.replicator.cron

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import cats.ApplicativeError
import cats.effect.{Sync, Timer}
import cron4s.expr.CronExpr
import cron4s.lib.javatime._
import fs2.Stream

import scala.concurrent.duration.FiniteDuration

object Fs2Cron {
  def awakeEveryCron[F[_]: Sync](cronExpr: CronExpr)(implicit timer: Timer[F]): Stream[F, Unit] =
    sleepCron(cronExpr).repeat

  /** Creates a single element stream that waits until the next
   * date-time that matches `cronExpr` before emitting unit.
   */
  def sleepCron[F[_]: Sync](cronExpr: CronExpr)(implicit timer: Timer[F]): Stream[F, Unit] =
    durationFromNow(cronExpr).flatMap(Stream.sleep[F])

  /** Creates a single element stream of the duration between the
   * current date-time and the next date-time that matches `cronExpr`.
   */
  def durationFromNow[F[_]: Sync](cronExpr: CronExpr): fs2.Stream[F, FiniteDuration] =
    evalNow.flatMap(now => durationFrom(now, cronExpr))


  /** Creates a single element stream of the duration between `from`
   * and the next date-time that matches `cronExpr`.
   */
  def durationFrom[F[_]](
                          from: LocalDateTime,
                          cronExpr: CronExpr
                        )(implicit F: ApplicativeError[F, Throwable]): Stream[F, FiniteDuration] =
    cronExpr.next(from) match {
      case Some(next) =>
        val durationInMillis = from.until(next, ChronoUnit.MILLIS)
        Stream.emit(FiniteDuration(durationInMillis, TimeUnit.MILLISECONDS))
      case None =>
        val msg = s"Could not calculate the next date-time from $from " +
          s"given the cron expression '$cronExpr'. This should never happen."
        Stream.raiseError(new Throwable(msg))
    }

  /** Creates a single element stream of the current date-time. */
  def evalNow[F[_]](implicit F: Sync[F]): Stream[F, LocalDateTime] =
    Stream.eval(F.delay(LocalDateTime.now))
}
