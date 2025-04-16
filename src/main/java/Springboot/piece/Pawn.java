package Springboot.piece;

import Springboot.engine.LosAlamosEngine;

import java.util.ArrayList;
import java.util.List;


/**
 * @author stizr
 * @license MIT License
 *
 * Classe permettant de générer tout les coups possibles pour les pions
 */
public class Pawn {


    /**
     * Génère la liste des mouvement des pions
     *
     * @param pawns          -> bitboard des pions qui jouent
     * @param ownPieces      -> bitboard des pièces alliées
     * @param opponentPieces -> bitboard des pièces ennemies
     * @param isWhite        -> si le joueur est blanc
     * @return liste des mouvement du pion (hors promo et e.p)
     */
    public static List<String> generatePawnMoves(long pawns, long ownPieces, long opponentPieces, boolean isWhite) {
        List<String> moves = new ArrayList<>();

        long singleStep, doubleStep, capturesLeft, capturesRight;
        if (isWhite) {

            /**
             * 0x00FF000000000000L signifie tout sauf la ligne 7 -> cas à part pour les promotions
             * 0x0000000000FF0000L -> ligne 3
             * 0x0101010101010101L -> la colonne A
             * 0x8080808080808080L -> la colonne H
             *
             * Soit quand on fait par exemple pawns & ~ ligne 7, on vérifie qu'aucun pion ne soit sur la ligne 7
             * Ensuite on décale de 8 vers la droite (soit devant le pion) et on regarde qu'il n'y a aucune pièce
             * alliées ou ennemies.
             *
             */

            // Déplacements du pion blanc
            singleStep = ((pawns & ~0x00FF000000000000L) << 8) & ~ownPieces & ~opponentPieces; // Déplacement simple
            doubleStep = ((singleStep & 0x0000000000FF0000L) << 8) & ~ownPieces & ~opponentPieces; // Déplacement double
            capturesLeft = ((pawns & ~0x0101010101010101L & ~0x00FF000000000000L) << 7) & opponentPieces & ~ownPieces; // Capture à gauche
            capturesRight = ((pawns & ~0x8080808080808080L & ~0x00FF000000000000L) << 9) & opponentPieces & ~ownPieces; // Capture à droite
            addPawnMoves(moves, singleStep, 8, isWhite);
            addPawnMoves(moves, doubleStep, 16, isWhite);
            addPawnMoves(moves, capturesLeft, 7, isWhite);
            addPawnMoves(moves, capturesRight, 9, isWhite);
        } else {

            // Déplacements du pion noir
            singleStep = ((pawns & ~0x000000000000FF00L) >>> 8) & ~ownPieces & ~opponentPieces; // Déplacement simple
            doubleStep = ((singleStep & 0x0000FF0000000000L) >>> 8) & ~ownPieces & ~opponentPieces; // Déplacement double
            capturesLeft = ((pawns & ~0x000000000000FF00L) >>> 7) & opponentPieces & ~0x0101010101010101L; // Capture à gauche
            capturesRight = ((pawns & ~0x000000000000FF00L) >>> 9) & opponentPieces & ~0x8080808080808080L; // Capture à droite
            addPawnMoves(moves, singleStep, 8, isWhite);
            addPawnMoves(moves, doubleStep, 16, isWhite);
            addPawnMoves(moves, capturesLeft, 7, isWhite);
            addPawnMoves(moves, capturesRight, 9, isWhite);
        }


        return moves;
    }

