package lila.importer

import lila.game.{ Game, GameRepo }

final class Importer(gameRepo: GameRepo)(using Executor):

  def apply(data: ImportData, user: Option[UserId], forceId: Option[GameId] = None): Fu[Game] =

    def gameExists(processing: => Fu[Game]): Fu[Game] =
      gameRepo.findPgnImport(data.pgn) flatMap { _.fold(processing)(fuccess) }

    gameExists {
      (data preprocess user).toFuture flatMap { case Preprocessed(g, _, initialFen, _) =>
        val game = forceId.fold(g.sloppy)(g.withId)
        gameRepo.insertDenormalized(game, initialFen = initialFen) >> {
          game.pgnImport.flatMap(_.user).isDefined ?? gameRepo.setImportCreatedAt(game)
        } >> {
          gameRepo.finish(
            id = game.id,
            winnerColor = game.winnerColor,
            winnerId = None,
            status = game.status
          )
        } inject game
      }
    }
