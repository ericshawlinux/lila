package lila.forum

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*
import lila.common.Iso

private object BSONHandlers:

  given BSONDocumentHandler[ForumCateg] = Macros.handler

  given BSONDocumentHandler[OldVersion] = Macros.handler

  given BSONHandler[ForumPost.Reaction] = quickHandler[ForumPost.Reaction](
    { case BSONString(key) => ForumPost.Reaction(key) err s"Unknown reaction $key" },
    reaction => BSONString(reaction.key)
  )

  private given Iso.StringIso[ForumPost.Reaction] =
    Iso.string(key => ForumPost.Reaction(key) err s"Unknown reaction $key", _.key)

  private given BSONHandler[ForumPost.Reactions] = typedMapHandlerIso[ForumPost.Reaction, Set[UserId]]

  given BSONDocumentHandler[ForumPost] = Macros.handler

  given BSONDocumentHandler[ForumTopic] = Macros.handler
