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


}