    /**
     * Génération des mouvements En Passant d'une couleur donnée
     *
     * @param pawns           -> bitboard des pions qui jouent
     * @param opponentPawns   -> bitboard des pièces ennemies
     * @param enPassantSquare -> case en passant (la dernière position d'un pion qui avance de 2 cases
     * @param isWhite         -> si le joueur qui joue est blanc ou non
     * @return la liste des mouvement en passant
     */
    public static List<String> generateEnPassantMoves(long pawns, long opponentPawns, int enPassantSquare, boolean isWhite) {
        List<String> moves = new ArrayList<>();
        if (enPassantSquare != -1) {
            long enPassantBit = 1L << enPassantSquare;

            if (isWhite) {
                long capturesLeft = ((pawns & 0xFEFEFEFEFEFEFEFEL) << 7) & enPassantBit & ~opponentPawns;
                long capturesRight = ((pawns & 0x7F7F7F7F7F7F7F7FL) << 9) & enPassantBit & ~opponentPawns;
                if (capturesLeft != 0) {
                    int from = Long.numberOfTrailingZeros(capturesLeft >> 7);
                    moves.add(translateMove(from, enPassantSquare));
                }
                if (capturesRight != 0) {
                    int from = Long.numberOfTrailingZeros(capturesRight >> 9);
                    moves.add(translateMove(from, enPassantSquare));
                }
            } else {
                long capturesLeft = ((pawns & 0xFEFEFEFEFEFEFEFEL) >> 9) & enPassantBit & ~opponentPawns;
                long capturesRight = ((pawns & 0x7F7F7F7F7F7F7F7FL) >> 7) & enPassantBit & ~opponentPawns;

                if (capturesLeft != 0) {
                    int from = Long.numberOfTrailingZeros(capturesLeft << 9);
                    moves.add(translateMove(from, enPassantSquare));
                }
                if (capturesRight != 0) {
                    int from = Long.numberOfTrailingZeros(capturesRight << 7);
                    moves.add(translateMove(from, enPassantSquare));
                }
            }
        }

        return moves;
    }


