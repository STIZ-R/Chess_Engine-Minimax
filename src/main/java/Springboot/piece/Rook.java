package Springboot.piece;

import Springboot.engine.LosAlamosEngine;

import java.util.ArrayList;
import java.util.List;


/**
 * @author stizr
 * @license MIT License
 *
 * Génére la liste des coups de la tour
 */
public class Rook {

    /**
     * Génére la liste des coups de la tour
     *
     * @param rooks -> tour alliée
     * @param ownPieces -> pièces alliées
     * //@param opponentPieces -> pièces ennemies
     * @return la liste des coups de la tour
     */
//    public static List<String> generateRookMoves(long rooks, long ownPieces, long opponentPieces) {
//        long movesBitboard = Engine.computeRookMoveSets(rooks, opponentPieces, ~(ownPieces | opponentPieces));
//        List<String> moves = new ArrayList<>();
//        while (movesBitboard != 0) {
//            int to = Long.numberOfTrailingZeros(movesBitboard);
//            movesBitboard &= movesBitboard - 1;
//            long fromBits = rooks;
//            while (fromBits != 0) {
//                int from = Long.numberOfTrailingZeros(fromBits);
//                fromBits &= fromBits - 1;
//                if ((Engine.getLineSegment(from, to) & movesBitboard) != 0) {
//                    moves.add(translateMove(from, to));
//                }
//            }
//        }
//        return moves;
//    }
    public static List<String> generateRookMoves(long rooks, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();

        int[] directions = {8, -8, 1, -1};

        for (int i = 0; i < 64; i++) {
            if ((rooks & (1L << i)) != 0) {
                for (int dir : directions) {
                    int currentPos = i;
                    while (true) {
                        currentPos += dir;

                        if (currentPos < 0 || currentPos >= 64) break;
                        if ((dir == 1 || dir == -1) && (currentPos / 8 != (currentPos - dir) / 8)) break;
                        if ((ownPieces & (1L << currentPos)) != 0) break;

                        moves.add(translateMove(i, currentPos));

                        if ((opponentPieces & (1L << currentPos)) != 0) break;
                    }
                }
            }
        }

        return moves;
    }


//    public static List<String> generateRookMoves(long rooks, long occupancy, long ownPieces) {
//        List<String> moves = new ArrayList<>();
//
//        while (rooks != 0) {
//            int square = Long.numberOfTrailingZeros(rooks);
//            rooks &= rooks - 1;
//            long attacks = Engine.calculateRookAttacks(square, occupancy);
//
//            attacks &= ~ownPieces;
//
//            while (attacks != 0) {
//                int targetSquare = Long.numberOfTrailingZeros(attacks);
//                attacks &= attacks - 1;
//
//                moves.add(translateMove(square, targetSquare));
//            }
//        }
//
//        return moves;
//    }



    private static String translateMove(int from, int to) {
        return "" + (char) ('a' + from % 8) + (from / 8 + 1) + (char) ('a' + to % 8) + (to / 8 + 1);
    }

    public static List<String> generateRookMovesLosAlamos(long rooks, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();
        int[] directions = {6, -6, 1, -1}; // Up, Down, Right, Left

        for (int i = 0; i < 36; i++) {
            if ((rooks & (1L << i)) != 0) {
                for (int dir : directions) {
                    int currentPos = i;
                    while (true) {
                        int prevPos = currentPos;
                        currentPos += dir;

                        if (currentPos < 0 || currentPos >= 36) break;

                        if ((dir == 1 && ((1L << prevPos) & LosAlamosEngine.FILE_F) != 0) ||
                                (dir == -1 && ((1L << prevPos) & LosAlamosEngine.FILE_A) != 0)) {
                            break;
                        }

                        long bit = 1L << currentPos;

                        if ((ownPieces & bit) != 0) break;

                        moves.add(translateMoveLosAlamos(i, currentPos));

                        if ((opponentPieces & bit) != 0) break;
                    }
                }
            }
        }
        return moves;
    }

    private static String translateMoveLosAlamos(int from, int to) {
        return "" + (char) ('a' + from % 6) + (from / 6 + 1) + (char) ('a' + to % 6) + (to / 6 + 1);
    }
}

