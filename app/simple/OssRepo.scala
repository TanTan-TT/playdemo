package simple

import scala.collection.mutable.ListBuffer

import com.aliyun.oss._
import com.aliyun.oss.model.ObjectMetadata
import java.io.BufferedReader
import java.io.InputStream
import java.io.ByteArrayInputStream
import play.api.libs.json.JsValue
import play.api.Logger
import scala.util.Try


class OssRepo(endpoint: String, accessKeyId: String, accessKeySecret: String) {

  private val logger = Logger(getClass)

  private val ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret)
  /**  
   * inputStream to Array[Byte] method  
   **/  
  def inputStreamToByteArray(is: InputStream): Array[Byte] = {  
      val buf = ListBuffer[Byte]()  
      if(is != null) {
        var b = is.read()  
        while (b != -1) {  
            buf.append(b.byteValue)  
            b = is.read()  
        } 
      }
       
      buf.toArray  
      // Resource.fromInputStream(in).byteArray  
  }  

  def get(bucketName: String, objectName: String): Try[(Array[Byte], Map[String, String])] = {
    import scala.collection.JavaConverters._

    logger.info(s"[get] bucketName:$bucketName, objectName: $objectName")

    Try {
      val ossObject = ossClient.getObject(bucketName, objectName)
      val content = ossObject.getObjectContent()
      
      val meta = ossObject.getObjectMetadata()
      var metaData = meta.getUserMetadata();
      logger.info("[get] meta: " + metaData)
      val ret: Array[Byte] = inputStreamToByteArray(content)
      var metaMap: Map[String, String] = Map()
      for((k,v) <- metaData.asScala) {
        metaMap += (k.toString() -> v.toString())
      }
      logger.info("[get] meta converted: " + metaMap)
      
      (ret, metaMap)
    }
    
  }

  def getUrl(ossEndpoint:String, bucketName: String, objectName: String, jsonBody: Option[JsValue] = None): String = {
    jsonBody match {
      case Some(jsonBody) => 
        val height = (jsonBody \ "Height").as[Int]
        val width = (jsonBody \ "Width").as[Int]
        val mode = (jsonBody \ "Mode").as[Int]
        val x = (jsonBody \ "X").as[Int]
        val y = (jsonBody \ "Y").as[Int]
        val typeValue = (jsonBody \ "Type").as[String]
        val compression = (jsonBody \ "Compression").as[Int]
        val resTemplate = s"http://$bucketName.$ossEndpoint/$objectName?x-image-process=image/resize,m_lfit,w_$width,h_$height,limit_0/format,$typeValue"
        resTemplate
      case None =>
        s"http://$bucketName.$ossEndpoint/$objectName"
    }
    
  }

  def post(bucketName: String, objectName: String, bytes: Array[Byte], meta: Map[String, String] = Map()): Try[Int] = {
    import scala.collection.JavaConversions.mapAsScalaMap
    
    val metaObj = new ObjectMetadata();

    for((k,v) <- meta) {
      metaObj.addUserMetadata(k, v)
    }

    logger.info(s"[post] bucketName:$bucketName, objectName: $objectName, metaObj: ${metaObj.getUserMetadata()}")
    
    Try {
      ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes), metaObj)
      200
    }
      
  }
}