    /**
     * Génère la liste de coup des promotions
     *
     * @param pawns          -> pions qui jouent
     * @param ownPieces      -> pièces qui jouent
     * @param opponentKing   -> roi ennemi
     * @param opponentPieces -> pièces ennemies
     * @param isWhite        -> si il est blanc
     * @return la liste des promotions possibles
     */
    public static List<String> generatePawnPromotions(long pawns, long ownPieces, long opponentKing, long opponentPieces,
                                                      boolean isWhite) {
        List<String> moves = new ArrayList<>();

        long promotions = isWhite
                ? pawns & 0x00FF000000000000L
                : pawns & 0x000000000000FF00L;
        while (promotions != 0) {
            int from = Long.numberOfTrailingZeros(promotions);
            int toStraight = isWhite ? from + 8 : from - 8;
            long toStraightBit = 1L << toStraight;
            if ((ownPieces & toStraightBit) == 0 && (opponentPieces & toStraightBit) == 0) {
                char[] promotionPieces = isWhite
                        ? new char[]{'Q', 'R', 'B', 'N'}
                        : new char[]{'q', 'r', 'b', 'n'}; // je ne sais pas si je dois laisser en maj pour les 2
                for (char promotion : promotionPieces) {
                    moves.add(translatePromMove(from, toStraight, promotion));
                }
            }

            if (isWhite) {
                if (from % 8 > 0 && (opponentPieces & ~opponentKing & (1L << (from + 7))) != 0) {
                    int toLeft = from + 7;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toLeft, promotion));
                    }
                }
                if (from % 8 < 7 && (opponentPieces & ~opponentKing & (1L << (from + 9))) != 0) {
                    int toRight = from + 9;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toRight, promotion));
                    }
                }
            } else {
                if (from % 8 > 0 && (opponentPieces & ~opponentKing & (1L << (from - 9))) != 0) {
                    int toLeft = from - 9;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toLeft, promotion));
                    }
                }
                if (from % 8 < 7 && (opponentPieces & ~opponentKing & (1L << (from - 7))) != 0) {
                    int toRight = from - 7;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toRight, promotion));
                    }
                }
            }
            promotions &= promotions - 1;
        }

        return moves;
    }

    public static List<String> generatePawnPromotionsCaptureOnlyGameMode(long pawns, long ownPieces, long opponentKing, long opponentPieces,
                                                                         boolean isWhite) {
        List<String> moves = new ArrayList<>();
        long promotions = isWhite
                ? pawns & 0x00FF000000000000L
                : pawns & 0x000000000000FF00L;
        while (promotions != 0) {
            int from = Long.numberOfTrailingZeros(promotions);
            int toStraight = isWhite ? from + 8 : from - 8;
            long toStraightBit = 1L << toStraight;
            if ((ownPieces & toStraightBit) == 0 && (opponentPieces & toStraightBit) == 0) {
                char[] promotionPieces = isWhite
                        ? new char[]{'Q', 'R', 'B', 'N', 'K'}
                        : new char[]{'q', 'r', 'b', 'n', 'k'}; // je ne sais pas si je dois laisser en maj pour les 2
                for (char promotion : promotionPieces) {
                    moves.add(translatePromMove(from, toStraight, promotion));
                }
            }

            //promotion en capturant avant
            if (isWhite) {
                if (from % 8 > 0 && (opponentPieces & ~opponentKing & (1L << (from + 7))) != 0) {
                    int toLeft = from + 7;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N', 'K'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toLeft, promotion));
                    }
                }
                if (from % 8 < 7 && (opponentPieces & ~opponentKing & (1L << (from + 9))) != 0) {
                    int toRight = from + 9;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N', 'K'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toRight, promotion));
                    }
                }
            } else {
                if (from % 8 > 0 && (opponentPieces & ~opponentKing & (1L << (from - 9))) != 0) {
                    int toLeft = from - 9;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n', 'k'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toLeft, promotion));
                    }
                }
                if (from % 8 < 7 && (opponentPieces & ~opponentKing & (1L << (from - 7))) != 0) {
                    int toRight = from - 7;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n', 'k'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMove(from, toRight, promotion));
                    }
                }
            }
            promotions &= promotions - 1;
        }

        return moves;
    }

    public static List<String> generatePawnPromotionsLosAlamos(long pawns, long ownPieces, long opponentKing, long opponentPieces,
                                                               boolean isWhite) {
        List<String> moves = new ArrayList<>();

        // Identify pawns ready for promotion
        long promotions = isWhite
                ? pawns & 0x00FF00000000L
                : pawns & 0x00000000FF00L;
        while (promotions != 0) {
            int from = Long.numberOfTrailingZeros(promotions);
            int toStraight = isWhite ? from + 6 : from - 6;
            long toStraightBit = 1L << toStraight;
            if ((ownPieces & toStraightBit) == 0 && (opponentPieces & toStraightBit) == 0) {
                char[] promotionPieces = isWhite
                        ? new char[]{'Q', 'R', 'B', 'N'}
                        : new char[]{'q', 'r', 'b', 'n'}; // je ne sais pas si je dois laisser en maj pour les 2
                for (char promotion : promotionPieces) {
                    moves.add(translatePromMoveLosAlamos(from, toStraight, promotion));
                }
            }

            if (isWhite) {
                if (from % 6 > 0 && (opponentPieces & ~opponentKing & (1L << (from + 5))) != 0) {
                    int toLeft = from + 5;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMoveLosAlamos(from, toLeft, promotion));
                    }
                }
                if (from % 6 < 5 && (opponentPieces & ~opponentKing & (1L << (from + 7))) != 0) {
                    int toRight = from + 7;
                    char[] promotionPieces = new char[]{'Q', 'R', 'B', 'N'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMoveLosAlamos(from, toRight, promotion));
                    }
                }
            } else {
                if (from % 6 > 0 && (opponentPieces & ~opponentKing & (1L << (from - 7))) != 0) {
                    int toLeft = from - 7;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMoveLosAlamos(from, toLeft, promotion));
                    }
                }
                if (from % 6 < 5 && (opponentPieces & ~opponentKing & (1L << (from - 5))) != 0) {
                    int toRight = from - 5;
                    char[] promotionPieces = new char[]{'q', 'r', 'b', 'n'};
                    for (char promotion : promotionPieces) {
                        moves.add(translatePromMoveLosAlamos(from, toRight, promotion));
                    }
                }
            }
            promotions &= promotions - 1;
        }

        return moves;
    }

    public static List<String> generatePawnMovesLosAlamos(long pawns, long ownPieces, long opponentPieces, boolean isWhite) {
        List<String> moves = new ArrayList<>();

        long singleStep, capturesLeft, capturesRight;
        if (isWhite) {

            // Déplacements du pion blanc
            singleStep = ((pawns & ~LosAlamosEngine.FILE_5) << 6) & ~ownPieces & ~opponentPieces; // Déplacement simple
            capturesLeft = ((pawns & ~LosAlamosEngine.FILE_A & ~LosAlamosEngine.FILE_5) << 5) & opponentPieces & ~ownPieces; // Capture à gauche
            capturesRight = ((pawns & ~LosAlamosEngine.FILE_F & ~LosAlamosEngine.FILE_5) << 7) & opponentPieces & ~ownPieces; // Capture à droite
            addPawnMovesLosAlamos(moves, singleStep, 6, isWhite);
            addPawnMovesLosAlamos(moves, capturesLeft, 5, isWhite);
            addPawnMovesLosAlamos(moves, capturesRight, 7, isWhite);
        } else {

            // Déplacements du pion noir
            singleStep = ((pawns & ~LosAlamosEngine.FILE_2) >>> 6) & ~ownPieces & ~opponentPieces; // Déplacement simple
            capturesLeft = ((pawns & ~LosAlamosEngine.FILE_2) >>> 5) & opponentPieces & ~LosAlamosEngine.FILE_F; // Capture à gauche
            capturesRight = ((pawns & ~LosAlamosEngine.FILE_2) >>> 7) & opponentPieces & ~LosAlamosEngine.FILE_A; // Capture à droite
            addPawnMovesLosAlamos(moves, singleStep, 6, isWhite);
            addPawnMovesLosAlamos(moves, capturesLeft, 5, isWhite);
            addPawnMovesLosAlamos(moves, capturesRight, 7, isWhite);
        }


        return moves;
    }


    /**
     * Ajout à la liste
     *
     * @param moves    -> liste
     * @param bitboard -> représentation bitboard
     * @param to       -> où la pièce va
     */
    private static void addEnPassantMoves(List<String> moves, long bitboard, int to) {
        while (bitboard != 0) {
            int from = Long.numberOfTrailingZeros(bitboard);
            moves.add(translateMove(from, to));
            bitboard &= bitboard - 1;
        }
    }

    /**
     * Ajout à la liste
     *
     * @param moves    -> liste
     * @param bitboard -> représentation bitboard
     * @param offset   -> où la pièce va 8 ou 16
     * @param isWhite  -> si le joueur est blanc
     */
    private static void addPawnMoves(List<String> moves, long bitboard, int offset, boolean isWhite) {
        while (bitboard != 0) {
            int to = Long.numberOfTrailingZeros(bitboard);
            int from = to - (isWhite ? offset : -offset);
            if (from >= 0 && from < 64) {
                moves.add(translateMove(from, to));
            }
            bitboard &= bitboard - 1;
        }
    }

    private static void addPawnMovesLosAlamos(List<String> moves, long bitboard, int offset, boolean isWhite) {
        while (bitboard != 0) {
            int to = Long.numberOfTrailingZeros(bitboard);
            int from = to - (isWhite ? offset : -offset);
            if (from >= 0 && from < 36) {
                moves.add(translateMoveLosAlamos(from, to));
            }
            bitboard &= bitboard - 1;
        }
    }

    private static String translateMoveLosAlamos(int from, int to) {
        return "" + (char) ('a' + from % 6) + (from / 6 + 1) + (char) ('a' + to % 6) + (to / 6 + 1);
    }

    private static String translateMove(int from, int to) {
        return "" + (char) ('a' + from % 8) + (from / 8 + 1) + (char) ('a' + to % 8) + (to / 8 + 1);
    }

    private static String translatePromMove(int from, int to, char promotionPiece) {
        char fromFile = (char) ('a' + (from % 8));
        char fromRank = (char) ('1' + (from / 8));
        char toFile = (char) ('a' + (to % 8));
        char toRank = (char) ('1' + (to / 8));
        // Format "e7e8Q" pour promotion
        return "" + fromFile + fromRank + toFile + toRank + promotionPiece;
    }
    private static String translatePromMoveLosAlamos(int from, int to, char promotionPiece) {
        char fromFile = (char) ('a' + (from % 6));
        char fromRank = (char) ('1' + (from / 6));
        char toFile = (char) ('a' + (to % 6));
        char toRank = (char) ('1' + (to / 6));
        // Format "e7e8Q" pour promotion
        return "" + fromFile + fromRank + toFile + toRank + promotionPiece;
    }


}