//package Springboot.piece;
//
//import Springboot.engine.Engine;
//import Springboot.engine.MagicBitboards;
//import Springboot.engine.MagicBitboards.RookKey;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author stizr
// * @license MIT License
// *
// * Génère la liste des coups de la tour en utilisant Magic Bitboards.
// */
//public class Rook {
//    public static Map<RookKey, Long> rookTable = null;
//
//    public static void fileRookTable(Engine engine) {
//        rookTable = new MagicBitboards().createRookTable();
//    }
//
//    /**
//     * Génère la liste des coups légaux de la tour en utilisant Magic Bitboards.
//     *
//     * @param rooks          -> Bitboard des tours alliées
//     * @param opponentPieces -> Bitboard des pièces ennemies
//     * @return Liste des coups possibles de la tour
//     */
//    public static List<String> generateRookMoves(long rooks, long opponentPieces) {
//        List<String> moves = new ArrayList<>();
//
//        for (int i = 0; i < 64; i++) {
//            if ((rooks & (1L << i)) != 0) {
//                long enemyBlockers = opponentPieces;
//
//                long moveMask = getRookMovesFromTable(i, enemyBlockers);
//
//                while (moveMask != 0) {
//                    int targetSquare = Long.numberOfTrailingZeros(moveMask);
//                    moveMask &= moveMask - 1;
//
//                    moves.add(translateMove(i, targetSquare));
//                }
//            }
//        }
//        return moves;
//    }
//
//    /**
//     * Récupère les mouvements de la tour pré-calculés à partir de Magic Bitboards.
//     *
//     * @param squareIndex  -> Position de départ de la tour (0-63)
//     * @param enemyBlockers -> Bitboard des pièces qui bloquent les mouvements
//     * @return Bitboard des mouvements légaux de la tour
//     */
//    private static long getRookMovesFromTable(int squareIndex, long enemyBlockers) {
//        RookKey key = new RookKey(squareIndex, enemyBlockers);
//        return rookTable.getOrDefault(key, 0L);
//    }
//
//    /**
//     * Traduit un mouvement en notation échiquéenne (ex: e2e4).
//     */
//    private static String translateMove(int from, int to) {
//        return "" + (char) ('a' + from % 8) + (from / 8 + 1) +
//                (char) ('a' + to % 8) + (to / 8 + 1);
//    }
//}

//package Springboot.piece;
//
//import Springboot.engine.Engine;
//import Springboot.engine.MagicBitboards;
//import Springboot.engine.MagicBitboards.RookKey;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author stizr
// * @license MIT License
// *
// * Génère la liste des coups de la tour en utilisant Magic Bitboards.
// */
//public class Rook {
//    public static Map<RookKey, Long> rookTable = null;
//
//    /**
//     * Remplir la table des mouvements de la tour en utilisant les pièces alliées et ennemies.
//     */
//    public static void fileRookTable(Engine engine, boolean isWhitePlayer) {
//        long friendlyPieces = isWhitePlayer ? engine.whitePieces() : engine.blackPieces();
//        long enemyPieces = isWhitePlayer ? engine.blackPieces() : engine.whitePieces();
//        rookTable = new MagicBitboards().createRookTable(friendlyPieces, enemyPieces);  // Pass the bitboards
//    }
//
//    /**
//     * Génère la liste des coups légaux de la tour en utilisant Magic Bitboards.
//     *
//     * @param rooks          -> Bitboard des tours alliées
//     * @param opponentPieces -> Bitboard des pièces ennemies
//     * @return Liste des coups possibles de la tour
//     */
//    public static List<String> generateRookMoves(long rooks, long opponentPieces) {
//        List<String> moves = new ArrayList<>();
//
//        // Loop through each square (0-63) to check if a rook is present
//        for (int i = 0; i < 64; i++) {
//            if ((rooks & (1L << i)) != 0) {
//                long enemyBlockers = opponentPieces; // Blockers are enemy pieces
//
//                // Get legal moves from precomputed table
//                long moveMask = getRookMovesFromTable(i, enemyBlockers);
//
//                while (moveMask != 0) {
//                    int targetSquare = Long.numberOfTrailingZeros(moveMask); // Get the target square
//                    moveMask &= moveMask - 1;  // Clear the lowest bit
//
//                    moves.add(translateMove(i, targetSquare));  // Translate the move to algebraic notation
//                }
//            }
//        }
//        return moves;
//    }
//
//    /**
//     * Récupère les mouvements de la tour pré-calculés à partir de Magic Bitboards.
//     *
//     * @param squareIndex  -> Position de départ de la tour (0-63)
//     * @param enemyBlockers -> Bitboard des pièces qui bloquent les mouvements
//     * @return Bitboard des mouvements légaux de la tour
//     */
//    private static long getRookMovesFromTable(int squareIndex, long enemyBlockers) {
//        RookKey key = new RookKey(squareIndex, enemyBlockers);  // Create a key with the square and blockers
//        return rookTable.getOrDefault(key, 0L);  // Get the precomputed legal moves or return 0 if not found
//    }
//
//    /**
//     * Traduit un mouvement en notation échiquéenne (ex: e2e4).
//     */
//    private static String translateMove(int from, int to) {
//        return "" + (char) ('a' + from % 8) + (from / 8 + 1) +
//                (char) ('a' + to % 8) + (to / 8 + 1);  // Convert from/to indices to algebraic notation
//    }
//}
