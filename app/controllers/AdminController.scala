package controllers

import com.daumkakao.s2graph.core._
import com.daumkakao.s2graph.core.models.{HLabel, HLabelMeta, HService}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}


object AdminController extends Controller with RequestParser {

  /**
   * Management
   */
  def getService(serviceName: String) = Action { request =>
    Management.findService(serviceName) match {
      case None => NotFound
      case Some(service) => Ok(s"${service.toJson} exist.")
    }
  }

  def createService() = Action(parse.json) { request =>
    createServiceInner(request.body)
  }

  def createServiceInner(jsValue: JsValue) = {
    try {
      val (serviceName, cluster, tableName, preSplitSize, ttl) = toServiceElements(jsValue)
      val service = Management.createService(serviceName, cluster, tableName, preSplitSize, ttl)
      Ok(s"$service service created.\n")
    } catch {
      case e: Throwable =>
        Logger.error(s"$e", e)
        BadRequest(e.getMessage())
    }
  }

  def createLabel() = Action(parse.json) { request =>
    createLabelInner(request.body)
  }

  def createLabelInner(jsValue: JsValue) = {
    try {
      val (labelName, srcServiceName, srcColumnName, srcColumnType,
      tgtServiceName, tgtColumnName, tgtColumnType, isDirected,
      serviceName, idxProps, metaProps, consistencyLevel, hTableName, hTableTTL) = toLabelElements(jsValue)

      Management.createLabel(labelName, srcServiceName, srcColumnName, srcColumnType,
        tgtServiceName, tgtColumnName, tgtColumnType, isDirected, serviceName, idxProps, metaProps, consistencyLevel, hTableName, hTableTTL)
      Ok("Created\n")
    } catch {
      case e: Throwable =>
        Logger.error(s"$e", e)
        BadRequest(s"$e")
    }
  }

  def addIndex() = Action(parse.json) { request =>
    try {
      val (labelName, props) = toIndexElements(request.body)
      Management.addIndex(labelName, props)
      Ok("Created\n")
    } catch {
      case e: Throwable =>
        Logger.error(s"$e", e)
        BadRequest(s"$e")
    }
  }

  def getLabel(labelName: String) = Action { request =>
    Management.findLabel(labelName) match {
      case None => NotFound("NotFound\n")
      case Some(label) =>
        Ok(s"${label.toJson}\n")
    }
  }

  def getLabels(serviceName: String) = Action { request =>
    HService.findByName(serviceName) match {
      case None => BadRequest(s"create service first.")
      case Some(service) =>
        val srcs = HLabel.findBySrcServiceId(service.id.get)
        val tgts = HLabel.findByTgtServiceId(service.id.get)
        val json = Json.obj("from" -> srcs.map(src => src.toJson), "to" -> tgts.map(tgt => tgt.toJson))
        Ok(s"$json\n")
    }
  }

  def deleteLabel(labelName: String) = Action { request =>
    deleteLabelInner(labelName)
  }

  def deleteLabelInner(labelName: String) = {
    HLabel.findByName(labelName) match {
      case None => NotFound
      case Some(label) =>
        val json = label.toJson
        label.deleteAll
        Ok(s"${json} is deleted.\n")
    }
  }


  def addProp(labelName: String) = Action(parse.json) { request =>
    val (propName, defaultValue, dataType, usedInIndex) = toPropElements(request.body)
    try {
      val metaOpt = for (label <- HLabel.findByName(labelName)) yield {
        HLabelMeta.findOrInsert(label.id.get, propName, defaultValue.toString, dataType, usedInIndex)
      }
      val meta = metaOpt.getOrElse(throw new KGraphExceptions.LabelNotExistException(s"$labelName label does not exist."))
      val json = meta.toJson
      Created(s"$json\n")
    } catch {
      case e: Throwable =>
        Logger.error(s"$e", e)
        BadRequest(s"$e")
    }
  }

  /**
   * end of management
   */

  // get all labels belongs to this service.
  def labels(serviceName: String) = Action {
    Ok("not implemented yet.")
  }

//  def page = Action {
//    Ok(views.html.admin("S2Graph Admin Page"))
//  }
//
//  def manager = Action {
//    Ok(views.html.manager("S2Graph Manager Page"))
//  }

  //
  //  def swapLabel(oldLabelName: String, newLabelName: String) = Action {
  //    Label.findByName(oldLabelName) match {
  //      case None => NotFound
  //      case Some(oldLabel) =>
  //        val ret = Label.copyLabel(oldLabel, newLabelName)
  //        Ok(s"$ret\n")
  //    }
  //  }

  def allServices = Action {
    val svcs = HService.findAllServices
    Ok(Json.toJson(svcs.map(svc => svc.toJson))).as("application/json")
  }

  /**
   * never, ever exposes this to user.
   */
  //  def deleteEdges(zkAddr: String, tableName: String, labelIds: String, minTs: Long, maxTs: Long) = Action {
  //    val stats = Management.deleteEdgesByLabelIds(zkAddr, tableName, labelIds = labelIds, minTs = minTs, maxTs = maxTs, include = true)
  //    Ok(s"$stats\n")
  //  }
  //  def deleteAllEdges(zkAddr: String, tableName: String, minTs: Long, maxTs: Long) = Action {
  //    val labelIds = Label.findAllLabels(None, offset = 0, limit = Int.MaxValue).map(_.id.get).mkString(",")
  //    val stats = Management.deleteEdgesByLabelIds(zkAddr, tableName, labelIds = labelIds, minTs = minTs, maxTs = maxTs, include = false)
  //    Ok(s"$stats\n")
  //  }
}