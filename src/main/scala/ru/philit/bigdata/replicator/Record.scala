package ru.philit.bigdata.replicator

import doobie.{Read, Write}

case class Record(
                   system: String,
                   dictName: Option[String] = None,
                   dictTechName: String,
                   attrName: Option[String] = None ,
                   attrTechName: String,
                   recordNum: Int,
                   value: Option[String] = None
                 )

object Record {

  implicit val recordRead: Read[Record] = Read[(String, Option[String], String, Option[String], String, Int, Option[String])].map {
    case (str, str1, str2, str3, str4, int, str5) => Record(str, str1, str2, str3, str4, int, str5)
  }

  implicit val recordWrit: Write[Record] = Write[(String, Option[String], String, Option[String], String, Int, Option[String])].contramap {
    r => Record.unapply(r).get
  }
}
