package Springboot.piece;

import Springboot.engine.LosAlamosEngine;

import java.util.ArrayList;
import java.util.List;


/**
 * @author stizr
 * @license MIT License
 *
 * Classe qui permet de générer la liste des coups pour les fous
 */
public class Bishop {

    /**
     * Génére la liste des coups des fous
     * <p>
     * On lance un "ray" en 4 directions afin de voire si cela touche une pièce ou non
     *
     * @param bishops        -> liste des bishops alliés
     * @param ownPieces      -> liste des pièces alliées
     * @param opponentPieces -> liste des pièces ennemies
     * @return la liste des coups possibles pour les bishops
     */
    /*public static List<String> generateBishopMoves(long bishops, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();
        int[] directions = {9, -9, 7, -7};

        for (int i = 0; i < 64; i++) {
            if ((bishops & (1L << i)) != 0) {
                for (int dir : directions) {
                    int currentPos = i;
                    while (true) {
                        int prevPos = currentPos;
                        currentPos += dir;

                        if (currentPos < 0 || currentPos >= 64) break;

                        if (((dir == 9 || dir == -7) && ((1L << prevPos) & 0x8080808080808080L) != 0) ||
                         ((dir == -9 || dir == 7) && ((1L << prevPos) & 0x0101010101010101L) != 0)) break;

                        long bit = 1L << currentPos;

                        if ((ownPieces & bit) != 0) break;

                        moves.add(translateMove(i, currentPos));

                        if ((opponentPieces & bit) != 0) break;
                    }
                }
            }
        }
        return moves;
    }*/
    public static List<String> generateBishopMoves(long bishops, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();
        int[] directions = {9, -9, 7, -7};
        for (int i = 0; i < 64; i++) {
            if ((bishops & (1L << i)) != 0) {
                for (int dir : directions) {
                    int currentPos = i;
                    while (true) {
                        currentPos += dir;
                        if (currentPos < 0 || currentPos >= 64) break;
                        if (Math.abs(currentPos % 8 - (currentPos - dir) % 8) != 1) break;
                        long bit = 1L << currentPos;
                        if ((ownPieces & bit) != 0) break;
                        moves.add(translateMove(i, currentPos));
                        if ((opponentPieces & bit) != 0) break;
                    }
                }
            }
        }

        return moves;
    }

//    public static List<String> generateBishopMoves(long bishops, long occupancy, long ownPieces) {
//        List<String> moves = new ArrayList<>();
//
//        while (bishops != 0) {
//            // Récupérer la position du fou le moins significatif
//            int square = Long.numberOfTrailingZeros(bishops);
//            bishops &= bishops - 1; // Retirer ce fou du bitboard
//
//            // Calculer les attaques en utilisant les magic bitboards
//            long attacks = Engine.calculateBishopAttacks(square, occupancy);
//
//            // Exclure les cases occupées par les pièces alliées
//            attacks &= ~ownPieces;
//
//            // Convertir les attaques en coups
//            while (attacks != 0) {
//                int targetSquare = Long.numberOfTrailingZeros(attacks);
//                attacks &= attacks - 1; // Retirer cette attaque du bitboard
//
//                // Traduire en notation standard (ex : e2c4)
//                moves.add(translateMove(square, targetSquare));
//            }
//        }
//
//        return moves;
//    }



    private static String translateMove(int from, int to) {
        return "" + (char) ('a' + from % 8) + (from / 8 + 1) +
                (char) ('a' + to % 8) + (to / 8 + 1);
    }


