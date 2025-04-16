package Springboot.piece;

import Springboot.engine.LosAlamosEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author stizr
 * @license MIT License
 *
 * Génére les coups des cavaliers
 */
public class Knight {

    /**
     * Génére les coups des cavaliers
     *
     * @param knights
     * @param ownPieces
     * @param opponentPieces
     * @return
     */
    /*public static List<String> generateKnightMoves(long knights, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();

        // Knight's L-shaped moves
        int[] knightMoves = {-17, -15, -10, -6, 6, 10, 15, 17};

        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);

            for (int move : knightMoves) {
                int to = from + move;

                if (to < 0 || to >= 64) continue;

                if ((move == -17 || move == -10 || move == 6 || move == 15) && ((1L << from) & 0x0101010101010101L) != 0) continue;
                if ((move == -15 || move == -6 || move == 10 || move == 17) && ((1L << from) & 0x8080808080808080L) != 0) continue;
                if ((move == -10 || move == 6) && ((1L << from) & 0x0303030303030303L) != 0) continue;
                if ((move == -6 || move == 10) && ((1L << from) & 0xC0C0C0C0C0C0C0C0L) != 0) continue;

                long bit = 1L << to;
                if ((ownPieces & bit) == 0) {
                    moves.add(translateMove(from, to));
                }
            }

            knights &= knights - 1;
        }
        return moves;
    }*/
    public static List<String> generateKnightMoves(long knights, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();
        //mouvement en L
        int[] knightMoves = {-17, -15, -10, -6, 6, 10, 15, 17};
        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            for (int move : knightMoves) {
                int to = from + move;
                if (to >= 0 && to < 64 && isValidMove(from, to)) {
                    long bit = 1L << to;
                    if ((ownPieces & bit) == 0) {
                        moves.add(translateMove(from, to));
                    }
                }
            }
            knights &= knights - 1;
        }
        return moves;
    }


    public static List<String> generateKnightMovesLosAlamos(long knights, long ownPieces, long opponentPieces) {
        List<String> moves = new ArrayList<>();

        // Knight's L-shaped moves
        int[] knightMoves = {-13, -11, -8, -4, 4, 8, 11, 13};

        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);

            for (int move : knightMoves) {
                int to = from + move;

                if (to < 0 || to >= 36) continue;

                if ((move == -13 || move == -8 || move == 4 || move == 11) && ((1L << from) & LosAlamosEngine.FILE_A) != 0) continue;
                if ((move == -11 || move == -4 || move == 8 || move == 13) && ((1L << from) & LosAlamosEngine.FILE_F) != 0) continue;
                if ((move == -8 || move == 4) && ((1L << from) & LosAlamosEngine.FILE_AB) != 0) continue;
                if ((move == -4 || move == 8) && ((1L << from) & LosAlamosEngine.FILE_EF) != 0) continue;

                long bit = 1L << to;
                if ((ownPieces & bit) == 0) {
                    moves.add(translateMoveLosAlamos(from, to));
                }
            }

            knights &= knights - 1;
        }
        return moves;
    }


    /**
     * Vérifie si on ne sort pas du tableau
     *
     * @param from -> position départ
     * @param to -> position arrivée
     * @return true ou false si on est en dehors des limites
     */
    private static boolean isValidMove(int from, int to) {
        int fromRank = from / 8, fromFile = from % 8;
        int toRank = to / 8, toFile = to % 8;
        return Math.abs(fromRank - toRank) == 2 && Math.abs(fromFile - toFile) == 1 ||
                Math.abs(fromRank - toRank) == 1 && Math.abs(fromFile - toFile) == 2;
    }

    private static String translateMove(int from, int to) {
        return "" + (char) ('a' + from % 8) + (from / 8 + 1) + (char) ('a' + to % 8) + (to / 8 + 1);
    }

    private static String translateMoveLosAlamos(int from, int to) {
        return "" + (char) ('a' + from % 6) + (from / 6 + 1) + (char) ('a' + to % 6) + (to / 6 + 1);
    }
}




