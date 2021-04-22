package lila.relay

import akka.actor._
import com.softwaremill.macwire._
import play.api.libs.ws.StandaloneWSClient
import scala.concurrent.duration._

import lila.common.config._

final class Env(
    ws: StandaloneWSClient,
    db: lila.db.Db,
    studyApi: lila.study.StudyApi,
    chapterRepo: lila.study.ChapterRepo,
    gameRepo: lila.game.GameRepo,
    pgnDump: lila.game.PgnDump,
    gameProxy: lila.round.GameProxyRepo,
    cacheApi: lila.memo.CacheApi,
    slackApi: lila.irc.SlackApi,
    baseUrl: BaseUrl
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  lazy val forms = wire[RelayForm]

  private lazy val relayRepo = new RelayRepo(db(CollName("relay")))

  private lazy val tourRepo = new RelayTourRepo(db(CollName("relay_tour")))

  private lazy val withStudy = wire[RelayWithStudy]

  lazy val jsonView = wire[JsonView]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val pager = wire[RelayPager]

  lazy val push = wire[RelayPush]

  private lazy val sync = wire[RelaySync]

  private lazy val formatApi = wire[RelayFormatApi]

  system.actorOf(Props(wire[RelayFetch]))

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    api.autoStart >> api.autoFinishNotSyncing
    ()
  }

  lila.common.Bus.subscribeFun("study", "relayToggle") {
    case lila.hub.actorApi.study.RemoveStudy(studyId, _) => api.onStudyRemove(studyId).unit
    case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi.isContributor(id, who.u) foreach {
        _ ?? {
          api.requestPlay(Relay.Id(id.value), v)
        }
      }
  }
}