    public static List<String> generateBishopMovesLosAlamos(long bishops, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();
        int[] directions = {7, -7, 5, -5};

        for (int i = 0; i < 36; i++) {
            if ((bishops & (1L << i)) != 0) {
                for (int dir : directions) {
                    int currentPos = i;
                    while (true) {
                        int prevPos = currentPos;
                        currentPos += dir;

                        if (currentPos < 0 || currentPos >= 36) break;

                        if ((dir == 7 || dir == -5) && ((1L << prevPos) & LosAlamosEngine.FILE_F) != 0) break;
                        if ((dir == -7 || dir == 5) && ((1L << prevPos) & LosAlamosEngine.FILE_A) != 0) break;

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
        return "" + (char) ('a' + from % 6) + (from / 6 + 1) +
                (char) ('a' + to % 6) + (to / 6 + 1);
    }
}


//package Springboot.piece;
//
//import Springboot.engine.Engine;
//import Springboot.engine.MagicBitboards;
//import Springboot.engine.MagicBitboards.BishopKey;
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
//public class Bishop {
//    public static Map<BishopKey, Long> rookTable = null;
//
//    public static void fileBishopTable(Engine engine) {
//        rookTable = new MagicBitboards().createBishopTable(engine.ColorToMove == 1 ? engine.whitePieces() : engine.blackPieces());
//    }
//
//    /**
//     * Génère la liste des coups légaux du fou en utilisant Magic Bitboards.
//     *
//     * @param bishops        -> Bitboard des fous alliées
//     * @param ownPieces      -> Bitboard des pièces alliées
//     * @param opponentPieces -> Bitboard des pièces ennemies
//     * @return Liste des coups possibles de la tour
//     */
//    public static List<String> generateBishopMoves(long bishops, long ownPieces, long opponentPieces) {
//        List<String> moves = new ArrayList<>();
//
//        for (int i = 0; i < 64; i++) {
//            if ((bishops & (1L << i)) != 0) {
//                long allyBlockers = ownPieces;
//                long enemyBlockers = opponentPieces;
//
//                long moveMask = getRookMovesFromTable(i, allyBlockers, enemyBlockers);
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
//     * @param allyBlockers -> Bitboard des pièces qui bloquent les mouvements
//     * @param enemyBlockers -> Bitboard des pièces qui bloquent les mouvements
//     * @return Bitboard des mouvements légaux de la tour
//     */
//    private static long getRookMovesFromTable(int squareIndex, long allyBlockers, long enemyBlockers) {
//        BishopKey key = new BishopKey(squareIndex, allyBlockers, enemyBlockers);
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
//import Springboot.engine.MagicBitboards.BishopKey;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author stizr
// * @license MIT License
// *
// * Génère la liste des coups du fou en utilisant Magic Bitboards.
// */
//public class Bishop {
//    public static Map<BishopKey, Long> bishopTable = null;
//
//    /**
//     * Remplir la table des mouvements du fou en utilisant les pièces alliées et ennemies.
//     */
//    public static void fileBishopTable(Engine engine, boolean isWhitePlayer) {
//        long friendlyPieces = isWhitePlayer ? engine.whitePieces() : engine.blackPieces();
//        long enemyPieces = isWhitePlayer ? engine.blackPieces() : engine.whitePieces();
//        bishopTable = new MagicBitboards().createBishopTable(friendlyPieces, enemyPieces);  // Pass the bitboards
//    }
//
//    /**
//     * Génère la liste des coups légaux du fou en utilisant Magic Bitboards.
//     *
//     * @param bishops        -> Bitboard des fous alliées
//     * @param ownPieces      -> Bitboard des pièces alliées
//     * @param opponentPieces -> Bitboard des pièces ennemies
//     * @return Liste des coups possibles du fou
//     */
//    public static List<String> generateBishopMoves(long bishops, long ownPieces, long opponentPieces) {
//        List<String> moves = new ArrayList<>();
//
//        // Loop through each square (0-63) to check if a bishop is present
//        for (int i = 0; i < 64; i++) {
//            if ((bishops & (1L << i)) != 0) {
//                long allyBlockers = ownPieces; // Blockers are friendly pieces
//                long enemyBlockers = opponentPieces; // Blockers are enemy pieces
//
//                // Get legal moves from precomputed table
//                long moveMask = getBishopMovesFromTable(i, enemyBlockers);
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
//     * Récupère les mouvements du fou pré-calculés à partir de Magic Bitboards.
//     *
//     * @param squareIndex    -> Position de départ du fou (0-63)
//     * @param enemyBlockers  -> Bitboard des pièces ennemies qui bloquent les mouvements
//     * @return Bitboard des mouvements légaux du fou
//     */
//    private static long getBishopMovesFromTable(int squareIndex, long enemyBlockers) {
//        BishopKey key = new BishopKey(squareIndex, enemyBlockers);  // Create a key with the square and blockers
//        return bishopTable.getOrDefault(key, 0L);  // Get the precomputed legal moves or return 0 if not found
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




