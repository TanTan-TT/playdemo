package tag

import scala.collection.mutable.ListBuffer

import com.alicloud.openservices.tablestore.{SyncClient, AsyncClient, TableStoreCallback}
import com.alicloud.openservices.tablestore.model._
import java.io.BufferedReader
import java.io.InputStream
import java.io.ByteArrayInputStream
import play.api.libs.json.JsValue
import play.api.Logger
import scala.util.Try
import scala.util.control.Breaks
import scala.concurrent.{Future,Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.annotation.{tailrec}
import play.api.libs.json.{ JsNull, Json, JsString, JsValue }

object OtsRepo {
  val TAGID = "Id"
  val TIME = "Time"
  val RAW = "Raw"
  val QUALITY = "Quality"
  val VALUE = "Value"
}

class OtsRepo(endpoint: String, accessKeyId: String, accessKeySecret: String, instanceName: String) {

  private val logger = Logger(getClass)

  private val otsClient = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName)
  

  private def recursiveRowList(rangeRowQueryCriteria: RangeRowQueryCriteria): List[Row] = {
    import scala.collection.JavaConverters._
    
    val res = otsClient.getRange(new GetRangeRequest(rangeRowQueryCriteria))
    if(res.getNextStartPrimaryKey() != null){
      rangeRowQueryCriteria.setInclusiveStartPrimaryKey(res.getNextStartPrimaryKey())
      return res.getRows.asScala.toList ::: recursiveRowList(rangeRowQueryCriteria)
    }
    return res.getRows.asScala.toList
    
  }

  def getRawData(tableName: String, id: Int, startTime: Long, endTime: Long): Try[List[Row]] = {
    logger.info(s"[get] tableName:$tableName")
    
    Try {
      val rangeRowQueryCriteria = new RangeRowQueryCriteria(tableName);

      val realId = Integer.reverse(id)

      //set range start primary key: id + startTime - 1
      val primaryStartKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
      primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
      primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(startTime - 1))
      rangeRowQueryCriteria.setInclusiveStartPrimaryKey(primaryStartKeyBuilder.build());

      //set range start primary key: id + endTime
      val primaryEndKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
      primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
      primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(endTime));
      rangeRowQueryCriteria.setExclusiveEndPrimaryKey(primaryEndKeyBuilder.build());

      rangeRowQueryCriteria.setMaxVersions(1);
      
      recursiveRowList(rangeRowQueryCriteria)
    }
    
  }

}