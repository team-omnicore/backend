package sc.plugin2019

import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import sc.api.plugins.TwoPlayerGameState
import sc.framework.plugins.Player
import sc.plugin2019.util.Constants
import sc.plugin2019.util.GameRuleLogic
import sc.shared.InvalidGameStateException
import sc.shared.PlayerColor

class GameState(
        override var red: Player = Player(PlayerColor.RED),
        override var blue: Player = Player(PlayerColor.BLUE),
        override var board: Board = Board()) : TwoPlayerGameState<Player, Move>(), Cloneable {

    constructor(state: GameState) : this(state.red.clone(), state.blue.clone(), state.board.clone()) {
        lastMove = state.lastMove?.clone()
        turn = state.turn
        currentPlayerColor = state.currentPlayerColor
        startPlayerColor = state.startPlayerColor
    }

    public override fun clone() = GameState(this)

    @XStreamAsAttribute
    override var turn = 0
        set(value) {
            val turnLimit = Constants.ROUND_LIMIT * 2
            if(value > turnLimit) throw InvalidGameStateException("Turn $value exceeds turnLimit($turnLimit)")
            field = value
        }

    fun getField(x: Int, y: Int) = board.getField(x, y)

    /** wechselt den Spieler, der aktuell an der Reihe ist, anhand der Zugzahl [turn]  */
    fun switchCurrentPlayer() {
        currentPlayerColor = if(turn % 2 == 0) PlayerColor.RED else PlayerColor.BLUE
    }

    override fun getPointsForPlayer(playerColor: PlayerColor) = GameRuleLogic.greatestSwarmSize(board, playerColor)

    fun getPlayerStats(player: Player): IntArray =
            getPlayerStats(player.color)

    /**
     * Liefert Statusinformationen zu einem Spieler als Array mit folgenden
     * Einträgen:
     *
     *  * `[0]` - Punktekonto des Spielers (Größe des Schwarms)
     *
     */
    fun getPlayerStats(playerColor: PlayerColor): IntArray =
            intArrayOf(this.getPointsForPlayer(playerColor))

    /**
     * Liefert Statusinformationen zum Spiel. Diese sind ein Array der
     * [getPlayerStats], wobei `getGameStats()[0]`,
     * einem Aufruf von `getPlayerStats(PlayerColor.RED)` entspricht.
     *
     * @return Statusinformationen beider Spieler
     *
     * @see [getPlayerStats]
     */
    fun getGameStats(): Array<IntArray> {
        return Array(2) { index -> getPlayerStats(PlayerColor.values().first { it.index == index }) }
    }

    /**
     * Fuegt einem Spiel einen weiteren Spieler hinzu.
     *
     * Diese Methode ist nur fuer den Spielserver relevant und sollte vom
     * Spielclient i.A. nicht aufgerufen werden!
     */
    fun addPlayer(player: Player) {
        when(player.color) {
            PlayerColor.RED -> red = player
            PlayerColor.BLUE -> blue = player
        }
    }

}