/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{Cursor, ReadConcern}
import reactivemongo.bson.{BSONDocument, BSONString}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class MongoDiagnosticsController @Inject()(component: ReactiveMongoComponent,
                                           cc: ControllerComponents)(implicit ec: ExecutionContext) extends BackendController(cc) {

  // scalastyle:off magic.number
  private val banner = Seq.fill(50)("-").mkString
  // scalastyle:on magic.number

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")

  def mongoDiagnostics(): Action[AnyContent] = Action.async {
      fetchDiagnostics() map {
        diagnostics =>
          Ok(diagnostics.mkString("\n"))
      }

  }

  def fetchDiagnostics(): Future[Seq[String]] = {

    val db = component.mongoConnector.db()

    db.collectionNames flatMap  {
      names =>
        Future.traverse(names) {
          name =>
            collectionDiagnostics(db.collection(name)) map {
              diagnostics =>
                (Seq(
                  banner,
                  name,
                  banner
                ) ++ diagnostics).mkString("\n").concat("\n")
            }
        }
    }

  }

  def collectionDiagnostics(collection: BSONCollection): Future[Seq[String]] = {

    for {
      rows <- rowCount(collection)
      indexes <- indexInfo(collection)
      minLastUpdated <- minLastUpdated(collection)
      externalIds <- ids(collection)
    } yield {
      Seq(
        s"Rows: $rows",
        s"Min Last Updated: $minLastUpdated",
        "",
        "Indexes...",
        indexes,
        "",
        "Ids (External Id, Session Id, etc)...",
        externalIds
      )
    }

  }

  def indexInfo(collection: BSONCollection): Future[String] = {

    collection.indexesManager.list() map {
      indexes =>
        indexes.map {
          index =>
            val ttl = index.options.getAs[Int]("expireAfterSeconds").getOrElse("<none>")
            s"Name: ${index.name.getOrElse("?")}; Fields: (${index.key.map(_._1).mkString(", ")}); Unique: ${index.unique}; TTL: $ttl"
        } mkString "\n"
    }

  }

  def minLastUpdated(collection: BSONCollection): Future[String] = {

    import collection.BatchCommands.AggregationFramework.{Group, MinField}

    collection.aggregatorContext[BSONDocument](
      Group(BSONString(""))("minLastUpdated" -> MinField("lastUpdated"))
    ).prepared.cursor.collect[Seq](
      -1,
      Cursor.FailOnError[Seq[BSONDocument]]()
    ).map(docs => {
      docs.head.getAs[Date]("minLastUpdated").map {
        date => dateFormat.format(date)
      }.getOrElse("<none>")
    })
  }

  def ids(collection: BSONCollection): Future[String] = {

    val query = BSONDocument()
    val projection = Some(BSONDocument("_id" -> 0, "id" -> 1))

    collection.find(query, projection)
      .cursor[BSONDocument]()
      .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
      .map {
        docs =>
          docs.map {
            doc =>
              doc.getAs[String]("id").getOrElse("<unknown>")
          } mkString "\n"
      }
  }

  def rowCount(collection: BSONCollection): Future[Long] = collection.count(None, None, skip = 0, None, ReadConcern.Local)

}
