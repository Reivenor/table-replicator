package ru.philit.bigdata.replicator

import doobie.{Read, Write}

case class Record(
                   system: String,
                   dictName: String = "null",
                   dictTechName: String,
                   attrName: String = "null",
                   attrTechName: String,
                   recordNum: Int,
                   value: String
                 )

object Record {

  implicit val recordRead: Read[Record] = Read[(String, String, String, String, String, Int, String)].map {
    case (str, str1, str2, str3, str4, int, str5) => Record(str, str1, str2, str3, str4, int, str5)
  }

  implicit val recordWrit: Write[Record] = Write[(String, String, String, String, String, Int, String)].contramap {
    r => Record.unapply(r).get
  }
}
