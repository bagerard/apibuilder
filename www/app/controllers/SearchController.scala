package controllers

import com.gilt.apidoc.api.v0.models.ItemType
import lib.{Pagination, PaginatedCollection}
import play.api._
import play.api.mvc._
import play.api.Logger
import java.util.UUID
import scala.concurrent.Future

object SearchController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(orgKey: String, q: Option[String], page: Int = 0) = AnonymousOrg.async { implicit request =>
    for {
      items <- request.api.items.get(
        orgKey = Some(orgKey),
        q = q,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.search.index(
        request.mainTemplate().copy(
          query = q
        ),
        q = q,
        items = PaginatedCollection(page, items)
      ))
    }
  }
  
  def redirect(orgKey: String, itemType: ItemType, guid: UUID) = AnonymousOrg.async { implicit request =>
    itemType match {

      case ItemType.Application => {
        request.api.applications.getByOrgKey(orgKey, guid = Some(guid), limit = 1).map { apps =>
          apps.headOption match {
            case None => Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> "Application not found")
            case Some(app) => Redirect(routes.Versions.show(orgKey, app.key, "latest"))
          }
        }
      }

      case ItemType.UNDEFINED(desc) => {
        Logger.warn(s"Undefined search item type[$desc] w/ guid[$guid]")
        Future {
          Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> "Unrecognized search result")
        }
      }
    }
  }

}
