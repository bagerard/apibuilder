package controllers

import com.gilt.apidoc.FailedRequest
import com.gilt.apidoc.models.{Organization, Service, User, Watch, WatchForm}
import com.gilt.apidoc.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class WatchesSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  def createWatch(
    form: WatchForm = createWatchForm()
  ): Watch = {
    await(client.watches.post(form))
  }

  def createWatchForm(
    user: User = createUser(),
    org: Organization = createOrganization(),
    service: Option[Service] = None
  ) = WatchForm(
    userGuid = user.guid,
    organizationKey = org.key,
    serviceKey = service.getOrElse(createService(org)).key
  )

  "POST /watches" in new WithServer {
    val user = createUser()
    val service = createService(org)
    val watch = createWatch(
      WatchForm(
        organizationKey = org.key,
        userGuid = user.guid,
        serviceKey = service.key
      )
    )

    watch.user.guid must be(user.guid)
    watch.organization.key must be(org.key)
    watch.service.key must be(service.key)
  }

  "POST /watches is idempotent" in new WithServer {
    val user = createUser()
    val service = createService(org)

    val form = WatchForm(
      organizationKey = org.key,
      userGuid = user.guid,
      serviceKey = service.key
    )

    val watch = createWatch(form)
    createWatch(form)
  }

  "GET /watches by service key" in new WithServer {
    val org1 = createOrganization()
    val service1 = createService(org1)

    val org2 = createOrganization()
    val service2 = createService(org2, key = service1.key)
    val service3 = createService(org2)

    val user = createUser()

    createWatch(WatchForm(user.guid, org1.key, service1.key))
    createWatch(WatchForm(user.guid, org2.key, service2.key))
    createWatch(WatchForm(user.guid, org2.key, service3.key))

    await(client.watches.get(userGuid = Some(user.guid))).map(_.service.key).sorted must be(Seq(service1.key, service2.key, service3.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), serviceKey = Some(service1.key))).map(_.service.key).sorted must be(Seq(service1.key, service2.key).sorted)
    await(client.watches.get(userGuid = Some(user.guid), organizationKey = Some(org1.key), serviceKey = Some(service1.key))).map(_.service.key).sorted must be(Seq(service1.key).sorted)
  }

}
