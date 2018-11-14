package tag


import com.alicloud.openservices.tablestore.model._
import play.api.libs.json.{ Writes, Reads, JsNull, Json, JsString, JsValue, JsArray }
import com.alicloud.openservices.tablestore.model._


object JsonParser {

  implicit val rawTagRowWrites = new Writes[Row] {
    def writes(row: Row) = {
      val rawColumnValue = row.getColumns().
                                filter(_.getName == OtsRepo.RAW_VALUE).
                                foldLeft(0.0)(_ + _.getValue.asDouble)

      val valueColumnValue = row.getColumns().
                                  filter(_.getName == OtsRepo.VALUE).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)

      val qualityColumnValue = row.getColumns().
                                  filter(_.getName == OtsRepo.QUALITY).
                                  foldLeft(0L)(_ + _.getValue.asLong)
    
      
      Json.obj(
        "Id" -> Integer.reverse(row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.TAGID).getValue().asLong.toInt ),
        "Time" -> row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.TIME).getValue().asLong,
        "RawValue" -> rawColumnValue,
        "Value" -> valueColumnValue,
        "Quality" -> qualityColumnValue.toInt
      )
    }
  }

  implicit val tagWrites = new Writes[Row] {
    def writes(row: Row) = {
      val rKeyColumnValue = row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.RKEY).getValue().asString.split("_")

      val valueColumnValue = row.getColumns().
                                  filter(_.getName == OtsRepo.VALUE).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)

      val qualityColumnValue = row.getColumns().
                                  filter(_.getName == OtsRepo.QUALITY).
                                  foldLeft(0L)(_ + _.getValue.asLong)
    
      val step = rKeyColumnValue(0).toInt
      val time = rKeyColumnValue(1).toLong

      val costRepr = row.getColumns().
                                  filter(_.getName == OtsRepo.COST_VALUE);

      val costValue = costRepr.foldLeft(0.0)(_ + _.getValue.asDouble)

      val touPeak = row.getColumns().
                                  filter(_.getName == OtsRepo.TOU_PEAK).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)

      val touValley = row.getColumns().
                                  filter(_.getName == OtsRepo.TOU_VALLEY).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)

      val touPlain = row.getColumns().
                                  filter(_.getName == OtsRepo.TOU_PLAIN).
                                  foldLeft(0.0)(_ + _.getValue.asDouble)
      
      val json = Json.obj(
        "Id" -> Integer.reverse(row.getPrimaryKey.getPrimaryKeyColumn(OtsRepo.TAGID).getValue().asLong.toInt ),
        "Step" -> step,
        "Time" -> time,
        "Value" -> valueColumnValue,
        "Quality" -> qualityColumnValue.toInt
      )

      costRepr.size match {
        case 0 => json
        case _ => 
          json ++ Json.obj(
            "CostValue" -> costValue,
            "TouPeak" -> touPeak,
            "TouPlain" -> touPlain,
            "TouValley" -> touValley
          )
      }
      
    }
  }


}

