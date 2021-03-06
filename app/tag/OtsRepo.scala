package tag

import scala.collection.mutable.ListBuffer

import com.alicloud.openservices.tablestore.{SyncClient, AsyncClient, TableStoreCallback}
import com.alicloud.openservices.tablestore.model._
import java.io.BufferedReader
import java.io.InputStream
import java.io.ByteArrayInputStream
import play.api.Logger
import scala.util.Try
import scala.util.control.Breaks
import scala.concurrent.{Future,Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.annotation.{tailrec}
import play.api.libs.json.{ JsNull, Json, JsString, JsValue, JsArray }

object OtsRepo {
  val TAGID = "Id"
  val TIME = "Time"
  val RKEY = "RKey"
  val RAW_VALUE = "RawValue"
  val QUALITY = "Quality"
  val VALUE = "Value"
  val COST_VALUE = "CostValue"
  val TOU_PEAK = "TouPeak"
  val TOU_PLAIN = "TouPlain"
  val TOU_VALLEY = "TouValley"
}

class OtsRepo(endpoint: String, accessKeyId: String, accessKeySecret: String, instanceName: String) {

  private val logger = Logger(getClass)

  private val otsClient = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName)

  def getRawData(tableName: String, id: Long, startTime: Long, endTime: Long): Try[JsValue] = {
    logger.info(s"[get] tableName:$tableName")
    
    Try {
      val rangeIteratorParameter = new RangeIteratorParameter(tableName);

      val realId = Integer.reverse(id.toInt)

      //set range start primary key: id + startTime - 1
      val primaryStartKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
      primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
      primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(startTime - 1))
      rangeIteratorParameter.setInclusiveStartPrimaryKey(primaryStartKeyBuilder.build());

      //set range start primary key: id + endTime
      val primaryEndKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
      primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
      primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(endTime));
      rangeIteratorParameter.setExclusiveEndPrimaryKey(primaryEndKeyBuilder.build());

      rangeIteratorParameter.setMaxVersions(1);

      val iterator = otsClient.createRangeIterator(rangeIteratorParameter);

      var list = ListBuffer[Row]();
      while (iterator.hasNext()) {
          val row = iterator.next();
          list += row
      }
      
      import JsonParser.rawTagRowWrites
      Json.toJson(list)
    }
    
  }

  def saveRawData(tableName: String, jsonBody: JsArray): Try[Int] = {
      Try {

        val batchWriteRowRequest = new BatchWriteRowRequest();
        jsonBody.value.map { json => 
          logger.trace(s"parsing json into row: ${json}")

          val tagId = Integer.reverse((json \ "TagId").as[Int])
          val time = (json \ "Time").as[Long]
          val value = (json \ "Value").as[Double]
          val rawValue = (json \ "RawValue").asOpt[Double] match { 
            case Some(v) => v
            case None => value
          }
          val quality = (json \ "Quality").asOpt[Int] match {
            case Some(v) => v
            case None => 0
          }

          // 构造主键
          val primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(tagId));
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(time));
          
          val rowUpdateChange = new RowUpdateChange(tableName, primaryKeyBuilder.build());
          rowUpdateChange.put(new Column(OtsRepo.VALUE, ColumnValue.fromDouble(value)));
          rowUpdateChange.put(new Column(OtsRepo.RAW_VALUE, ColumnValue.fromDouble(rawValue)));
          rowUpdateChange.put(new Column(OtsRepo.QUALITY, ColumnValue.fromLong(quality)));

          rowUpdateChange

        }.foreach { item =>
          // 添加到batch操作中
          batchWriteRowRequest.addRowChange(item);
        }

        logger.debug(s"saveRawData data is ready")
        val response = otsClient.batchWriteRow(batchWriteRowRequest);

        
        if (!response.isAllSucceed()) {
          logger.error(s"Not all rows are written: fail number is ${response.getFailedRows().size}")
          
          import JsonParser.rawTagRowWrites
          response.getFailedRows().forEach { row => 
            val jsonRow = Json.toJson(row.getRow())
            logger.error(s"row data is ${jsonRow}")
          }
        }

        response.getSucceedRows().size

      }
    }

    def deleteRawData(tableName: String, jsonBody: JsArray): Try[Int] = {

      Try {

        jsonBody.value.map { json => 
          logger.trace(s"parsing json into row: ${json}")

          val tagId = Integer.reverse((json \ "TagId").as[Int])
          val time = (json \ "Time").as[Long]
        
          // 构造主键
          val primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(tagId));
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromLong(time));
  
          val rowDeleteChange = new RowDeleteChange(tableName, primaryKeyBuilder.build());
  
          val result = otsClient.deleteRow(new DeleteRowRequest(rowDeleteChange));

        }

        jsonBody.value.size
      }

    }

    private def getRKey(step: Int, time: Long): String = {
      s"00${step}_${time}"
    }

    def getTagData(tableName: String, id: Long, step: Int, startTime: Long, endTime: Long, 
                          columns: Option[Seq[String]], 
                          isReverse: Option[Boolean], 
                          top: Option[Int]): Try[JsValue] = {
    
      logger.trace(s"[get] tableName:$tableName")
    
      Try {
        val rangeIteratorParameter = new RangeIteratorParameter(tableName);

        val realId = Integer.reverse(id.toInt)

        //set range start primary key: id + RKey(step + startTime - 1)
        val primaryStartKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
        primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
        primaryStartKeyBuilder.addPrimaryKeyColumn(OtsRepo.RKEY, PrimaryKeyValue.fromString(getRKey(step, startTime - 1)))
        rangeIteratorParameter.setInclusiveStartPrimaryKey(primaryStartKeyBuilder.build());

        //set range start primary key: id + RKey(step + endTime)
        val primaryEndKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder
        primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(realId))
        primaryEndKeyBuilder.addPrimaryKeyColumn(OtsRepo.TIME, PrimaryKeyValue.fromString(getRKey(step, endTime)))
        rangeIteratorParameter.setExclusiveEndPrimaryKey(primaryEndKeyBuilder.build());

        rangeIteratorParameter.setMaxVersions(1);

        val iterator = otsClient.createRangeIterator(rangeIteratorParameter);

        var list = ListBuffer[Row]();
        while (iterator.hasNext()) {
            val row = iterator.next();
            list += row
        }
        
        import JsonParser.rawTagRowWrites
        Json.toJson(list)
      }
    
  }

  def saveTagData(tableName: String, jsonBody: JsArray): Try[Int] = {
      Try {

        val batchWriteRowRequest = new BatchWriteRowRequest();
        jsonBody.value.map { json => 
          logger.trace(s"parsing json into row: ${json}")

          val tagId = Integer.reverse((json \ "TagId").as[Int])
          val step = (json \ "Step").as[Int]
          val time = (json \ "Time").as[Long]
          val value = (json \ "Value").as[Double]
          val quality = (json \ "Quality").asOpt[Int] match {
            case Some(v) => v
            case None => 0
          }

          val costValue = (json \ "CostValue").asOpt[Double]
          val touPeakValue = (json \ "TouPeak").asOpt[Double]
          val touPlainValue = (json \ "TouPlain").asOpt[Double]
          val touValleyValue = (json \ "TouValley").asOpt[Double]

          val rKey = getRKey(step, time)
          logger.trace(s"rKey:$rKey")
          // 构造主键
          val primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.TAGID, PrimaryKeyValue.fromLong(tagId));
          primaryKeyBuilder.addPrimaryKeyColumn(OtsRepo.RKEY, PrimaryKeyValue.fromString(rKey));
          
          val rowUpdateChange = new RowUpdateChange(tableName, primaryKeyBuilder.build());
          rowUpdateChange.put(new Column(OtsRepo.VALUE, ColumnValue.fromDouble(value)));
          rowUpdateChange.put(new Column(OtsRepo.QUALITY, ColumnValue.fromLong(quality)));

          costValue match {
            case Some(v) => rowUpdateChange.put(new Column(OtsRepo.COST_VALUE, ColumnValue.fromDouble(v)))
            case None => Unit
          }

          touPeakValue match {
            case Some(v) => rowUpdateChange.put(new Column(OtsRepo.TOU_PEAK, ColumnValue.fromDouble(v)))
            case None => Unit
          }

          touPlainValue match {
            case Some(v) => rowUpdateChange.put(new Column(OtsRepo.TOU_PLAIN, ColumnValue.fromDouble(v)))
            case None => Unit
          }

          touValleyValue match {
            case Some(v) => rowUpdateChange.put(new Column(OtsRepo.TOU_VALLEY, ColumnValue.fromDouble(v)))
            case None => Unit
          }
          
          rowUpdateChange

        }.foreach { item =>
          // 添加到batch操作中
          batchWriteRowRequest.addRowChange(item);
        }

        logger.debug(s"saveTagData data is ready")
        val response = otsClient.batchWriteRow(batchWriteRowRequest);

        
        if (!response.isAllSucceed()) {
          logger.error(s"Not all rows are written: fail number is ${response.getFailedRows().size}")
          
          import JsonParser.tagWrites
          response.getFailedRows().forEach { row => 
            val jsonRow = Json.toJson(row.getRow())
            logger.error(s"row data is ${jsonRow}")
          }
        }

        response.getSucceedRows().size

      }
    }
      
}