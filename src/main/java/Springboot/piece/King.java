package Springboot.piece;

import Springboot.engine.Engine;
import Springboot.engine.LosAlamosEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stizr
 * @license MIT License
 *
 * Génére al liste des coups du roi
 */
public class King {

    /**
     * Génére la liste des coups du roi
     *
     * @param kings
     * @param ownPieces
     * @param opponentPieces
     * @return
     */
    public static List<String> generateKingMoves(long kings, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();

        //position possible du roi
        /**         7  8   9
         *           \ | /
         *      -1 <- pos -> 1
         *           / | \
         *        -9  -8  -7
         */

        int[] kingMoves = {8, 9, 1, -7, -8, -9, -1, 7};

        while (kings != 0) {
            int from = Long.numberOfTrailingZeros(kings);
            long fromBit = 1L << from;

            for (int move : kingMoves) {
                int to = from + move;
                if (to >= 0 && to < 64) {
                    long toBit = 1L << to;

                    if ((from % 8 == 0) && (move == -1 || move == -9 || move == 7)) continue; // Bord gauche (Colonne A)
                    if ((from % 8 == 7) && (move == 1 || move == 9 || move == -7)) continue;  // Bord droit (Colonne H)
                    if ((from / 8 == 0) && (move == -8 || move == -9 || move == -7)) continue; // Bord bas (Rangée 1)
                    if ((from / 8 == 7) && (move == 8 || move == 9 || move == 7)) continue; // Bord haut (Rangée 8)

                    if ((ownPieces & toBit) == 0) {
                        // je ne sais pas si je fais les magics bitboards
                        moves.add(translateMove(from, to));

                    }
                }
            }
            kings &= kings - 1;
        }
        return moves;
    }


    /**
     * Génére la liste possible des roques
     *
     * @param whiteKing -> roi blanc
     * @param whiteRooks -> tours blanches
     * @param blackKing -> roi noir
     * @param blackRooks -> tours noires
     * @param ownPieces -> pièces alliées
     * @param opponentPieces -> pièces ennemies
     * @param isWhite -> si le joueur est blanc
     * @param castlingRights -> permissions des roques (petit blanc, grand b, p n, l n)
     * @return
     */
    public static List<String> generateCastlingMoves(long whiteKing, long whiteRooks, long blackKing, long blackRooks, long ownPieces, long opponentPieces, boolean isWhite, boolean[] castlingRights, long enemyAttacks) {
        List<String> moves = new ArrayList<>();

        if (isWhite) {
            if (castlingRights[0] && (whiteKing & (1L << 4)) != 0 && (whiteRooks & (1L << 0)) != 0)  { // Grand roque
                if ((ownPieces & 0x000000000000000E) == 0 && (opponentPieces & 0x000000000000000E) == 0) {
                    if(!Engine.isCaseAttacked(4, enemyAttacks) && !Engine.isCaseAttacked(2, enemyAttacks) && !Engine.isCaseAttacked(3, enemyAttacks)) {
                        moves.add("e1c1");
                    }
                }
            }
            if (castlingRights[1] && (whiteKing & (1L << 4)) != 0 && (whiteRooks & (1L << 7)) != 0) { // Petit roque
                if ((ownPieces & 0x0000000000000060) == 0 && (opponentPieces & 0x0000000000000060) == 0) {
                    if(!Engine.isCaseAttacked(5, enemyAttacks) && !Engine.isCaseAttacked(6, enemyAttacks) && !Engine.isCaseAttacked(4, enemyAttacks)) {
                        moves.add("e1g1");
                    }
                }
            }
        } else {
            if (castlingRights[2] && (blackKing & (1L << 60)) != 0 && (blackRooks & (1L << 56)) != 0) { // Grand roque
                if ((ownPieces & 0x0E00000000000000L) == 0 && (opponentPieces & 0x0E00000000000000L) == 0) {
                    if(!Engine.isCaseAttacked(58, enemyAttacks) && !Engine.isCaseAttacked(59, enemyAttacks) && !Engine.isCaseAttacked(60, enemyAttacks)) {
                        moves.add("e8c8");
                    }
                }
            }
            if (castlingRights[3] && (blackKing & (1L << 60)) != 0 && (blackRooks & (1L << 63)) != 0) { // Petit roque
                if ((ownPieces & 0x6000000000000000L) == 0 && (opponentPieces & 0x6000000000000000L) == 0) {
                    if(!Engine.isCaseAttacked(60, enemyAttacks) && !Engine.isCaseAttacked(61, enemyAttacks) && !Engine.isCaseAttacked(62, enemyAttacks)) {
                        moves.add("e8g8");
                    }
                }
            }
        }
        return moves;
    }

    /**
     * Génére la liste des coups du roi
     *
     * @param kings
     * @param ownPieces
     * @param opponentPieces
     * @return
     */
    public static List<String> generateKingMovesLosAlamos(long kings, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();


        int[] kingMoves = {6, 7, 1, -5, -6, -7, -1, 5};

        while (kings != 0) {
            int from = Long.numberOfTrailingZeros(kings);
            long fromBit = 1L << from;

            for (int move : kingMoves) {
                int to = from + move;
                if (to >= 0 && to < 36) {
                    long toBit = 1L << to;

                    if ((fromBit & LosAlamosEngine.FILE_A) != 0 && (move == 7 || move == 1 || move == -5)) continue; // Bloque `>>>9`, `>>>1`, `<<7` (Colonne A)
                    if ((fromBit & LosAlamosEngine.FILE_F) != 0 && (move == 5 || move == -1 || move == -7)) continue; // Bloque `>>>7`, `<<1`, `<<9` (Colonne H)
                    if ((fromBit & LosAlamosEngine.FILE_6) != 0 && (move == 7 || move == 6 || move == 5)) continue; // Bloque `<<9`, `<<8`, `<<7` (Ligne 8)
                    if ((fromBit & LosAlamosEngine.FILE_1) != 0 && (move == -7 || move == -6 || move == -5)) continue; // Bloque `>>>9`, `>>>8`, `>>>7` (Ligne 1)

                    if ((ownPieces & toBit) == 0) {
                        // je ne sais pas si je fais les magics bitboards
                        if (true) {
                            moves.add(translateMoveLosAlamos(from, to));
                        }
                    }
                }
            }
            kings &= kings - 1;
        }
        return moves;
    }


// 2 3 4 5 |  5 6 7
    //62 61 60 | 60 59 58 57


    private static String translateMove(int from, int to) {
        return "" + (char) ('a' + from % 8) + (from / 8 + 1) + (char) ('a' + to % 8) + (to / 8 + 1);
    }

    private static String translateMoveLosAlamos(int from, int to) {
        return "" + (char) ('a' + from % 6) + (from / 6 + 1) + (char) ('a' + to % 6) + (to / 6 + 1);
    }
}


