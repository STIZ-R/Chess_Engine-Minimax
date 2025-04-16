package Springboot.piece;

import java.util.List;

/**
 * @author stizr
 * @license MIT License
 *
 * Génére la liste des coups de la reine
 */
public class Queen {

    /**
     * Génére al liste des coups de la reine en utilisant les méthodes du fou et de la tour
     *
     * @param queens -> reines alliées
     * @param ownPieces -> pièces alliées
     * @param opponentPieces -> pièces ennemies
     * @return la liste des coups de la reine
     */
    public static List<String> generateQueenMoves(long queens, long ownPieces, long opponentPieces) {
        List<String> rookMoves = Rook.generateRookMoves(queens, ownPieces, opponentPieces);
        List<String> bishopMoves = Bishop.generateBishopMoves(queens, ownPieces, opponentPieces);
        rookMoves.addAll(bishopMoves);
        return rookMoves;
    }

    public static List<String> generateQueenMovesLosAlamos(long queens, long ownPieces, long opponentPieces) {
        List<String> rookMoves = Rook.generateRookMovesLosAlamos(queens, ownPieces, opponentPieces);
        List<String> bishopMoves = Bishop.generateBishopMovesLosAlamos(queens, ownPieces, opponentPieces);
        rookMoves.addAll(bishopMoves);
        return rookMoves;
    }
}

