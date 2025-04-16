package Springboot.engine;


/**
 * Améliorer unmakeMove en faisant des xor
 * Roque possible même si échecs
 * Quiescence search trop long (at 7 depth)
 * Order Moves: compléxité trop grande
 * <p>
 * A MODIFIER DANS QUEEN.JAVA LE GENERATE ROOKMOVES
 * A MODIFIER DANS ROOK.JAVA LES ANCIENNES FONCTION SSANS LES MBITBOARDS
 * A MODIFIER DANS GENERATEMOVES, LES FONCTIONS GENERATEROOKMOVES
 */

//History heuristics
//Silent moves - sort by PSQT value

//On se fait pas chier

import Springboot.piece.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author stizr
 * @license MIT License
 * <p>
 * Moteur de jeu fait en bitboards
 * <p>
 * Informations externes pour comprendre -> https://www.chessprogramming.org/Bitboards
 * <p>
 * But du bitboard:
 * Représenter l'échiquier sous forme de long en 64 bits -> pour les 64 cases de l'échiquier
 * <p>
 * Pour obtenir des informations plus précises comme quelle pièce est à tel emplacement,
 * on utilise d'autres bitboards (whitepawn, blackrook,...)
 * En combinant les 2 bitboards à l'aide de l'opération bitwise '&', on peut maintenant savoir
 * à quelle picèe cela correspond
 * <p>
 * Exemple:
 * <p>
 * bitboard (réduit) : 0b00010111L
 * whitepieces : 0b00000011L
 * blackpawns : 0b00010100L
 * <p>
 * Si on fait bitboard & blackpawns, on obtient 0b00010100L Soit on sait que sur l'échiquier
 * les cases suivantes sont des pions noires
 * <p>
 * Pourquoi le bitboard:
 * On utilise les bitboards afin de pouvoir faire dew opérations (coups possibles)
 * beaucoup plus rapidement qu'une représentation tel que les doubles tableaux
 * <p>
 * Le problème est que c'est plus difficile à comprendre mais une fois compris
 * cela devient très intuitif
 * <p>
 * Perft:
 * "Perft" est tout simplement notre moteur qui va jouer tout les coups possibles et légaux.
 * Cela va permettre de tester notre moteur en calculant le nombres de noeuds à la seconde (NPS)
 * Ou pour voire s'il y a des coups en trop ou en moins par rapport à stockfish.
 * Cela pourrait indiquer des règles manquantes ou des cas illégaux.
 * <p>
 * Voici le résultat de notre perft sur la position suivante:
 * <p>
 * r n b q k b n r
 * p p p p p p p p
 * . . . . . . . .
 * . . . . . . . .
 * . . . . . . . .
 * . . . . . . . .
 * P P P P P P P P
 * R N B Q K B N R
 * <p>
 * Notre Moteur:                                           Stcokfish:
 * Depth 1: 20 nodes (0,028 seconds)           -           20
 * Depth 2: 400 nodes (0,007 seconds)          -           400
 * Depth 3: 8902 nodes (0,030 seconds)         -           8902
 * Depth 4: 197281 nodes (0,212 seconds)       -           197281
 * Depth 5: 4865607 nodes (1,823 seconds)      -           4865609
 * Depth 6: 119060496 nodes (29,314 seconds)   -           119,060,324
 * Depth 7: 3195902725 nodes (756,324 seconds) -           3,195,901,860
 * <p>
 * V2:
 * Depth 1: 20 nodes (0,011 seconds)
 * Depth 2: 400 nodes (0,004 seconds)
 * Depth 3: 8902 nodes (0,017 seconds)
 * Depth 4: 197281 nodes (0,127 seconds)
 * Depth 5: 4865607 nodes (1,200 seconds)
 * <p>
 * [ISSUE]
 * On remarque qu'il manque des coups dans notre moteur.
 * Pour vérifier pourquoi c'est plus problématique mais voici ce que j'ai trouvé pour le moment:
 * Le roi peut roquer même en étant en échec
 * Je ne vérifie pas que les cases entre (pour le roques) ne sont pas la cible d'attaques
 * Et après je ne sais pas.
 * <p>
 * [SOLUTION]
 * Mettre en place une matrice d'attaques des pièces afin de vérifier si une case donné est attaqué ou non
 * Magics bitboards,...
 * <p>
 * [ROAD MAP]
 * -MagicBitboard:
 * Faire l'implémentation des Magics Bitboards qui vont permettrent de générer les attaques
 * plus rapidement à l'aide de nombres "magiques".
 * <p>
 * -Corrections:
 * Correction de bugs tels que les coups illégaux ou incomplets
 * Reconnaissance des coups en triple (3 coups pareils d'affilés)
 * <p>
 * -Lisibilité:
 * Amélioration générale de la qualité du code en séparant par exemple de grosses fonctions
 * en sous fonctions (makeMove(),...)
 * <p>
 * -Precompute:
 * Faire des pré-calcul sur le plateau d'échiquier, plutôt que de devoir chercher les pièces à chaque fois et de
 * vérifier si elles sortent du plateau ou non.
 */
public class Engine {

    //MATRICES
    public long whitePawns, whiteRooks, whiteKnights, whiteBishops, whiteQueens, whiteKing;
    public long blackPawns, blackRooks, blackKnights, blackBishops, blackQueens, blackKing;
    public long whitePieces, blackPieces;

    public long blackAttacks, whiteAttacks;
    public long whitePawnsAttack, whiteRooksAttack, whiteKnightsAttack, whiteBishopsAttack, whiteQueensAttack, whiteKingAttack, whitePawnsAttacksAllied;
    public long blackPawnsAttack, blackRooksAttack, blackKnightsAttack, blackBishopsAttack, blackQueensAttack, blackKingAttack, blackPawnsAttacksAllied;

    public long blackDefense, whiteDefense;
    public long whitePawnsDefense, whiteRooksDefense, whiteKnightsDefense, whiteBishopsDefense, whiteQueensDefense, whiteKingDefense;
    public long blackPawnsDefense, blackRooksDefense, blackKnightsDefense, blackBishopsDefense, blackQueensDefense, blackKingDefense;

    //DROITS
    public int enPassantSquare = -1;
    public int enPassantColor;
    public int ColorToMove;
    public boolean[] castlingRights = {}; // [White long, White short, Black long, Black short]


    //COUPS
    //règle des 50 coups
    public int nbMoveSinceCapture;
    //nb coups (noir + blanc) total
    public int nbMoveTotal;
    public ChessClock clock;

    public Stack<MoveState> moveHistory = new Stack<>();
    public Stack<Engine> s = new Stack<>();
    public Zobrist zobrist;
    public long zobristKey;

    public boolean flagEnd = false;

    public static final List<String> MOVE_BUFFER = new ArrayList<>(600);
    // Magics config;


    //CONSTANTES
    public static final long FILE_A = 0x0101010101010101L; // Colonne A
    public static final long FILE_H = 0x8080808080808080L; // Colonne H
    public static final long FILE_AB = 0x0303030303030303L; // Colonnes A et B
    public static final long FILE_GH = 0xC0C0C0C0C0C0C0C0L; // Colonnes G et H
    public static final long FILE_1 = 0x00000000000000FFL;
    public static final long FILE_8 = 0xFF00000000000000L;
    public static final long FILE_87 = 0xFFFF000000000000L;
    public static final long FILE_12 = 0x000000000000FFFFL;
    public static final long OUT_OF_BOUNDS_MASK = 0xFFFFFFFFFFFFFFFFL; // Hors échiquier
    public static final long centerSquares = 0x1818000000L;

    public RepetitionTable repetitionTable;


    // Masques pertinents pour les tours et les fous
//    private static final long[] ROOK_MASKS = new long[64];
//    private static final long[] BISHOP_MASKS = new long[64];

    static {
        MagicBitboards.initMagics();
    }

    public void resetToInitialPosition() {
        setFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }


    public Engine(String fen) {
        setFEN(fen);
        repetitionTable = new RepetitionTable();
        zobrist = new Zobrist();
        zobristKey = Zobrist.getKeyForBoard(this);
        clock = new ChessClock(180_000_000);
    }

//    private static void initializeMasks() {
//        for (int square = 0; square < 64; square++) {
//            ROOK_MASKS[square] = calculateRookMask(square);
//            BISHOP_MASKS[square] = calculateBishopMask(square);
//        }
//    }

    /**
     * Calcule le masque pertinent pour une tour sur une case donnée.
     */
//    private static long calculateRookMask(int square) {
//        long mask = 0L;
//        int rank = square / 8;
//        int file = square % 8;
//        for (int f = file + 1; f < 8; f++) mask |= (1L << (rank * 8 + f));
//        for (int f = file - 1; f >= 0; f--) mask |= (1L << (rank * 8 + f));
//        for (int r = rank + 1; r < 8; r++) mask |= (1L << (r * 8 + file));
//        for (int r = rank - 1; r >= 0; r--) mask |= (1L << (r * 8 + file));
//        return mask & ~(1L << square); // Exclut uniquement la case de la tour
//    }

    /**
     * Calcule le masque pertinent pour un fou sur une case donnée.
     */
//    private static long calculateBishopMask(int square) {
//        long mask = 0L;
//
//        int rank = square / 8;
//        int file = square % 8;
//
//        for (int r = rank + 1, f = file + 1; r < 7 && f < 7; r++, f++) {
//            mask |= (1L << (r * 8 + f));
//        }
//        for (int r = rank - 1, f = file - 1; r > 0 && f > 0; r--, f--) {
//            mask |= (1L << (r * 8 + f));
//        }
//
//        for (int r = rank + 1, f = file - 1; r < 7 && f > 0; r++, f--) {
//            mask |= (1L << (r * 8 + f));
//        }
//        for (int r = rank - 1, f = file + 1; r > 0 && f < 7; r--, f++) {
//            mask |= (1L << (r * 8 + f));
//        }
//
//        return mask;
//    }

    /**
     * Initialise les tables d'attaques pour les tours et les fous.
     */
//    private static void initializeAttackTables() {
//        initializeRookAttackTable();
//        initializeBishopAttackTable();
//    }

    /**
     * Initialise la table des attaques pour les tours.
     */
//    private static void initializeRookAttackTable() {
//        Magics config = Magics.getInstance();
//        for (int square = 0; square < 64; square++) {
//            long mask = ROOK_MASKS[square];
//            int occupancyVariations = 1 << Long.bitCount(mask);
//            Map.Entry<Long, Byte> magicData = config.getRookMagics(square);
//            if (magicData == null) {
//                throw new IllegalStateException("Magic data not found for rook at square " + square);
//            }
//            long magicNumber = magicData.getKey();
//            byte magicShift = magicData.getValue();
//            boolean[] usedIndices = new boolean[1 << (64 - magicShift)]; // Taille de la table
//
//            for (int occIndex = 0; occIndex < occupancyVariations; occIndex++) {
//                long occupancy = calculateOccupancy(mask, occIndex);
//                long attacks = calculateRookAttacksForOccupancy(square, occupancy);
//                int magicIndex = (int) ((occupancy * magicNumber) >>> magicShift);
//
//                if (usedIndices[magicIndex] && ROOK_ATTACK_TABLE[square][magicIndex] != attacks) {
//                    System.err.println("Collision detected at square " + square + ", index " + magicIndex);
//                }
//                usedIndices[magicIndex] = true;
//                ROOK_ATTACK_TABLE[square][magicIndex] = attacks;
//            }
//        }
//    }

    /**
     * Initialise la table des attaques pour les fous.
     */
//    private static void initializeBishopAttackTable() {
//        Magics config = Magics.getInstance();
//
//        for (int square = 0; square < 64; square++) {
//            long mask = BISHOP_MASKS[square];
//            int occupancyVariations = 1 << Long.bitCount(mask);
//
//            for (int occIndex = 0; occIndex < occupancyVariations; occIndex++) {
//                long occupancy = calculateOccupancy(mask, occIndex);
//                long attacks = calculateBishopAttacksForOccupancy(square, occupancy);
//
//                Map.Entry<Long, Byte> magicData = config.getBishopMagics(square);
//                long magicNumber = magicData.getKey();
//                byte magicShift = magicData.getValue();
//
//                int magicIndex = (int) ((occupancy * magicNumber) >>> magicShift);
//                BISHOP_ATTACK_TABLE[square][magicIndex] = attacks;
//            }
//        }
//    }
//
//    public static long calculateBishopAttacks(int square, long occupancy) {
//        Magics config = Magics.getInstance();
//        Map.Entry<Long, Byte> magicData = config.getBishopMagics(square);
//
//        long magicNumber = magicData.getKey();
//        byte magicShift = magicData.getValue();
//
//        long relevantOccupancy = occupancy & calculateBishopMask(square);
//
//        int magicIndex = (int) ((relevantOccupancy * magicNumber) >>> magicShift);
//
//        return BISHOP_ATTACK_TABLE[square][magicIndex];
//    }
//
//    public static long calculateRookAttacks(int square, long occupancy) {
//        Magics config = Magics.getInstance();
//        Map.Entry<Long, Byte> magicData = config.getRookMagics(square);
//
//        long magicNumber = magicData.getKey();
//        byte magicShift = magicData.getValue();
//
//        long relevantOccupancy = occupancy & calculateRookMask(square);
//
//        int magicIndex = (int) ((relevantOccupancy * magicNumber) >>> magicShift);
//
//        return ROOK_ATTACK_TABLE[square][magicIndex];
//    }


    /**
     * Calcule une variation d'occupation à partir d'un masque et d'un index.
     */
    private static long calculateOccupancy(long mask, int index) {
        long occupancy = 0L;
        int bitPosition = 0;

        while (mask != 0) {
            int square = Long.numberOfTrailingZeros(mask);
            mask &= mask - 1; // Supprime le bit traité

            if ((index & (1 << bitPosition)) != 0) {
                occupancy |= (1L << square);
            }
            bitPosition++;
        }

        return occupancy;
    }

    /**
     * Calcule les attaques d'une tour pour une case et une occupation donnée.
     */
    private static long calculateRookAttacksForOccupancy(int square, long occupancy) {
        long attacks = 0L;

        int rank = square / 8;
        int file = square % 8;

        for (int f = file + 1; f < 8; f++) {
            attacks |= (1L << (rank * 8 + f));
            if ((occupancy & (1L << (rank * 8 + f))) != 0) break;
        }
        for (int f = file - 1; f >= 0; f--) {
            attacks |= (1L << (rank * 8 + f));
            if ((occupancy & (1L << (rank * 8 + f))) != 0) break;
        }

        for (int r = rank + 1; r < 8; r++) {
            attacks |= (1L << (r * 8 + file));
            if ((occupancy & (1L << (r * 8 + file))) != 0) break;
        }
        for (int r = rank - 1; r >= 0; r--) {
            attacks |= (1L << (r * 8 + file));
            if ((occupancy & (1L << (r * 8 + file))) != 0) break;
        }

        return attacks;
    }

    private static long calculateBishopAttacksForOccupancy(int square, long occupancy) {
        long attacks = 0L;

        int rank = square / 8;
        int file = square % 8;

        for (int r = rank + 1, f = file + 1; r < 8 && f < 8; r++, f++) {
            attacks |= (1L << (r * 8 + f));
            if ((occupancy & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            attacks |= (1L << (r * 8 + f));
            if ((occupancy & (1L << (r * 8 + f))) != 0) break;
        }

        for (int r = rank + 1, f = file - 1; r < 8 && f >= 0; r++, f--) {
            attacks |= (1L << (r * 8 + f));
            if ((occupancy & (1L << (r * 8 + f))) != 0) break;
        }
        for (int r = rank - 1, f = file + 1; r >= 0 && f < 8; r--, f++) {
            attacks |= (1L << (r * 8 + f));
            if ((occupancy & (1L << (r * 8 + f))) != 0) break;
        }

        return attacks;
    }


    public Engine clone() {

        Engine clone = new Engine(getFEN());

        clone.whitePawns = this.whitePawns;
        clone.whiteRooks = this.whiteRooks;
        clone.whiteKnights = this.whiteKnights;
        clone.whiteBishops = this.whiteBishops;
        clone.whiteQueens = this.whiteQueens;
        clone.whiteKing = this.whiteKing;
        clone.blackPawns = this.blackPawns;
        clone.blackRooks = this.blackRooks;
        clone.blackKnights = this.blackKnights;
        clone.blackBishops = this.blackBishops;
        clone.blackQueens = this.blackQueens;
        clone.blackKing = this.blackKing;
        clone.blackAttacks = this.blackAttacks;
        clone.whiteAttacks = this.whiteAttacks;
        clone.whitePawnsAttack = this.whitePawnsAttack;
        clone.whiteRooksAttack = this.whiteRooksAttack;
        clone.whiteKnightsAttack = this.whiteKnightsAttack;
        clone.whiteBishopsAttack = this.whiteBishopsAttack;
        clone.whiteQueensAttack = this.whiteQueensAttack;
        clone.whiteKingAttack = this.whiteKingAttack;
        clone.blackPawnsAttack = this.blackPawnsAttack;
        clone.blackRooksAttack = this.blackRooksAttack;
        clone.blackKnightsAttack = this.blackKnightsAttack;
        clone.blackBishopsAttack = this.blackBishopsAttack;
        clone.blackQueensAttack = this.blackQueensAttack;
        clone.blackKingAttack = this.blackKingAttack;
        clone.blackDefense = this.blackDefense;
        clone.whiteDefense = this.whiteDefense;
        clone.whitePawnsDefense = this.whitePawnsDefense;
        clone.whiteRooksDefense = this.whiteRooksDefense;
        clone.whiteKnightsDefense = this.whiteKnightsDefense;
        clone.whiteBishopsDefense = this.whiteBishopsDefense;
        clone.whiteQueensDefense = this.whiteQueensDefense;
        clone.whiteKingDefense = this.whiteKingDefense;
        clone.blackPawnsDefense = this.blackPawnsDefense;
        clone.blackRooksDefense = this.blackRooksDefense;
        clone.blackKnightsDefense = this.blackKnightsDefense;
        clone.blackBishopsDefense = this.blackBishopsDefense;
        clone.blackQueensDefense = this.blackQueensDefense;
        clone.blackKingDefense = this.blackKingDefense;
        clone.enPassantSquare = this.enPassantSquare;
        clone.enPassantColor = this.enPassantColor;
        clone.ColorToMove = this.ColorToMove;
        clone.castlingRights = this.castlingRights.clone();
        clone.nbMoveSinceCapture = this.nbMoveSinceCapture;
        clone.nbMoveTotal = this.nbMoveTotal;
        clone.moveHistory = (Stack<MoveState>) this.moveHistory.clone();

        return clone;
    }


    /**
     * Permet de mettre en place l'échiquer selon une notation FEN
     *
     * @param fen
     */
    public void setFEN(String fen) {
        whiteAttacks = blackAttacks = 0L;
        String[] parts = fen.split(" ");
        String board = parts[0];
        String turn = parts[1];
        String castling = parts[2];
        enPassantSquare = getEnPassantSquare(parts[3]);
        String capture = parts[4];
        String move = parts[5];
        castlingRights = new boolean[]{castling.contains("K"), castling.contains("Q"), castling.contains("k"), castling.contains("q")};
        nbMoveSinceCapture = Integer.parseInt(capture);
        nbMoveTotal = Integer.parseInt(move);
        ColorToMove = turn.equals("w") ? 1 : -1;
        whitePawns = whiteRooks = whiteKnights = whiteBishops = whiteQueens = whiteKing = whitePieces = 0L;
        blackPawns = blackRooks = blackKnights = blackBishops = blackQueens = blackKing = blackPieces = 0L;
        whitePawnsAttack = whiteBishopsAttack = whiteKingAttack = whiteKnightsAttack = whiteRooksAttack = whiteQueensAttack = whitePawnsAttacksAllied = 0L;
        blackRooksAttack = blackKingAttack = blackPawnsAttack = blackQueensAttack = blackKnightsAttack = blackBishopsAttack = blackPawnsAttacksAllied = 0L;
        int square = 56;
        for (char c : board.toCharArray()) {
            if (c == '/') {
                square -= 16;
            } else if (Character.isDigit(c)) {
                square += (c - '0');
            } else {
                long bit = 1L << square;
                switch (c) {
                    case 'P':
                        whitePawns |= bit;
                        break;
                    case 'R':
                        whiteRooks |= bit;
                        break;
                    case 'N':
                        whiteKnights |= bit;
                        break;
                    case 'B':
                        whiteBishops |= bit;
                        break;
                    case 'Q':
                        whiteQueens |= bit;
                        break;
                    case 'K':
                        whiteKing |= bit;
                        break;
                    case 'p':
                        blackPawns |= bit;
                        break;
                    case 'r':
                        blackRooks |= bit;
                        break;
                    case 'n':
                        blackKnights |= bit;
                        break;
                    case 'b':
                        blackBishops |= bit;
                        break;
                    case 'q':
                        blackQueens |= bit;
                        break;
                    case 'k':
                        blackKing |= bit;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid FEN character: " + c);
                }
                square++;
            }
        }


        whitePieces = whitePawns | whiteBishops | whiteKnights | whiteRooks | whiteQueens | whiteKing;
        blackPieces = blackPawns | blackBishops | blackKnights | blackRooks | blackQueens | blackKing;
        //updatePieceBitboards();
        updateAttackBitboards();
        //updateDefenseBitboards();
    }

    /**
     * Génére le FEN associé à la position actuelle
     *
     * @return le fen associé
     */
    public String getFEN() {
        StringBuilder fen = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int squareIndex = rank * 8 + file;
                char piece = getPieceAt(squareIndex);
                if (piece == '.') {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (rank > 0) {
                fen.append('/');
            }
        }

        fen.append(' ').append(ColorToMove == 1 ? 'w' : 'b');

        fen.append(' ');
        if (castlingRights[0]) fen.append('K');
        if (castlingRights[1]) fen.append('Q');
        if (castlingRights[2]) fen.append('k');
        if (castlingRights[3]) fen.append('q');
        if (!castlingRights[0] && !castlingRights[1] && !castlingRights[2] && !castlingRights[3]) {
            fen.append('-');
        }

        fen.append(' ').append(enPassantSquare != -1 ? convertNumberToSquare(enPassantSquare) : '-');

        fen.append(' ').append(nbMoveSinceCapture);

        fen.append(' ').append(nbMoveTotal);

        return fen.toString();
    }

    public boolean isCastlingWhiteLong() {
        return this.castlingRights[0];
    }


    public boolean isCastlingWhiteShort() {
        return this.castlingRights[1];
    }

    public boolean isCastlingBlackLong() {
        return this.castlingRights[2];
    }

    public boolean isCastlingBlackShort() {
        return this.castlingRights[3];
    }

    public int getEnPassantSquare(String enPassant) {
        if (enPassant.equals("-")) return -1;
        int file = enPassant.charAt(0) - 'a';
        int rank = enPassant.charAt(1) - '1';
        return rank * 8 + file;
    }


    public static String convertNumberToSquare(int index) {
        int row = index / 8;
        int col = index % 8;
        char file = (char) ('a' + col);
        char rank = (char) ('1' + row);
        return "" + file + rank;
    }

    public void makeMove2(String move) {
        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        zobristKey ^= Zobrist.getKeyForMove(from, to, getPieceAt(from));
    }

    /**
     * Fonction qui effectue le coup et met à jour la liste des droits
     *
     * @param move -> coup joué
     */
    public void makeMove(String move) {
        if (!flagEnd) {


            List<String> legalMoves = generateLegalMoves(ColorToMove, false);


            if (!legalMoves.contains(move)) {
                throw new IllegalArgumentException("Mouvement illégal: " + move);
            }


            int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
            int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
            long fromBit = 1L << from;
            long toBit = 1L << to;

            //int pieceType = getPieceAt(from);


            if ((ColorToMove == 1 && (whitePieces() & fromBit) == 0) ||
                    (ColorToMove == -1 && (blackPieces() & fromBit) == 0)) {
                throw new IllegalArgumentException("C'est au tour des " + (ColorToMove == 1 ? "Blancs" : "Noirs"));
            }

            if (nbMoveSinceCapture + 1 == 51)
                throw new IllegalArgumentException("Plus de 50 coups depuis la dernière capture.");

//        zobrist.updateZobrist(move, this);

            if (getPieceAt(to) == '.') nbMoveSinceCapture++;
            else nbMoveSinceCapture = 0;

            long piece = ColorToMove == 1 ? whitePawns : blackPawns;
            long otherPiece = ColorToMove == 1 ? blackPawns : whitePawns;
            if ((piece & fromBit) != 0) {
                if (Math.abs(from - to) == 16) {
                    enPassantSquare = ColorToMove == 1 ? (to - 8) : (to + 8);
                    enPassantColor = ColorToMove;
                }
            }

//        if ((ColorToMove == 1 && (whitePieces() & toBit) != 0) ||
//                (ColorToMove == -1 && (blackPieces() & toBit) != 0)) {
//            throw new IllegalArgumentException("Vous ne pouvez pas capturer une pièce alliée.");
//        }

            if ((whitePieces() & toBit) != 0) {
                whitePawns &= ~toBit;
                whiteRooks &= ~toBit;
                whiteKnights &= ~toBit;
                whiteBishops &= ~toBit;
                whiteQueens &= ~toBit;
                whiteKing &= ~toBit;
            } else if ((blackPieces() & toBit) != 0) {
                blackPawns &= ~toBit;
                blackRooks &= ~toBit;
                blackKnights &= ~toBit;
                blackBishops &= ~toBit;
                blackQueens &= ~toBit;
                blackKing &= ~toBit;
            }

            if ((whiteKing & fromBit) != 0) {
                whiteKing ^= fromBit | toBit;
                castlingRights[0] = castlingRights[1] = false;

                // Déplacement des tours pour un roque blanc
                if (to - from == 2) {
                    whiteRooks ^= (1L << 7) | (1L << 5);
                } else if (to - from == -2) {
                    whiteRooks ^= (1L << 3) | (1L << 0);
                }
            } else if ((blackKing & fromBit) != 0) {
                blackKing ^= fromBit | toBit;
                castlingRights[2] = castlingRights[3] = false;

                // Déplacement des tours pour un roque noir
                if (to - from == 2) {
                    blackRooks ^= (1L << 63) | (1L << 61);
                } else if (to - from == -2) {
                    blackRooks ^= (1L << 56) | (1L << 59);
                }
            }

            // Capture en passant
            if (enPassantSquare != -1 && to == enPassantSquare) {
                if (ColorToMove == 1) {
                    blackPawns &= ~(1L << (to - 8));
                } else {
                    whitePawns &= ~(1L << (to + 8));
                }
                enPassantSquare = -1;
            }

            // Promotion de pions
            if ((ColorToMove == 1 && (to / 8 == 7) && (piece & fromBit) != 0) ||
                    (ColorToMove == -1 && (to / 8 == 0) && (piece & fromBit) != 0)) {
                char promotionPiece = move.length() == 5 ? move.charAt(4) : ColorToMove == 1 ? 'Q' : 'q'; // Par défaut à Reine si non spécifié

                if (ColorToMove == 1) {
                    whitePawns ^= fromBit;
                    switch (promotionPiece) {
                        case 'Q':
                            whiteQueens |= toBit;
                            break;
                        case 'R':
                            whiteRooks |= toBit;
                            break;
                        case 'B':
                            whiteBishops |= toBit;
                            break;
                        case 'N':
                            whiteKnights |= toBit;
                            break;
                    }
                } else {
                    blackPawns ^= fromBit;
                    switch (promotionPiece) {
                        case 'q':
                            blackQueens |= toBit;
                            break;
                        case 'r':
                            blackRooks |= toBit;
                            break;
                        case 'b':
                            blackBishops |= toBit;
                            break;
                        case 'n':
                            blackKnights |= toBit;
                            break;
                    }
                }
            } else {
                // Mise à jour des bitboards pour les autres pièces
                if ((whitePawns & fromBit) != 0) {
                    whitePawns ^= fromBit | toBit;
                } else if ((blackPawns & fromBit) != 0) {
                    blackPawns ^= fromBit | toBit;
                } else if ((whiteRooks & fromBit) != 0) {
                    whiteRooks ^= fromBit | toBit;
                } else if ((blackRooks & fromBit) != 0) {
                    blackRooks ^= fromBit | toBit;
                } else if ((whiteKnights & fromBit) != 0) {
                    whiteKnights ^= fromBit | toBit;
                } else if ((blackKnights & fromBit) != 0) {
                    blackKnights ^= fromBit | toBit;
                } else if ((whiteBishops & fromBit) != 0) {
                    whiteBishops ^= fromBit | toBit;
                } else if ((blackBishops & fromBit) != 0) {
                    blackBishops ^= fromBit | toBit;
                } else if ((whiteQueens & fromBit) != 0) {
                    whiteQueens ^= fromBit | toBit;
                } else if ((blackQueens & fromBit) != 0) {
                    blackQueens ^= fromBit | toBit;
                }
            }
            if (enPassantColor != ColorToMove) {
                enPassantSquare = -1;
            }

            //update les matrices d'attaques
            //updatePieceBitboards();
            updateAttackBitboards();
            //updateDefenseBitboards();

            whitePieces = whitePawns | whiteBishops | whiteKnights | whiteRooks | whiteQueens | whiteKing;
            blackPieces = blackPieces | blackBishops | blackKnights | blackRooks | blackQueens | blackKing;
//        System.out.println("White " + Long.toBinaryString(whiteAttacks));
//        System.out.println("Black " + Long.toBinaryString(blackAttacks));


            if (ColorToMove == -1) nbMoveTotal++;
            ColorToMove = -ColorToMove;

            String status = checkGameStatus();
            if (!status.equals("Game continues")) {
                flagEnd = true;
                System.out.println(status);
            }

        }

    }

    private void endDraw() {
        flagEnd = true;
        System.out.println("Draw");
    }

//    public void makeRapidMove(String move) {
//        if (isCheckmate(ColorToMove)) return;
//        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
//        char destinationPiece = getPieceAt(to);
//        boolean legalMoves = false;
//        moveHistory.push(new MoveState(this));
//        executeMove(move);
//        boolean kingInCheck = isKingInCheck(ColorToMove);
//        if (!kingInCheck/*ColorToMove == 1 ? (whiteKing & blackAttacks) == 0 : (blackKing & whiteAttacks) == 0*/) {
//            legalMoves = true;
//        }
//        unmakeMove();
//        if (!legalMoves) {
//            throw new IllegalArgumentException("Mouvement illégal: " + move);
//        }
//
//        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
//        //int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
//        long fromBit = 1L << from;
//        long toBit = 1L << to;
//
//        int pieceType = from;
//
//        if ((ColorToMove == 1 && (whitePieces() & fromBit) == 0) ||
//                (ColorToMove == -1 && (blackPieces() & fromBit) == 0)) {
//            throw new IllegalArgumentException("C'est au tour des " + (ColorToMove == 1 ? "Blancs" : "Noirs"));
//        }
//
//        if (nbMoveSinceCapture + 1 == 51)
//            throw new IllegalArgumentException("Plus de 50 coups depuis la dernière capture.");
//
////        zobrist.updateZobrist(move, this);
//        if (getPieceAt(to) == '.') nbMoveSinceCapture++;
//        else nbMoveSinceCapture = 0;
//
//        long piece = ColorToMove == 1 ? whitePawns : blackPawns;
//        long otherPiece = ColorToMove == 1 ? blackPawns : whitePawns;
//        if ((piece & fromBit) != 0) {
//            if (Math.abs(from - to) == 16) {
//                enPassantSquare = ColorToMove == 1 ? (to - 8) : (to + 8);
//                enPassantColor = ColorToMove;
//            }
//        }
//
//        if ((ColorToMove == 1 && (whitePieces() & toBit) != 0) ||
//                (ColorToMove == -1 && (blackPieces() & toBit) != 0)) {
//            throw new IllegalArgumentException("Vous ne pouvez pas capturer une pièce alliée.");
//        }
//
//        if ((whitePieces() & toBit) != 0) {
//            whitePawns &= ~toBit;
//            whiteRooks &= ~toBit;
//            whiteKnights &= ~toBit;
//            whiteBishops &= ~toBit;
//            whiteQueens &= ~toBit;
//            whiteKing &= ~toBit;
//        } else if ((blackPieces() & toBit) != 0) {
//            blackPawns &= ~toBit;
//            blackRooks &= ~toBit;
//            blackKnights &= ~toBit;
//            blackBishops &= ~toBit;
//            blackQueens &= ~toBit;
//            blackKing &= ~toBit;
//        }
//
//        if ((whiteKing & fromBit) != 0) {
//            whiteKing ^= fromBit | toBit;
//            castlingRights[0] = castlingRights[1] = false;
//
//            // Déplacement des tours pour un roque blanc
//            if (to - from == 2) {
//                whiteRooks ^= (1L << 7) | (1L << 5);
//            } else if (to - from == -2) {
//                whiteRooks ^= (1L << 3) | (1L << 0);
//            }
//        } else if ((blackKing & fromBit) != 0) {
//            blackKing ^= fromBit | toBit;
//            castlingRights[2] = castlingRights[3] = false;
//
//            // Déplacement des tours pour un roque noir
//            if (to - from == 2) {
//                blackRooks ^= (1L << 63) | (1L << 61);
//            } else if (to - from == -2) {
//                blackRooks ^= (1L << 56) | (1L << 59);
//            }
//        }
//
//        // Capture en passant
//        if (enPassantSquare != -1 && to == enPassantSquare) {
//            if (ColorToMove == 1) {
//                blackPawns &= ~(1L << (to - 8));
//            } else {
//                whitePawns &= ~(1L << (to + 8));
//            }
//            enPassantSquare = -1;
//        }
//
//        // Promotion de pions
//        if ((ColorToMove == 1 && (to / 8 == 7) && (piece & fromBit) != 0) ||
//                (ColorToMove == -1 && (to / 8 == 0) && (piece & fromBit) != 0)) {
//            char promotionPiece = move.length() == 5 ? move.charAt(4) : ColorToMove == 1 ? 'Q' : 'q'; // Par défaut à Reine si non spécifié
//
//            if (ColorToMove == 1) {
//                whitePawns ^= fromBit;
//                switch (promotionPiece) {
//                    case 'Q':
//                        whiteQueens |= toBit;
//                        break;
//                    case 'R':
//                        whiteRooks |= toBit;
//                        break;
//                    case 'B':
//                        whiteBishops |= toBit;
//                        break;
//                    case 'N':
//                        whiteKnights |= toBit;
//                        break;
//                }
//            } else {
//                blackPawns ^= fromBit;
//                switch (promotionPiece) {
//                    case 'q':
//                        blackQueens |= toBit;
//                        break;
//                    case 'r':
//                        blackRooks |= toBit;
//                        break;
//                    case 'b':
//                        blackBishops |= toBit;
//                        break;
//                    case 'n':
//                        blackKnights |= toBit;
//                        break;
//                }
//            }
//        } else {
//            // Mise à jour des bitboards pour les autres pièces
//            if ((whitePawns & fromBit) != 0) {
//                whitePawns ^= fromBit | toBit;
//            } else if ((blackPawns & fromBit) != 0) {
//                blackPawns ^= fromBit | toBit;
//            } else if ((whiteRooks & fromBit) != 0) {
//                whiteRooks ^= fromBit | toBit;
//            } else if ((blackRooks & fromBit) != 0) {
//                blackRooks ^= fromBit | toBit;
//            } else if ((whiteKnights & fromBit) != 0) {
//                whiteKnights ^= fromBit | toBit;
//            } else if ((blackKnights & fromBit) != 0) {
//                blackKnights ^= fromBit | toBit;
//            } else if ((whiteBishops & fromBit) != 0) {
//                whiteBishops ^= fromBit | toBit;
//            } else if ((blackBishops & fromBit) != 0) {
//                blackBishops ^= fromBit | toBit;
//            } else if ((whiteQueens & fromBit) != 0) {
//                whiteQueens ^= fromBit | toBit;
//            } else if ((blackQueens & fromBit) != 0) {
//                blackQueens ^= fromBit | toBit;
//            }
//        }
//        if (enPassantColor != ColorToMove) {
//            enPassantSquare = -1;
//        }
//
//        //update les matrices d'attaques
//
//        //updatePieceBitboards();
//        //updateAttackBitboards();
//
////        System.out.println("White " + Long.toBinaryString(whiteAttacks));
////        System.out.println("Black " + Long.toBinaryString(blackAttacks));
//
//        if (ColorToMove == -1) nbMoveTotal++;
//        ColorToMove = -ColorToMove;
//    }

    private boolean isKingUnderAttack(long kingPosition, int opponentColor) {
        List<String> opponentMoves = generateMoves(opponentColor);

        for (String opponentMove : opponentMoves) {
            int opponentTo = (opponentMove.charAt(3) - '1') * 8 + (opponentMove.charAt(2) - 'a');
            long opponentToBit = 1L << opponentTo;

            if ((kingPosition & opponentToBit) != 0) {
                return true; // Le roi est attaqué
            }
        }
        return false;
    }

    public int getDestinationSquare(String move) {
        String to = move.substring(2, 4);
        return squareToIndex(to);
    }

    public List<Integer> getAttackers(int targetSquare, int attackerColor) {
        List<Integer> attackers = new ArrayList<>();
        List<String> opponentMoves = generateLegalMoves(attackerColor, false); // Générer les coups légaux de l'adversaire

        for (String move : opponentMoves) {
            if (getDestinationSquare(move) == targetSquare) {
                attackers.add(squareToIndex(move.substring(0, 2))); // Ajoute la case de départ de l'attaquant
            }
        }
        return attackers;
    }

    public List<Integer> getDefenders(int targetSquare, int defenderColor) {
        List<Integer> defenders = new ArrayList<>();
        List<String> allyMoves = generateLegalMoves(defenderColor, false);

        for (String move : allyMoves) {
            if (getDestinationSquare(move) == targetSquare) {
                defenders.add(squareToIndex(move.substring(0, 2))); // Ajoute la case de départ du défenseur
            }
        }
        return defenders;
    }


    /**
     * Génére la liste des coups légaux, càd si le roi après coup n'est pas en échec
     *
     * @param color                -> couleur du joueur afin de générer les coups
     * @param onlyGenerateCaptures -> retourne seulmeent les coups non silencieux pour la Quiescence Search
     * @return la liste des cousp légaux
     */
    public List<String> generateLegalMoves(int color, boolean onlyGenerateCaptures) {
        List<String> pseudoLegalMoves = generateMoves(color);
        List<String> legalMoves = new ArrayList<>();
        //MoveState moveState = new MoveState(this);

        for (String moveToVerify : pseudoLegalMoves) {
            int to = (moveToVerify.charAt(3) - '1') * 8 + (moveToVerify.charAt(2) - 'a');
            char destinationPiece = getPieceAt(to);
            moveHistory.push(new MoveState(this));
            executeMove(moveToVerify);
            boolean kingInCheck = isKingInCheck(color);
            if (!kingInCheck/*ColorToMove == 1 ? (whiteKing & blackAttacks) == 0 : (blackKing & whiteAttacks) == 0*/) {
                if (onlyGenerateCaptures) {
                    if (destinationPiece != '.') {
                        legalMoves.add(moveToVerify);
                    }
                } else {
                    legalMoves.add(moveToVerify);
                }
            }
            //moveState.restoreState(this);
            unmakeMove();
        }
        return legalMoves;
    }

    /**
     * Retourne True si le move donné en paramètre est valide, false sinon.
     * Petit ajout de ma part, nécessite la validation de Romain :eyes:
     *
     * @param move
     * @return
     */
    public boolean isLegalMove(String move) {
        List<String> legalMoves = generateLegalMoves(ColorToMove, false);
        return legalMoves.contains(move);
    }

    /**
     * Permet de simuler un coup (moins contraignant que makeMove()
     *
     * @param move
     */
    public void executeMove(String move) {
        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        long fromBit = 1L << from;
        long toBit = 1L << to;

        // Sauvegarder l'état pour en passant
        int newEnPassantSquare = -1;
        int newEnPassantColor = 0;

        // Identifier le type de pièce qui bouge
        long piece = ColorToMove == 1 ? whitePawns : blackPawns;

        // Gérer la cible en passant pour un coup de pion sur deux cases
        if ((piece & fromBit) != 0 && Math.abs(from - to) == 16) {
            // Vérifier que le pion est sur le rang de départ (sécurité supplémentaire)
            if (ColorToMove == 1 && from / 8 == 1 || ColorToMove == -1 && from / 8 == 6) {
                newEnPassantSquare = ColorToMove == 1 ? (to - 8) : (to + 8);
                newEnPassantColor = ColorToMove;
            }
        }

        // Supprimer la pièce capturée (captures standards, hors en passant)
        if ((whitePieces() & toBit) != 0) {
            whitePawns &= ~toBit;
            whiteRooks &= ~toBit;
            whiteKnights &= ~toBit;
            whiteBishops &= ~toBit;
            whiteQueens &= ~toBit;
        } else if ((blackPieces() & toBit) != 0) {
            blackPawns &= ~toBit;
            blackRooks &= ~toBit;
            blackKnights &= ~toBit;
            blackBishops &= ~toBit;
            blackQueens &= ~toBit;
        }

        // Gérer la capture en passant
        if ((piece & fromBit) != 0 && enPassantSquare != -1 && to == enPassantSquare) {
            if (ColorToMove == 1) {
                // Blanc capture en passant : supprimer pion noir sur to-8
                blackPawns &= ~(1L << (to - 8));
            } else {
                // Noir capture en passant : supprimer pion blanc sur to+8
                whitePawns &= ~(1L << (to + 8));
            }
        }

        // Gérer les roques pour le roi blanc
        if ((whiteKing & fromBit) != 0) {
            whiteKing ^= fromBit | toBit;
            castlingRights[0] = castlingRights[1] = false;
            if (to - from == 2) { // Roque petit côté blanc
                whiteRooks ^= (1L << 7) | (1L << 5); // h1 à f1
            } else if (to - from == -2) { // Roque grand côté blanc
                whiteRooks ^= (1L << 0) | (1L << 3); // a1 à d1
            }
        }
        // Gérer les roques pour le roi noir
        else if ((blackKing & fromBit) != 0) {
            blackKing ^= fromBit | toBit;
            castlingRights[2] = castlingRights[3] = false;
            if (to - from == 2) { // Roque petit côté noir
                blackRooks ^= (1L << 63) | (1L << 61); // h8 à f8
            } else if (to - from == -2) { // Roque grand côté noir
                blackRooks ^= (1L << 56) | (1L << 59); // a8 à d8
            }
        }

        // Gérer les promotions
        if ((piece & fromBit) != 0 && (ColorToMove == 1 && to / 8 == 7 || ColorToMove == -1 && to / 8 == 0)) {
            char promotionPiece = move.length() == 5 ? move.charAt(4) : (ColorToMove == 1 ? 'Q' : 'q');
            if (ColorToMove == 1) {
                whitePawns &= ~fromBit; // Supprimer le pion
                switch (promotionPiece) {
                    case 'Q': whiteQueens |= toBit; break;
                    case 'R': whiteRooks |= toBit; break;
                    case 'B': whiteBishops |= toBit; break;
                    case 'N': whiteKnights |= toBit; break;
                }
            } else {
                blackPawns &= ~fromBit;
                switch (promotionPiece) {
                    case 'q': blackQueens |= toBit; break;
                    case 'r': blackRooks |= toBit; break;
                    case 'b': blackBishops |= toBit; break;
                    case 'n': blackKnights |= toBit; break;
                }
            }
        } else {
            // Déplacements normaux (y compris en passant pour le pion preneur)
            if ((whitePawns & fromBit) != 0) {
                whitePawns ^= fromBit ^ toBit; // Utiliser ^ séparé pour éviter erreurs
            } else if ((blackPawns & fromBit) != 0) {
                blackPawns ^= fromBit ^ toBit;
            } else if ((whiteRooks & fromBit) != 0) {
                whiteRooks ^= fromBit ^ toBit;
            } else if ((blackRooks & fromBit) != 0) {
                blackRooks ^= fromBit ^ toBit;
            } else if ((whiteKnights & fromBit) != 0) {
                whiteKnights ^= fromBit ^ toBit;
            } else if ((blackKnights & fromBit) != 0) {
                blackKnights ^= fromBit ^ toBit;
            } else if ((whiteBishops & fromBit) != 0) {
                whiteBishops ^= fromBit ^ toBit;
            } else if ((blackBishops & fromBit) != 0) {
                blackBishops ^= fromBit ^ toBit;
            } else if ((whiteQueens & fromBit) != 0) {
                whiteQueens ^= fromBit ^ toBit;
            } else if ((blackQueens & fromBit) != 0) {
                blackQueens ^= fromBit ^ toBit;
            }
        }

        // Appliquer la nouvelle cible en passant
        enPassantSquare = newEnPassantSquare;
        enPassantColor = newEnPassantColor;

        // Mettre à jour les bitboards généraux
        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing;
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;

        // Mettre à jour les attaques
        updateAttackBitboards();

        // Changer le côté à jouer
        ColorToMove = -ColorToMove;
    }

    /**
     * Dépush l'état de la queue afin de restaurer l'échiquier
     */
    public void unmakeMove() {
        if (!moveHistory.isEmpty()) {
            MoveState previousState = moveHistory.pop();

            this.whitePawns = previousState.whitePawns;
            this.whiteRooks = previousState.whiteRooks;
            this.whiteKnights = previousState.whiteKnights;
            this.whiteBishops = previousState.whiteBishops;
            this.whiteQueens = previousState.whiteQueens;
            this.whiteKing = previousState.whiteKing;
            this.blackPawns = previousState.blackPawns;
            this.blackRooks = previousState.blackRooks;
            this.blackKnights = previousState.blackKnights;
            this.blackBishops = previousState.blackBishops;
            this.blackQueens = previousState.blackQueens;
            this.blackKing = previousState.blackKing;
            this.enPassantSquare = previousState.enPassantSquare;
            this.ColorToMove = previousState.ColorToMove;
            this.castlingRights = previousState.castlingRights.clone();

//            whitePieces = whitePawns | whiteBishops | whiteKnights | whiteRooks | whiteQueens | whiteKing;
//            blackPieces = blackPieces | blackBishops | blackKnights | blackRooks | blackQueens | blackKing;
            // updatePieceBitboards();
            //updateAttackBitboards();
        }
    }

    public void executeNullMove() {
        ColorToMove = -ColorToMove;
    }

    public void unmakeNullMove() {
        ColorToMove = -ColorToMove;
    }

    public int getEnPassantColumn(int squareEP) {
        return squareEP % 8;
    }


    /**
     * Vérifie si le roi est en échec ou non
     *
     * @param color
     * @return
     */
    public boolean isKingInCheck(int color) {
        long kingPosition = (color == 1) ? whiteKing : blackKing;
        long opponentPieces = (color == 1) ? blackPieces() : whitePieces();
        if (isSlidingPieceAttacking(kingPosition, opponentPieces & (color == 1 ? blackRooks | blackQueens : whiteRooks | whiteQueens), new int[]{1, -1, 8, -8})) {
            //System.out.println("TOur");
            return true;
        }
        if (isSlidingPieceAttacking(kingPosition, opponentPieces & (color == 1 ? blackBishops | blackQueens : whiteBishops | whiteQueens), new int[]{7, -7, 9, -9})) {
            //System.out.println("bish");

            return true;
        }
        if (isKnightAttacking(kingPosition, opponentPieces & (color == 1 ? blackKnights : whiteKnights))) {
            //System.out.println("knight");

            return true;
        }
        if (isPawnAttacking(kingPosition, opponentPieces & (color == 1 ? blackPawns : whitePawns), color)) {
            //System.out.println("pawn");

            return true;
        }
        if (isKingAttacking(kingPosition, opponentPieces & (color == 1 ? blackKing : whiteKing))) {
            //System.out.println("king");

            return true;
        }
        return false;
    }

    public boolean isPawnAttacking(long king, long pawns, int color) {
        //Echec après en Passant
        //La tour met en échec quand le pion effectue en Passant

        long attacks;
        if (color == 1) {
            attacks = ((pawns & ~FILE_H /*& ~FILE_1*/) >>> 7) | ((pawns & ~FILE_A/* & ~FILE_1*/) >>> 9);
        } else {
            attacks = ((pawns & ~FILE_A/* & ~FILE_8*/) << 7) | ((pawns & ~FILE_H/* & ~FILE_8*/) << 9);
        }
        //if((enPassantSquare & pawns) && isKingNotInCheckAfterEP(king, pawns, (ColorToMove == 1)? this.blackPieces : this.whitePieces)) return false;
        return (attacks & king) != 0;
    }

    public boolean isKingNotInCheckAfterEP(long king, long pawns, long pieces) {
        return isKingInCheck(this.ColorToMove);
    }


    public boolean isKnightAttacking(long king, long knights) {
        long attacks = ((knights & ~FILE_H/* & ~FILE_87*/) << 17) |
                ((knights & ~FILE_A/* & ~FILE_87*/) << 15) |
                ((knights & ~FILE_GH/* & ~FILE_8*/) << 10) |
                ((knights & ~FILE_AB/* & ~FILE_8*/) << 6) |
                ((knights & ~FILE_A/* & ~FILE_12*/) >>> 17) |
                ((knights & ~FILE_H/* & ~FILE_12*/) >>> 15) |
                ((knights & ~FILE_AB/* & ~FILE_1*/) >>> 10) |
                ((knights & ~FILE_GH/* & ~FILE_1*/) >>> 6);
        return (attacks & king) != 0;
    }


    public boolean isSlidingPieceAttacking(long king, long slidingPieces, int[] directions) {
        for (int dir : directions) {
            long current = king;

            while (true) {

                if ((dir == 1) && (current & FILE_H) != 0) break;
                if ((dir == -1) && (current & FILE_A) != 0) break;
                if ((dir == 8) && (current & FILE_8) != 0) break;
                if ((dir == -8) && (current & FILE_1) != 0) break;
                if ((dir == 9) && (current & (FILE_H/* | FILE_8*/)) != 0) break;
                if ((dir == 7) && (current & (FILE_A/* | FILE_8*/)) != 0) break;
                if ((dir == -7) && (current & (FILE_H/* | FILE_1*/)) != 0) break;
                if ((dir == -9) && (current & (FILE_A /*| FILE_1*/)) != 0) break;

                current = (dir > 0) ? (current << dir) : (current >>> -dir);
                if (current == 0) break;

                if ((slidingPieces & current) != 0) {
                    return true;
                }

                if ((allPieces() & current) != 0) break;
            }
        }
        return false;
    }


    public boolean isKingAttacking(long king, long opponentKing) {
        long attacks = ((opponentKing & ~FILE_H) << 1) |
                ((opponentKing & ~FILE_A) >>> 1) |
                ((opponentKing & ~FILE_8) << 8) |
                ((opponentKing & ~FILE_1) >>> 8) |
                ((opponentKing & ~FILE_H /*& ~FILE_8*/) << 9) |
                ((opponentKing & ~FILE_A /*& ~FILE_8*/) << 7) |
                ((opponentKing & ~FILE_A /*& ~FILE_1*/) >>> 9) |
                ((opponentKing & ~FILE_H /*& ~FILE_1*/) >>> 7);

        return (attacks & king) != 0;
    }


    /**
     * Si le roi est en échec et mat #
     *
     * @param color
     * @return true ou false si en échec et mat
     */
    public boolean isCheckmate(int color) {
        if (isKingInCheck(color)) {
            List<String> legalMoves = generateLegalMoves(color, false);
            return legalMoves.isEmpty();
        }
        return false;
    }

    /**
     * Renvoie true si la partie est nulle.
     *
     * @return
     */
    public boolean isDraw() {
        boolean isDraw = false;
        int currentColor = ColorToMove;
        boolean kingInCheck = isKingInCheck(currentColor);
        List<String> legalMoves = generateLegalMoves(currentColor, false);
        if (!kingInCheck && legalMoves.isEmpty()) {
            isDraw = true;
        }
        long currentZobristKey = Zobrist.getKeyForBoard(this);
        repetitionTable.addPosition(currentZobristKey);
        if (repetitionTable.isTripleRepetition(currentZobristKey)) {
            isDraw = true;
        }

        if (nbMoveSinceCapture >= 50) {
            isDraw = true;
        }

        if (isInsufficientMaterial()) {
            isDraw = true;
        }
        return isDraw;
    }

    public boolean isPromotion(String move) {
        return move.length() == 5;
    }

    public boolean isCapture(String move) {
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        return getPieceAt(to) != '.';
    }

    public long getFileMask(long position, int color) {
        long mask = 0L;
        int squareIndex = Long.numberOfTrailingZeros(position);


        int file = squareIndex % 8;
        int rank = squareIndex / 8;
        int file_left = file;
        int file_right = file;
        if (file != 0) {
            file_left = file - 1;
        }
        if (file != 7) {
            file_right = file + 1;
        }

        if (color == 1) {
            for (int r = rank + 1; r < 8; r++) {
                mask |= 1L << (8 * r + file);
                mask |= 1L << (8 * r + file_left);
                mask |= 1L << (8 * r + file_right);
            }
        } else {
            for (int r = rank - 1; r >= 0; r--) {
                mask |= 1L << (8 * r + file);
                mask |= 1L << (8 * r + file_left);
                mask |= 1L << (8 * r + file_right);
            }
        }
        return mask;
    }

    public long getFileMaskForPawn(int squareIndex) {
        long mask = 0L;

        int file = squareIndex % 8;
        for (int r = 0; r < 8; r++) {
            mask |= 1L << (8 * r + file);
        }

        return mask;
    }

    /**
     * Retourne un masque de bits correspondant aux colonnes adjacentes d'une position donné
     *
     * @param squareIndex -> position d'une pièce sur l'échiquer à laquelle on veut obtenir un masque
     * @return un masque de bit sur les colonnes adjacentes.
     */
    public long getAdjacentMask(int squareIndex) {
        long mask = 0L;

        int file = squareIndex % 8;
        int file_left = 0;
        int file_right = 0;

        if (file != 0) {
            file_left = file - 1;
        }
        if (file != 7) {
            file_right = file + 1;
        }

        for (int r = 0; r < 8; r++) {
            if (file != 0) mask |= 1L << (8 * r + file_left);
            if (file != 7) mask |= 1L << (8 * r + file_right);
        }
        return mask;
    }


    public boolean canCastle(int color) {
        return color == 1 ? this.castlingRights[0] || this.castlingRights[1] : this.castlingRights[2] || this.castlingRights[3];
    }

    public long getAttackZone(String piece, int color) {
        if (color == 1) {
            switch (piece) {
                case ("Pawn"):
                    return whitePawnsAttack;
                case ("Bishop"):
                    return whiteBishopsAttack;
                case ("Rook"):
                    return whiteRooksAttack;
                case ("Queen"):
                    return whiteQueensAttack;
                case ("Knight"):
                    return whiteKnightsAttack;
                case ("King"):
                    return whiteKingAttack;
                default:
                    System.out.println("No piece selected");
                    return 0;
            }
        } else {
            switch (piece) {
                case ("Pawn"):
                    return blackPawnsAttack;
                case ("Bishop"):
                    return blackBishopsAttack;
                case ("Rook"):
                    return blackRooksAttack;
                case ("Queen"):
                    return blackQueensAttack;
                case ("Knight"):
                    return blackKnightsAttack;
                case ("King"):
                    return blackKingAttack;
                default:
                    System.out.println("No piece selected");
                    return 0;
            }
        }
    }


    /**
     * Vérifie l'état actuel du jeu : échec et mat, pat, ou match nul.
     *
     * @return Une chaîne décrivant l'état du jeu.
     */
    public String checkGameStatus() {
        int currentColor = ColorToMove;
        boolean kingInCheck = isKingInCheck(currentColor);
        List<String> legalMoves = generateLegalMoves(currentColor, false);

        if (kingInCheck && legalMoves.isEmpty()) {
            return currentColor == 1 ? "Black wins by checkmate" : "White wins by checkmate";
        }

        if (!kingInCheck && legalMoves.isEmpty()) {
            return "Draw by stalemate";
        }

        long currentZobristKey = Zobrist.getKeyForBoard(this);
        repetitionTable.addPosition(currentZobristKey);
        if (repetitionTable.isTripleRepetition(currentZobristKey)) {
            return "Draw by threefold repetition";
        }

        if (nbMoveSinceCapture >= 50) {
            return "Draw by fifty-move rule";
        }

        if (isInsufficientMaterial()) {
            return "Draw by insufficient material";
        }

        return "Game continues";
    }

    /**
     * Vérifie s'il y a un matériel insuffisant pour un échec et mat.
     *
     * @return true si le matériel est insuffisant, false sinon.
     */
    private boolean isInsufficientMaterial() {
        long allPieces = allPieces();
        int pieceCount = Long.bitCount(allPieces);

        if (pieceCount == 2 && whiteKing != 0 && blackKing != 0 &&
                whitePawns == 0 && blackPawns == 0 && whiteRooks == 0 && blackRooks == 0 &&
                whiteKnights == 0 && blackKnights == 0 && whiteBishops == 0 && blackBishops == 0 &&
                whiteQueens == 0 && blackQueens == 0) {
            return true;
        }

        if (pieceCount == 3 && whiteKing != 0 && blackKing != 0 &&
                (Long.bitCount(whiteKnights) == 1 || Long.bitCount(blackKnights) == 1) &&
                whitePawns == 0 && blackPawns == 0 && whiteRooks == 0 && blackRooks == 0 &&
                whiteBishops == 0 && blackBishops == 0 && whiteQueens == 0 && blackQueens == 0) {
            return true;
        }

        if (pieceCount == 3 && whiteKing != 0 && blackKing != 0 &&
                (Long.bitCount(whiteBishops) == 1 || Long.bitCount(blackBishops) == 1) &&
                whitePawns == 0 && blackPawns == 0 && whiteRooks == 0 && blackRooks == 0 &&
                whiteKnights == 0 && blackKnights == 0 && whiteQueens == 0 && blackQueens == 0) {
            return true;
        }

        return false;
    }

    private List<String> convertAttacksToMoves(int fromSquare, long attacks) {
        List<String> moves = new ArrayList<>();
        String from = convertNumberToSquare(fromSquare);
        while (attacks != 0) {
            int toSquare = Long.numberOfTrailingZeros(attacks);
            String to = convertNumberToSquare(toSquare);
            moves.add(from + to);
            attacks &= attacks - 1;
        }
        return moves;
    }

    /**
     * Génére les coups sans vérifier la légalité ou non
     * <p>
     * [AMELIORATION]
     * Si le roi est en double échec, ne générer que les mouvements du roi.
     * Pas d'autres possibilités.
     *
     * @param color -> couleur à jouer
     * @return la liste des coups non légaux
     */
    public List<String> generateMoves(int color) {
        MOVE_BUFFER.clear();
        if (color == 1) {

            //Si double échec pas d'autres mouvements possibles
            if (isKingDoubleCheck(whiteKing, blackPawnsAttack, blackBishopsAttack, blackRooksAttack, blackKnightsAttack, blackQueensAttack, blackKingAttack)) {
                MOVE_BUFFER.addAll(King.generateKingMoves(whiteKing, whitePieces(), blackPieces()));
                return new ArrayList<>(MOVE_BUFFER);
            }

            MOVE_BUFFER.addAll(Pawn.generatePawnMoves(whitePawns, whitePieces(), blackPieces(), true));
            MOVE_BUFFER.addAll(Rook.generateRookMoves(whiteRooks, whitePieces(), blackPieces()));

//            long rooks = whiteRooks;
//            while (rooks != 0) {
//                int square = Long.numberOfTrailingZeros(rooks);
//                long attacks = MagicBitboards.getRookAttacks(square, blackPieces | whitePieces) & ~whitePieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                rooks &= rooks - 1;
//            }
//
//            long bishops = whiteBishops;
//            while (bishops != 0) {
//                int square = Long.numberOfTrailingZeros(bishops);
//                long attacks = MagicBitboards.getBishopAttacks(square, blackPieces | whitePieces) & ~whitePieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                bishops &= bishops - 1;
//            }
//            long queens = whiteQueens;
//            while (queens != 0) {
//                int square = Long.numberOfTrailingZeros(queens);
//                long attacks = (MagicBitboards.getRookAttacks(square, blackPieces | whitePieces) |
//                        MagicBitboards.getBishopAttacks(square, blackPieces | whitePieces)) & ~whitePieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                queens &= queens - 1;
//            }


            //POUR LES MAGICS BITBOARDS
            //moves.addAll(Rook.generateRookMoves(whiteRooks,  blackPieces()));

            MOVE_BUFFER.addAll(Knight.generateKnightMoves(whiteKnights, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Bishop.generateBishopMoves(whiteBishops, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Queen.generateQueenMoves(whiteQueens, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(King.generateCastlingMoves(whiteKing, whiteRooks, blackKing, blackRooks, whitePieces(), blackPieces(), true, castlingRights, blackAttacks));
            MOVE_BUFFER.addAll(King.generateKingMoves(whiteKing, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Pawn.generateEnPassantMoves(whitePawns, blackPawns, enPassantSquare, true));
            MOVE_BUFFER.addAll(Pawn.generatePawnPromotions(whitePawns, whitePieces(), blackKing, blackPieces(), true));
        } else {

            if (isKingDoubleCheck(blackKing, whitePawnsAttack, whiteBishopsAttack, whiteRooksAttack, whiteKnightsAttack, whiteQueensAttack, whiteKingAttack)) {
                MOVE_BUFFER.addAll(King.generateKingMoves(blackKing, blackPieces(), whitePieces()));
                return new ArrayList<>(MOVE_BUFFER);
            }

            MOVE_BUFFER.addAll(Pawn.generatePawnMoves(blackPawns, blackPieces(), whitePieces(), false));
            MOVE_BUFFER.addAll(Rook.generateRookMoves(blackRooks, blackPieces(), whitePieces()));
            //MAGICS BITBOARDS

//            long rooks = blackRooks;
//            while (rooks != 0) {
//                int square = Long.numberOfTrailingZeros(rooks);
//                long attacks = MagicBitboards.getRookAttacks(square, blackPieces | whitePieces) & ~blackPieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                rooks &= rooks - 1;
//            }
//            long bishops = blackBishops;
//            while (bishops != 0) {
//                int square = Long.numberOfTrailingZeros(bishops);
//                long attacks = MagicBitboards.getBishopAttacks(square, blackPieces | whitePieces) & ~blackPieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                bishops &= bishops - 1;
//            }
//            long queens = blackQueens;
//            while (queens != 0) {
//                int square = Long.numberOfTrailingZeros(queens);
//                long attacks = (MagicBitboards.getRookAttacks(square, blackPieces | whitePieces) |
//                        MagicBitboards.getBishopAttacks(square, blackPieces | whitePieces)) & ~blackPieces;
//                MOVE_BUFFER.addAll(convertAttacksToMoves(square, attacks));
//                queens &= queens - 1;
//            }


            //moves.addAll(Rook.generateRookMoves(blackRooks, whitePieces()));
            MOVE_BUFFER.addAll(Knight.generateKnightMoves(blackKnights, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Bishop.generateBishopMoves(blackBishops, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Queen.generateQueenMoves(blackQueens, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(King.generateCastlingMoves(whiteKing, whiteRooks, blackKing, blackRooks, blackPieces(), whitePieces(), false, castlingRights, whiteAttacks));

            MOVE_BUFFER.addAll(King.generateKingMoves(blackKing, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Pawn.generateEnPassantMoves(blackPawns, whitePawns, enPassantSquare, false));
            MOVE_BUFFER.addAll(Pawn.generatePawnPromotions(blackPawns, blackPieces(), whiteKing, whitePieces(), false));
        }
        return new ArrayList<>(MOVE_BUFFER);
    }

    private long generateRookAttacksForAllPieces(long rooks, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        long allPieces = alliedPieces | enemyPieces;
        while (rooks != 0) {
            int square = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            attacks |= MagicBitboards.getRookAttacks(square, allPieces);
        }
        return attacks;
    }

    private long generateBishopAttacksForAllPieces(long bishops, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        long allPieces = alliedPieces | enemyPieces;
        while (bishops != 0) {
            int square = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            attacks |= MagicBitboards.getBishopAttacks(square, allPieces);
        }
        return attacks;
    }

    private long generateQueenAttacksForAllPieces(long queens, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        long allPieces = alliedPieces | enemyPieces;
        while (queens != 0) {
            int square = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            attacks |= MagicBitboards.getRookAttacks(square, allPieces) |
                    MagicBitboards.getBishopAttacks(square, allPieces);
        }
        return attacks;
    }

//    public List<String> generateMoves(int color) {
//        List<String> moves = new ArrayList<>();
//
//        // Obtenir les bitboards nécessaires
//        long ownPieces = (color == 1) ? whitePieces() : blackPieces();
//        long opponentPieces = (color == 1) ? blackPieces() : whitePieces();
//        long occupancy = ownPieces | opponentPieces;
//
//        // Double échec : seuls les mouvements du roi sont possibles
//        if ((color == 1 && isKingDoubleCheck(whiteKing, blackPawnsAttack, blackBishopsAttack, blackRooksAttack, blackKnightsAttack, blackQueensAttack, blackKingAttack)) ||
//                (color == 0 && isKingDoubleCheck(blackKing, whitePawnsAttack, whiteBishopsAttack, whiteRooksAttack, whiteKnightsAttack, whiteQueensAttack, whiteKingAttack))) {
//            if (color == 1) {
//                moves.addAll(King.generateKingMoves(whiteKing, ownPieces, opponentPieces));
//            } else {
//                moves.addAll(King.generateKingMoves(blackKing, ownPieces, opponentPieces));
//            }
//            return moves;
//        }
//
//        // Pions
//        if (color == 1) {
//            moves.addAll(Pawn.generatePawnMoves(whitePawns, ownPieces, opponentPieces, true));
//            moves.addAll(Pawn.generateEnPassantMoves(whitePawns, blackPawns, enPassantSquare, true));
//            moves.addAll(Pawn.generatePawnPromotions(whitePawns, ownPieces, blackKing, opponentPieces, true));
//        } else {
//            moves.addAll(Pawn.generatePawnMoves(blackPawns, ownPieces, opponentPieces, false));
//            moves.addAll(Pawn.generateEnPassantMoves(blackPawns, whitePawns, enPassantSquare, false));
//            moves.addAll(Pawn.generatePawnPromotions(blackPawns, ownPieces, whiteKing, opponentPieces, false));
//        }
//
//        // Tours
//        if (color == 1) {
//            moves.addAll(Rook.generateRookMoves(whiteRooks, occupancy, ownPieces)); // Avec magic bitboards
//        } else {
//            moves.addAll(Rook.generateRookMoves(blackRooks, occupancy, ownPieces)); // Avec magic bitboards
//        }
//
//        // Fous
//        if (color == 1) {
//            moves.addAll(Bishop.generateBishopMoves(whiteBishops, occupancy, ownPieces)); // Avec magic bitboards
//        } else {
//            moves.addAll(Bishop.generateBishopMoves(blackBishops, occupancy, ownPieces)); // Avec magic bitboards
//        }
//
//        // Cavaliers
//        if (color == 1) {
//            moves.addAll(Knight.generateKnightMoves(whiteKnights, ownPieces, opponentPieces));
//        } else {
//            moves.addAll(Knight.generateKnightMoves(blackKnights, ownPieces, opponentPieces));
//        }
//
//        // Reines
//        if (color == 1) {
//            moves.addAll(Queen.generateQueenMoves(whiteQueens, ownPieces, opponentPieces));
//        } else {
//            moves.addAll(Queen.generateQueenMoves(blackQueens, ownPieces, opponentPieces));
//        }
//
//        // Roques
//        if (color == 1) {
//            moves.addAll(King.generateCastlingMoves(whiteKing, whiteRooks, blackKing, blackRooks, ownPieces, opponentPieces, true, castlingRights, blackAttacks));
//        } else {
//            moves.addAll(King.generateCastlingMoves(blackKing, blackRooks, whiteKing, whiteRooks, ownPieces, opponentPieces, false, castlingRights, whiteAttacks));
//        }
//
//        // Roi
//        if (color == 1) {
//            moves.addAll(King.generateKingMoves(whiteKing, ownPieces, opponentPieces));
//        } else {
//            moves.addAll(King.generateKingMoves(blackKing, ownPieces, opponentPieces));
//        }
//
//        return moves;
//    }


    public List<String> generateMovesCaptureOnlyGameMode(int color) {
        List<String> moves = new ArrayList<>();
        if (color == 1) {

            //Si double échec pas d'autres mouvements possibles
//            if (isKingDoubleCheck(whiteKing, blackPawnsAttack, blackBishopsAttack, blackRooksAttack, blackKnightsAttack, blackQueensAttack, blackKingAttack)) {
//                moves.addAll(King.generateKingMoves(whiteKing, whitePieces(), blackPieces()));
//                return moves;
//            }

            moves.addAll(Pawn.generatePawnMoves(whitePawns, whitePieces(), blackPieces(), true));
            moves.addAll(Rook.generateRookMoves(whiteRooks, whitePieces(), blackPieces()));

            //POUR LES MAGICS BITBOARDS
            //moves.addAll(Rook.generateRookMoves(whiteRooks,  blackPieces()));

            moves.addAll(Knight.generateKnightMoves(whiteKnights, whitePieces(), blackPieces()));
            moves.addAll(Bishop.generateBishopMoves(whiteBishops, whitePieces(), blackPieces()));
            moves.addAll(Queen.generateQueenMoves(whiteQueens, whitePieces(), blackPieces()));
            //moves.addAll(King.generateCastlingMoves(whiteKing, whiteRooks, blackKing, blackRooks, whitePieces(), blackPieces(), true, castlingRights, blackAttacks));
            moves.addAll(King.generateKingMoves(whiteKing, whitePieces(), blackPieces()));
            moves.addAll(Pawn.generateEnPassantMoves(whitePawns, blackPawns, enPassantSquare, true));
            moves.addAll(Pawn.generatePawnPromotionsCaptureOnlyGameMode(whitePawns, whitePieces(), blackKing, blackPieces(), true));
        } else {

//            if (isKingDoubleCheck(blackKing, whitePawnsAttack, whiteBishopsAttack, whiteRooksAttack, whiteKnightsAttack, whiteQueensAttack, whiteKingAttack)) {
//                moves.addAll(King.generateKingMoves(blackKing, blackPieces(), whitePieces()));
//                return moves;
//            }

            moves.addAll(Pawn.generatePawnMoves(blackPawns, blackPieces(), whitePieces(), false));
            moves.addAll(Rook.generateRookMoves(blackRooks, blackPieces(), whitePieces()));
            //MAGICS BITBOARDS
            //moves.addAll(Rook.generateRookMoves(blackRooks, whitePieces()));
            moves.addAll(Knight.generateKnightMoves(blackKnights, blackPieces(), whitePieces()));
            moves.addAll(Bishop.generateBishopMoves(blackBishops, blackPieces(), whitePieces()));
            moves.addAll(Queen.generateQueenMoves(blackQueens, blackPieces(), whitePieces()));
            //moves.addAll(King.generateCastlingMoves(whiteKing, whiteRooks, blackKing, blackRooks, blackPieces(), whitePieces(), false, castlingRights, whiteAttacks));

            moves.addAll(King.generateKingMoves(blackKing, blackPieces(), whitePieces()));
            moves.addAll(Pawn.generateEnPassantMoves(blackPawns, whitePawns, enPassantSquare, false));
            moves.addAll(Pawn.generatePawnPromotionsCaptureOnlyGameMode(blackPawns, blackPieces(), whiteKing, whitePieces(), false));
        }
        return moves;
    }

    private long generateKnightAttacks(long knightPosition, long alliedPiece) {
        long attacks = 0L;

        // Masques des mouvements possibles
        attacks |= (knightPosition & ~FILE_A) << 15;// & ~alliedPiece; // 2 vers le haut, 1 à gauche
        attacks |= (knightPosition & ~FILE_H) << 17;// & ~alliedPiece; // 2 vers le haut, 1 à droite
        attacks |= (knightPosition & ~FILE_AB) << 6;// & ~alliedPiece; // 1 vers le haut, 2 à gauche
        attacks |= (knightPosition & ~FILE_GH) << 10;// & ~alliedPiece; // 1 vers le haut, 2 à droite
        attacks |= (knightPosition & ~FILE_H) >>> 15;// & ~alliedPiece; // 2 vers le bas, 1 à gauche
        attacks |= (knightPosition & ~FILE_A) >>> 17;// & ~alliedPiece; // 2 vers le bas, 1 à droite
        attacks |= (knightPosition & ~FILE_GH) >>> 6;// & ~alliedPiece; // 1 vers le bas, 2 à gauche
        attacks |= (knightPosition & ~FILE_AB) >>> 10;// & ~alliedPiece; // 1 vers le bas, 2 à droite

        return attacks;
    }


    private long generateDiagonalAttacks(long position, long alliedPieces, long enemyPieces, long enemyKing) {
        long attacks = 0L;
        long current;

        // Haut-droite
        current = position;
        while ((current = (current & ~FILE_H) << 9) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Haut-gauche
        current = position;
        while ((current = (current & ~FILE_A) << 7) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Bas-droite
        current = position;
        while ((current = (current & ~FILE_H) >>> 7) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Bas-gauche
        current = position;
        while ((current = (current & ~FILE_A) >>> 9) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        return attacks;
    }


    private long generateStraightAttacks(long position, long alliedPieces, long enemyPieces, long enemyKing) {
        long attacks = 0L;
        long current;

        // Haut
        current = position;
        while ((current = ((current & ~FILE_8) << 8)) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Bas
        current = position;
        while ((current = ((current & ~FILE_1) >>> 8)) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Droite
        current = position;
        while ((current = (current & ~FILE_H) << 1) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        // Gauche
        current = position;
        while ((current = (current & ~FILE_A) >>> 1) != 0) {
            //if ((current & alliedPieces) != 0) break;
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                if ((current & enemyKing) == 0) {
                    break;
                }
            }
        }

        return attacks;
    }


    private long generateKingAttacks(long kingPosition, long alliedPiece) {
        long attacks = 0L;

        attacks |= ((kingPosition & ~FILE_A) >>> 1);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_H) >>> 7);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_1) >>> 8);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_A) >>> 9);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_H) << 1);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_A) << 7);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_8) << 8);// & ~alliedPiece;
        attacks |= ((kingPosition & ~FILE_H) << 9);// & ~alliedPiece;

        return attacks;
    }

    public long generatePawnAttacksAllied(long pawnPosition, int color) {
        long attacks = 0L;

        if (color == 1) {
            attacks |= ((pawnPosition & ~FILE_A) << 7);
            attacks |= ((pawnPosition & ~FILE_H) << 9);
        } else {
            attacks |= ((pawnPosition & ~FILE_H) >>> 7);
            attacks |= ((pawnPosition & ~FILE_A) >>> 9);
        }
        return attacks;
    }

    public long generatePawnAttacks(long pawnPosition, long alliedPiece, int color) {
        long attacks = 0L;

        if (color == 1) {
            attacks |= ((pawnPosition & ~FILE_A) << 7);// & ~alliedPiece;
            attacks |= ((pawnPosition & ~FILE_H) << 9);// & ~alliedPiece;

//            if (enPassantSquare != -1 && pawnPosition << 7 == enPassantSquare) {
//                attacks |= ((pawnPosition & ~FILE_A) << 7) & ~alliedPiece;
//            }
//            if (enPassantSquare != -1 && pawnPosition << 9 == enPassantSquare) {
//                attacks |= ((pawnPosition & ~FILE_H) << 9) & ~alliedPiece;
//            }
        } else {
            attacks |= ((pawnPosition & ~FILE_H) >>> 7);// & ~alliedPiece;
            attacks |= ((pawnPosition & ~FILE_A) >>> 9);// & ~alliedPiece;

//            if (enPassantSquare != -1 && pawnPosition >>> 7 == enPassantSquare) {
//                attacks |= ((pawnPosition & ~FILE_H) >>> 7) & ~alliedPiece;
//            }
//            if (enPassantSquare != -1 && pawnPosition >>> 9 == enPassantSquare) {
//                attacks |= ((pawnPosition & ~FILE_A) >>> 9) & ~alliedPiece;
//            }
        }
        return attacks;
    }

    private long generateQueenAttacks(long position, long alliedPieces, long enemyPieces, long enemyKing) {
        return generateDiagonalAttacks(position, alliedPieces, enemyPieces, enemyKing) | generateStraightAttacks(position, alliedPieces, enemyPieces, enemyKing);
    }

    public void updateAttackBitboards() {
        this.whitePawnsDefense = generatePawnAttacks(whitePawns, this.whitePieces(), 1);
        this.blackPawnsDefense = generatePawnAttacks(blackPawns, this.blackPieces(), -1);

//        this.whitePawnsAttacksAllied = generatePawnAttacksAllied(whitePawns, 1);
//        this.blackPawnsAttacksAllied = generatePawnAttacksAllied(blackPawns, -1);

        this.whiteRooksDefense = generateStraightAttacksForAllPieces(whiteRooks, this.whitePieces(), this.blackPieces(), this.blackKing);
        this.blackRooksDefense = generateStraightAttacksForAllPieces(blackRooks, this.blackPieces(), this.whitePieces(), this.whiteKing);

        this.whiteBishopsDefense = generateDiagonalAttacksForAllPieces(whiteBishops, this.whitePieces(), this.blackPieces(), this.blackKing);
        this.blackBishopsDefense = generateDiagonalAttacksForAllPieces(blackBishops, this.blackPieces(), this.whitePieces(), this.whiteKing);

        this.whiteQueensDefense = generateQueenAttacksForAllPieces(whiteQueens, this.whitePieces(), this.blackPieces(), this.blackKing);
        this.blackQueensDefense = generateQueenAttacksForAllPieces(blackQueens, this.blackPieces(), this.whitePieces(), this.whiteKing);

        this.whiteKnightsDefense = generateKnightAttacksForAllPieces(whiteKnights, this.whitePieces());
        this.blackKnightsDefense = generateKnightAttacksForAllPieces(blackKnights, this.blackPieces());

        this.whiteKingDefense = generateKingAttacks(whiteKing, this.whitePieces());
        this.blackKingDefense = generateKingAttacks(blackKing, this.blackPieces());

        this.whiteDefense = whitePawnsDefense | whiteBishopsDefense | whiteKnightsDefense | whiteQueensDefense | whiteRooksDefense | whiteKingDefense;
        this.blackDefense = blackPawnsDefense | blackBishopsDefense | blackKnightsDefense | blackRooksDefense | blackQueensDefense | blackKingDefense;

        this.whitePawnsAttack = this.whitePawnsDefense & ~this.whitePieces;
        this.blackPawnsAttack = this.blackPawnsDefense & ~this.blackPieces;

        this.whiteRooksAttack = this.whiteRooksDefense & ~this.whitePieces;
        this.blackRooksAttack = this.blackRooksDefense & ~this.blackPieces;

        this.whiteBishopsAttack = this.whiteBishopsDefense & ~this.whitePieces;
        this.blackBishopsAttack = this.blackBishopsDefense & ~this.blackPieces;

        this.whiteQueensAttack = this.whiteQueensDefense & ~this.whitePieces;
        this.blackQueensAttack = this.blackQueensDefense & ~this.blackPieces;

        this.whiteKnightsAttack = this.whiteKnightsDefense & ~this.whitePieces;
        this.blackKnightsAttack = this.blackKnightsDefense & ~this.blackPieces;

        this.whiteKingAttack = this.whiteKingDefense & ~this.whitePieces;
        this.blackKingAttack = this.blackKingDefense & ~this.blackPieces;

        this.whiteAttacks = this.whiteDefense & ~this.whitePieces;
        this.blackAttacks = this.blackDefense & ~this.blackPieces;

    }

    private long generateStraightAttacksForAllPieces(long pieces, long alliedPieces, long enemyPieces, long enemyKing) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateStraightAttacks(piece, alliedPieces, enemyPieces, enemyKing);
        }
        return attacks;
    }

    private long generateDiagonalAttacksForAllPieces(long pieces, long alliedPieces, long enemyPieces, long enemyKing) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateDiagonalAttacks(piece, alliedPieces, enemyPieces, enemyKing);
        }
        return attacks;
    }

    private long generateQueenAttacksForAllPieces(long pieces, long alliedPieces, long enemyPieces, long enemyKing) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateQueenAttacks(piece, alliedPieces, enemyPieces, enemyKing);
        }
        return attacks;
    }

    private long generateKnightAttacksForAllPieces(long pieces, long alliedPieces) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateKnightAttacks(piece, alliedPieces);
        }
        return attacks;
    }


    public long generatePawnDefense(long pawnPosition, int color) {
        long attacks = 0L;

        if (color == 1) {
            attacks |= ((pawnPosition & ~FILE_A) << 7);
            attacks |= ((pawnPosition & ~FILE_H) << 9);
        } else {
            attacks |= ((pawnPosition & ~FILE_H) >>> 7);
            attacks |= ((pawnPosition & ~FILE_A) >>> 9);

        }
        return attacks;
    }

    private long generateStraightDefense(long position, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        long current;

        // Haut
        current = position;
        while ((current = ((current & ~FILE_8) << 8)) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Bas
        current = position;
        while ((current = ((current & ~FILE_1) >>> 8)) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Droite
        current = position;
        while ((current = (current & ~FILE_H) << 1) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Gauche
        current = position;
        while ((current = (current & ~FILE_A) >>> 1) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;
            }
        }

        return attacks;
    }

    private long generateDiagonalDefense(long position, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        long current;

        // Haut-droite
        current = position;
        while ((current = (current & ~FILE_H) << 9) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Haut-gauche
        current = position;
        while ((current = (current & ~FILE_A) << 7) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Bas-droite
        current = position;
        while ((current = (current & ~FILE_H) >>> 7) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        // Bas-gauche
        current = position;
        while ((current = (current & ~FILE_A) >>> 9) != 0) {
            attacks |= current;
            if ((current & (enemyPieces | alliedPieces)) != 0) {
                break;

            }
        }

        return attacks;
    }

    private long generateQueenDefense(long position, long alliedPieces, long enemyPieces) {
        return generateDiagonalDefense(position, alliedPieces, enemyPieces) | generateStraightDefense(position, alliedPieces, enemyPieces);
    }

    private long generateKnightDefense(long knightPosition) {
        long attacks = 0L;

        // Masques des mouvements possibles
        attacks |= (knightPosition << 15) & ~FILE_H & ~FILE_87; // 2 vers le haut, 1 à gauche
        attacks |= (knightPosition << 17) & ~FILE_A & ~FILE_87; // 2 vers le haut, 1 à droite
        attacks |= (knightPosition << 6) & ~FILE_GH & ~FILE_8; // 1 vers le haut, 2 à gauche
        attacks |= (knightPosition << 10) & ~FILE_AB & ~FILE_8; // 1 vers le haut, 2 à droite
        attacks |= (knightPosition >> 15) & ~FILE_A & ~FILE_12; // 2 vers le bas, 1 à gauche
        attacks |= (knightPosition >> 17) & ~FILE_H & ~FILE_12; // 2 vers le bas, 1 à droite
        attacks |= (knightPosition >> 6) & ~FILE_AB & ~FILE_1; // 1 vers le bas, 2 à gauche
        attacks |= (knightPosition >> 10) & ~FILE_GH & ~FILE_1; // 1 vers le bas, 2 à droite

        return attacks;
    }

    private long generateStraightDefenseForAllPieces(long pieces, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateStraightDefense(piece, alliedPieces, enemyPieces);
        }
        return attacks;
    }

    private long generateDiagonalDefenseForAllPieces(long pieces, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateDiagonalDefense(piece, alliedPieces, enemyPieces);
        }
        return attacks;
    }

    private long generateQueenDefenseForAllPieces(long pieces, long alliedPieces, long enemyPieces) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateQueenDefense(piece, alliedPieces, enemyPieces);
        }
        return attacks;
    }

    private long generateKnightDefenseForAllPieces(long pieces) {
        long attacks = 0L;
        while (pieces != 0) {
            long piece = Long.lowestOneBit(pieces);
            pieces &= pieces - 1;
            attacks |= generateKnightDefense(piece);
        }
        return attacks;
    }

    public void updateDefenseBitboards() {
        this.whitePawnsDefense = generatePawnDefense(whitePawns, 1);
        this.blackPawnsDefense = generatePawnDefense(whitePawns, -1);
        this.whiteRooksDefense = generateStraightDefenseForAllPieces(whiteRooks, this.whitePieces(), this.blackPieces());
        this.whiteRooksDefense = generateStraightDefenseForAllPieces(blackRooks, this.blackPieces(), this.whitePieces());
        this.whiteBishopsDefense = generateDiagonalDefenseForAllPieces(whiteBishops, this.whitePieces(), this.blackPieces());
        this.whiteBishopsDefense = generateDiagonalDefenseForAllPieces(blackBishops, this.blackPieces(), this.whitePieces());
        this.whiteQueensDefense = generateQueenDefenseForAllPieces(whiteQueens, this.whitePieces(), this.blackPieces());
        this.blackQueensDefense = generateQueenDefenseForAllPieces(blackQueens, this.blackPieces(), this.whitePieces());
        this.whiteKnightsDefense = generateKnightDefenseForAllPieces(whiteKnights);
        this.blackKnightsDefense = generateKnightDefenseForAllPieces(blackKnights);

        this.whiteDefense = whitePawnsDefense | whiteRooksDefense | whiteKnightsDefense | whiteBishopsDefense | whiteQueensDefense;
        this.blackDefense = blackPawnsDefense | blackRooksDefense | blackKnightsDefense | blackBishopsDefense | blackQueensDefense;
    }

    public long getZobristKey() {
        return Zobrist.getKeyForBoard(this);
    }

    public boolean hasCastlingRight(int index) {
        return castlingRights[index];
    }


    //échec
    public boolean isKingCHeck(long king, long attacks) {
        return (king & attacks) > 0;
    }

    public static boolean isCaseAttacked(int index, long enemyAttacks) {
        return ((0b0000000000000000000000000000000000000000000000000000000000000001L << index) & enemyAttacks) > 1;
    }

    public int squareToIndex(String square) {
        char file = square.charAt(0);
        char rank = square.charAt(1);

        int fileIndex = file - 'a';
        int rankIndex = rank - '1';

        return rankIndex * 8 + fileIndex;
    }

    /**
     * Nombre de mouvement par pièce
     *
     * @param index -> indexe de la pièce
     * @return nb de mouvement
     */
    public int numberOfMoves(int index) {
        char piece = getPieceAt(index);
        switch (piece) {
            case 'P':
                return Long.bitCount(this.whitePawnsAttack);
            case 'p':
                return Long.bitCount(this.blackPawnsAttack);
            case 'N':
                return Long.bitCount(this.whiteKnightsAttack);
            case 'n':
                return Long.bitCount(this.blackKnightsAttack);
            case 'R':
                return Long.bitCount(this.whiteRooksAttack);
            case 'r':
                return Long.bitCount(this.blackRooksAttack);
            case 'B':
                return Long.bitCount(this.whiteBishopsAttack);
            case 'b':
                return Long.bitCount(this.blackBishopsAttack);
            case 'Q':
                return Long.bitCount(this.whiteQueensAttack);
            case 'q':
                return Long.bitCount(this.blackQueensAttack);
            case 'K':
                return Long.bitCount(this.whiteKingAttack);
            case 'k':
                return Long.bitCount(this.blackKingAttack);
            default:
                return 0;
        }
    }

    /**
     * Regarde si le roi est en double échec ou plus
     *
     * @param king          -> position du roi
     * @param pawnAttacks   -> matrice d'attaque du pion
     * @param bishopAttacks -> matrice d'attaque du fou
     * @param rookAttacks   -> matrice d'attaque de la tour
     * @param knightAttacks -> matrice d'attaque de cavalier
     * @param queenAttacks  -> matrice d'attaque de la reine
     * @param kingAttacks   -> matrice d'attaque du roi
     * @return true si double échec ou plus
     */
    public boolean isKingDoubleCheck(long king, long pawnAttacks, long bishopAttacks, long rookAttacks, long knightAttacks, long queenAttacks, long kingAttacks) {
        int count = 0;
        if ((king & pawnAttacks) > 0) count++;
        if ((king & bishopAttacks) > 0) count++;
        if ((king & rookAttacks) > 0) count++;
        if ((king & knightAttacks) > 0) count++;
        if ((king & queenAttacks) > 0) count++;
        if ((king & kingAttacks) > 0) count++;
        return count >= 2;
    }

    // Fonction pour vérifier la présence d'une pièce à une position donnée
    private boolean isPieceAt(int position) {
        long bit = 1L << position;
        return (whitePieces() & bit) != 0 || (blackPieces() & bit) != 0;
    }

    public long whitePieces() {
        return whitePawns | whiteRooks | whiteKnights | whiteBishops | whiteQueens | whiteKing;
    }

    public long blackPieces() {
        return blackPawns | blackRooks | blackKnights | blackBishops | blackQueens | blackKing;
    }

    public long whiteAttacks() {
        return whiteAttacks;
    }

    public long blackAttacks() {
        return blackAttacks;
    }

    public long whiteDefense() {
        return whiteDefense;
    }

    public long blackDefense() {
        return blackDefense;
    }


    //Unsigned long because java doesn't implement that, else I can use BigInteger but no thanks...
    public long uWhitePieces() {
        return whitePieces;
    }

    public long uBlackPieces() {
        return blackPieces;
    }


    public long allPieces() {
        return whitePieces() | blackPieces();
    }


    // Affiche l'échiquier pour débuguer
    public void printBoard() {
        System.out.println();
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int squareIndex = rank * 8 + file;
                char piece = getPieceAt(squareIndex);
                System.out.print(piece + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public void printBitboard(long bitboard) {
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                long square = 1L << (rank * 8 + file);
                if ((bitboard & square) != 0) {
                    System.out.print("1 ");
                } else {
                    System.out.print(". ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }


    // Retourne le caractère correspondant à une pièce
    public char getPieceAt(int squareIndex) {
        long bit = 1L << squareIndex;
        if ((whitePawns & bit) != 0) return 'P';
        if ((whiteRooks & bit) != 0) return 'R';
        if ((whiteKnights & bit) != 0) return 'N';
        if ((whiteBishops & bit) != 0) return 'B';
        if ((whiteQueens & bit) != 0) return 'Q';
        if ((whiteKing & bit) != 0) return 'K';
        if ((blackPawns & bit) != 0) return 'p';
        if ((blackRooks & bit) != 0) return 'r';
        if ((blackKnights & bit) != 0) return 'n';
        if ((blackBishops & bit) != 0) return 'b';
        if ((blackQueens & bit) != 0) return 'q';
        if ((blackKing & bit) != 0) return 'k';
        return '.';
    }

    public void startClock() {
        clock.start();
    }

    public void stopClock() {
        clock.stop();
    }

    public void switchTurn() {
        clock.switchTurn();
    }

    public long getWhiteTime() {
        return clock.getWhiteTime();
    }

    public long getBlackTime() {
        return clock.getBlackTime();
    }

    public boolean isTimeUp() {
        return clock.isTimeUp();
    }


    public boolean isCastlingMove(String move) {
        return move.equals("e1g1") || move.equals("e1c1") || move.equals("e8g8") || move.equals("e8c8");
    }

    public boolean isPromotionMove(String move) {
        return move.length() == 5 && (move.charAt(4) == 'q' || move.charAt(4) == 'r' || move.charAt(4) == 'b' || move.charAt(4) == 'n');
    }


    public boolean isEnPassantMove(String move) {
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        return getPieceAt(to) != '.' & to == enPassantSquare;
    }

    public boolean haveBishopPair(int color) {
        return color == 1 ? Long.bitCount(whiteBishops) >= 2 : Long.bitCount(blackBishops) >= 2;
    }

    public boolean haveRookPair(int color) {
        return color == 1 ? Long.bitCount(whiteRooks) >= 2 : Long.bitCount(blackRooks) >= 2;
    }

    public boolean haveKnightPair(int color) {
        return color == 1 ? Long.bitCount(whiteKnights) >= 2 : Long.bitCount(blackKnights) >= 2;
    }

    //____________________________________________________________________________________________________________________
    //Nouveau mode de jeu

    public List<String> generateLegalMovesCaptureOnlyGameMode(int color, boolean onlyGenerateCaptures) {
        List<String> pseudoLegalMoves = generateMovesCaptureOnlyGameMode(color);
        List<String> legalCaptureMoves = new ArrayList<>();
        List<String> legalMoves = new ArrayList<>();
        for (String moveToVerify : pseudoLegalMoves) {
            int to = (moveToVerify.charAt(3) - '1') * 8 + (moveToVerify.charAt(2) - 'a');
            char destinationPiece = getPieceAt(to);
            moveHistory.push(new MoveState(this));
            executeMoveCaptureOnlyGameMode(moveToVerify);
            if (onlyGenerateCaptures) {
                if (destinationPiece != '.') {
                    legalCaptureMoves.add(moveToVerify);
                }
                legalMoves.add(moveToVerify);
            } else {
                legalMoves.add(moveToVerify);
            }
            unmakeMoveCaptureOnlyGameMode();
        }
        return legalCaptureMoves.isEmpty() ? legalMoves : legalCaptureMoves;
    }


    /**
     * Permet de simuler un coup (moins contraignant que makeMove()
     *
     * @param move
     */
    public void executeMoveCaptureOnlyGameMode(String move) {

        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        long fromBit = 1L << from;
        long toBit = 1L << to;

        if ((whitePieces() & toBit) != 0) {
            whitePawns &= ~toBit;
            whiteRooks &= ~toBit;
            whiteKnights &= ~toBit;
            whiteBishops &= ~toBit;
            whiteQueens &= ~toBit;
        } else if ((blackPieces() & toBit) != 0) {
            blackPawns &= ~toBit;
            blackRooks &= ~toBit;
            blackKnights &= ~toBit;
            blackBishops &= ~toBit;
            blackQueens &= ~toBit;
        }

        if ((whitePawns & fromBit) != 0) {
            whitePawns ^= fromBit | toBit;
        } else if ((blackPawns & fromBit) != 0) {
            blackPawns ^= fromBit | toBit;
        } else if ((whiteRooks & fromBit) != 0) {
            whiteRooks ^= fromBit | toBit;
        } else if ((blackRooks & fromBit) != 0) {
            blackRooks ^= fromBit | toBit;
        } else if ((whiteKnights & fromBit) != 0) {
            whiteKnights ^= fromBit | toBit;
        } else if ((blackKnights & fromBit) != 0) {
            blackKnights ^= fromBit | toBit;
        } else if ((whiteBishops & fromBit) != 0) {
            whiteBishops ^= fromBit | toBit;
        } else if ((blackBishops & fromBit) != 0) {
            blackBishops ^= fromBit | toBit;
        } else if ((whiteQueens & fromBit) != 0) {
            whiteQueens ^= fromBit | toBit;
        } else if ((blackQueens & fromBit) != 0) {
            blackQueens ^= fromBit | toBit;
        } else if ((whiteKing & fromBit) != 0) {
            whiteKing ^= fromBit | toBit;
        } else if ((blackKing & fromBit) != 0) {
            blackKing ^= fromBit | toBit;
        }

        if (Math.abs(from - to) == 16 && ((whitePawns & toBit) != 0 || (blackPawns & toBit) != 0)) {
            enPassantSquare = (from + to) / 2;
        } else {
            enPassantSquare = -1;
        }
        //zobrist.updateZobrist(move, this);
        updateAttackBitboards();
        //updatePieceBitboards();

        ColorToMove = -ColorToMove;
    }

    /**
     * Dépush l'état de la queue afin de restaurer l'échiquier
     */
    public void unmakeMoveCaptureOnlyGameMode() {
        if (!moveHistory.isEmpty()) {
            MoveState previousState = moveHistory.pop();

            this.whitePawns = previousState.whitePawns;
            this.whiteRooks = previousState.whiteRooks;
            this.whiteKnights = previousState.whiteKnights;
            this.whiteBishops = previousState.whiteBishops;
            this.whiteQueens = previousState.whiteQueens;
            this.whiteKing = previousState.whiteKing;
            this.blackPawns = previousState.blackPawns;
            this.blackRooks = previousState.blackRooks;
            this.blackKnights = previousState.blackKnights;
            this.blackBishops = previousState.blackBishops;
            this.blackQueens = previousState.blackQueens;
            this.blackKing = previousState.blackKing;
            this.enPassantSquare = previousState.enPassantSquare;
            this.ColorToMove = previousState.ColorToMove;
            this.castlingRights = previousState.castlingRights.clone();
            //updatePieceBitboards();
        }
    }

    public void makeMoveCaptureOnlyGameMode(String move) {
        List<String> legalMoves = generateLegalMovesCaptureOnlyGameMode(ColorToMove, true);

        if (!legalMoves.contains(move)) {
            throw new IllegalArgumentException("Mouvement illégal: " + move);
        }

        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        long fromBit = 1L << from;
        long toBit = 1L << to;

        int pieceType = from;

        if ((ColorToMove == 1 && (whitePieces() & fromBit) == 0) ||
                (ColorToMove == -1 && (blackPieces() & fromBit) == 0)) {
            throw new IllegalArgumentException("C'est au tour des " + (ColorToMove == 1 ? "Blancs" : "Noirs"));
        }

        if (nbMoveSinceCapture + 1 == 51)
            throw new IllegalArgumentException("Plus de 50 coups depuis la dernière capture.");

        // zobrist.updateZobrist(move, this);
        if (getPieceAt(to) == '.') nbMoveSinceCapture++;
        else nbMoveSinceCapture = 0;

        long piece = ColorToMove == 1 ? whitePawns : blackPawns;
        if ((piece & fromBit) != 0) {
            if (Math.abs(from - to) == 16) {
                enPassantSquare = ColorToMove == 1 ? (to - 8) : (to + 8);
                enPassantColor = ColorToMove;
            }
        }

        if ((ColorToMove == 1 && (whitePieces() & toBit) != 0) ||
                (ColorToMove == -1 && (blackPieces() & toBit) != 0)) {
            throw new IllegalArgumentException("Vous ne pouvez pas capturer une pièce alliée.");
        }

        if ((whitePieces() & toBit) != 0) {
            whitePawns &= ~toBit;
            whiteRooks &= ~toBit;
            whiteKnights &= ~toBit;
            whiteBishops &= ~toBit;
            whiteQueens &= ~toBit;
            whiteKing &= ~toBit;
        } else if ((blackPieces() & toBit) != 0) {
            blackPawns &= ~toBit;
            blackRooks &= ~toBit;
            blackKnights &= ~toBit;
            blackBishops &= ~toBit;
            blackQueens &= ~toBit;
            blackKing &= ~toBit;
        }

        if ((whiteKing & fromBit) != 0) {
            whiteKing ^= fromBit | toBit;
            castlingRights[0] = castlingRights[1] = false;

            // Déplacement des tours pour un roque blanc
            if (to - from == 2) {
                whiteRooks ^= (1L << 7) | (1L << 5);
            } else if (to - from == -2) {
                whiteRooks ^= (1L << 3) | (1L << 0);
            }
        } else if ((blackKing & fromBit) != 0) {
            blackKing ^= fromBit | toBit;
            castlingRights[2] = castlingRights[3] = false;

            // Déplacement des tours pour un roque noir
            if (to - from == 2) {
                blackRooks ^= (1L << 63) | (1L << 61);
            } else if (to - from == -2) {
                blackRooks ^= (1L << 56) | (1L << 59);
            }
        }

        // Capture en passant
        if (enPassantSquare != -1 && to == enPassantSquare) {
            if (ColorToMove == 1) {
                blackPawns &= ~(1L << (to - 8));
            } else {
                whitePawns &= ~(1L << (to + 8));
            }
            enPassantSquare = -1;
        }

        // Promotion de pions
        if ((ColorToMove == 1 && (to / 8 == 7) && (piece & fromBit) != 0) ||
                (ColorToMove == -1 && (to / 8 == 0) && (piece & fromBit) != 0)) {
            char promotionPiece = move.length() == 5 ? move.charAt(4) : ColorToMove == 1 ? 'Q' : 'q'; // Par défaut à Reine si non spécifié

            if (ColorToMove == 1) {
                whitePawns ^= fromBit;
                switch (promotionPiece) {
                    case 'Q':
                        whiteQueens |= toBit;
                        break;
                    case 'R':
                        whiteRooks |= toBit;
                        break;
                    case 'B':
                        whiteBishops |= toBit;
                        break;
                    case 'N':
                        whiteKnights |= toBit;
                        break;
                }
            } else {
                blackPawns ^= fromBit;
                switch (promotionPiece) {
                    case 'q':
                        blackQueens |= toBit;
                        break;
                    case 'r':
                        blackRooks |= toBit;
                        break;
                    case 'b':
                        blackBishops |= toBit;
                        break;
                    case 'n':
                        blackKnights |= toBit;
                        break;
                }
            }
        } else {
            // Mise à jour des bitboards pour les autres pièces
            if ((whitePawns & fromBit) != 0) {
                whitePawns ^= fromBit | toBit;
            } else if ((blackPawns & fromBit) != 0) {
                blackPawns ^= fromBit | toBit;
            } else if ((whiteRooks & fromBit) != 0) {
                whiteRooks ^= fromBit | toBit;
            } else if ((blackRooks & fromBit) != 0) {
                blackRooks ^= fromBit | toBit;
            } else if ((whiteKnights & fromBit) != 0) {
                whiteKnights ^= fromBit | toBit;
            } else if ((blackKnights & fromBit) != 0) {
                blackKnights ^= fromBit | toBit;
            } else if ((whiteBishops & fromBit) != 0) {
                whiteBishops ^= fromBit | toBit;
            } else if ((blackBishops & fromBit) != 0) {
                blackBishops ^= fromBit | toBit;
            } else if ((whiteQueens & fromBit) != 0) {
                whiteQueens ^= fromBit | toBit;
            } else if ((blackQueens & fromBit) != 0) {
                blackQueens ^= fromBit | toBit;
            }
        }
        if (enPassantColor != ColorToMove) {
            enPassantSquare = -1;
        }

        //update les matrices d'attaques

        //updatePieceBitboards();
        updateAttackBitboards();

        System.out.println("White " + Long.toBinaryString(whiteAttacks));
        System.out.println("Black " + Long.toBinaryString(blackAttacks));

        if (ColorToMove == -1) nbMoveTotal++;
        ColorToMove = -ColorToMove;
    }

    public static void main(String[] args) throws IOException {

        String startingFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        String FEN = "8/8/8/8/8/8/5P2/8 w KQkq - 0 1";
        String fFEN = "8/8/8/8/8/1kQ5/P7/8 w KQkq c3 0 1";
        String fdFEN = "r3k2r/8/8/8/8/8/8/8 b kq - 0 1";
        String ffFEN = "r3k2r/pppq1ppp/2n2n2/1B1p4/3P4/5N2/PPP1QPPP/R1B1K2R b KQkq - 0 1";
        String pos2 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        String pos3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        String mateIn1 = "6k1/5ppp/8/8/8/8/5PPP/R6K w KQkq - 0 1";
        String promote = "1r4k1/P7/8/8/8/8/5PPP/R6K w KQkq - 0 1";
        String FEN_2 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        /**
         * Depth	Nodes	Captures	E.p.	Castles	Promotions	Checks	Discovery Checks	Double Checks	Checkmates
         * 1	48	8	0	2	0	0	0	0	0
         * 2	2039	351	1	91	0	3	0	0	0
         * 3	97862	17102	45	3162	0	993	0	0	1
         * 4	4085603	757163	1929	128013	15172	25523	42	6	43
         * 5	193690690	35043416	73365	4993637	8392	3309887	19883	2637	30171
         * 6	8031647685	1558445089	3577504	184513607	56627920	92238050	568417	54948	360003
         */
        String FEN_3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        String FEN_31 = "8/8/8/KP6/1R6/8/4P1P1/8 w - - 0 1";
        /**
         *
         Depth	Nodes	Captures	E.p.	Castles	Promotions	Checks	Discovery Checks	Double Checks	Checkmates
         1	14	1	0	0	0	2	0	0	0
         2	191	14	0	0	0	10	0	0	0
         3	2812	209	2	0	0	267	3	0	0
         4	43238	3348	123	0	0	1680	106	0	17
         5	[9] 674624	52051	1165	0	0	52950	1292	3	0
         6	11030083	940350	33325	0	7552	452473	26067	0	2733
         7	178633661	14519036	294874	0	140024	12797406	370630	3612	87
         8	3009794393	267586558	8009239	0	6578076	135626805	7181487	1630	450410
         */
        String FEN_4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1";
        /**
         * Depth	Nodes	Captures	E.p.	Castles	Promotions	Checks	Checkmates
         * 1	6	0	0	0	0	0	0
         * 2	264	87	0	6	48	10	0
         * 3	9467	1021	4	0	120	38	22
         * 4	422333	131393	0	7795	60032	15492	5
         * 5	15833292	2046173	6512	0	329464	200568	50562
         * 6	706045033	210369132	212	10882006	81102984	26973664	81076
         */
        String FEN_5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8";
        /**
         *
         * Depth	Nodes
         * 1	44
         * 2	1,486
         * 3	62,379
         * 4	2,103,487
         * 5	89,941,194
         *
         * Issue e1g1 roque
         */

        String FENpoubelle = "3p4/8/8/3R4/8/8/8/3P4 w KQ - 1 8";
        String FENCheckCastle = "4pkp1/4p1p1/8/8/8/8/8/4K2R w KQ - 1 8"; //Fonctionne (le roi est en échec après le roque)
        String FENCheckEP = "4p1p1/4pkp1/8/3P4/8/8/8/4K2R b KQ - 1 8"; //Fonctionne (le roi est en échec après EP)

        /**
         * KADO ROMAING
         * C'est un mat de la position noire, mais l'engine renvoit false dans engine.isCheckmate(engine.ColorToMove)
         * T'aurais une idée ?
         * Des bisous !
         * Maxence (ton amour caché, le dis pas à Pimmy ^^)
         */
        String FenNeedToBeFixed = "5k2/2nq4/4pPQB/rp1pP2p/2pP3P/2P5/2P3P1/5RK1 b - - 2 28";
        // dont stop pawn prom 2k5/p1P3R1/1p3p2/8/7P/6P1/PP2p3/2n3K1 w - - 3 37

        Engine fix = new Engine(FenNeedToBeFixed);
        fix.printBoard();
        System.out.println(fix.generateLegalMoves(fix.ColorToMove, false));

//        Engine engine = new Engine(startingFEN);
        System.out.println("Knight");
        String Fen_N1 = "8/8/8/3N4/8/8/8/8 w KQ - 0 1";
        Engine engine_n1 = new Engine(Fen_N1);
        engine_n1.printBoard();
        engine_n1.printBitboard(engine_n1.whiteAttacks);

        System.out.println("Bishop");
        String Fen_B1 = "8/8/8/3B4/8/8/8/8 w KQ - 0 1";
        Engine engine_b1 = new Engine(Fen_B1);
        engine_b1.printBoard();
        engine_b1.printBitboard(engine_b1.whiteAttacks);

        System.out.println("Rook");
        String Fen_R1 = "8/8/8/3R4/8/8/8/8 w KQ - 0 1";
        Engine engine_r1 = new Engine(Fen_R1);
        engine_r1.printBoard();
        engine_r1.printBitboard(engine_r1.whiteAttacks);

        System.out.println("King");
        String Fen_K1 = "8/8/8/3K4/8/8/8/8 w KQ - 0 1";
        Engine engine_k1 = new Engine(Fen_K1);
        engine_k1.printBoard();
        engine_k1.printBitboard(engine_k1.whiteAttacks);

        System.out.println("Pawn");
        String Fen_P1 = "P7/8/8/P6P/8/8/8/P3P3 w KQ - 0 1";
        Engine engine_p1 = new Engine(Fen_P1);
        engine_p1.printBoard();
        engine_p1.printBitboard(engine_p1.whiteAttacks);


        System.out.println("Queen");
        String Fen_Q1 = "8/8/8/8/8/3pPp2/3pQp2/3ppp2 w KQ - 0 1";
        Engine engine_q1 = new Engine(Fen_Q1);
        engine_q1.printBoard();
        engine_q1.printBitboard(engine_q1.whiteAttacks);
        System.out.println(engine_q1.generateLegalMoves(1, false));
//

        Engine engineWhiteWin = new Engine("6k1/5ppp/8/8/8/8/5PPP/R6K w - - 0 1");
        engineWhiteWin.makeMove("a1a8");


        Engine engineBlackWin = new Engine("r5k1/5ppp/8/8/8/8/5PPP/7K b - - 0 1");
        engineBlackWin.makeMove("a8a1");

        Engine enginePat = new Engine(startingFEN);
        enginePat.makeMove("h2h4");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h7h5");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h1h3");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h8h6");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h3h1");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h6h8");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h1h3");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h8h6");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h3h1");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h6h8");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
        enginePat.makeMove("h1h3");
        System.out.println(Zobrist.getKeyForBoard(enginePat));
//        enginePat.makeMove("h8h6");
//        System.out.println(Zobrist.getKeyForBoard(enginePat));
//        enginePat.makeMove("h3h1");
//        System.out.println(Zobrist.getKeyForBoard(enginePat));
//        enginePat.makeMove("h6h8");
//        System.out.println(Zobrist.getKeyForBoard(enginePat));

        Engine engineInsuffisanceMaterialBishop = new Engine("7k/8/b7/8/8/8/8/7K b - - 0 1");
        engineInsuffisanceMaterialBishop.makeMove("h8g8");

        Engine engineInsuffisanceMaterialKing = new Engine("7k/8/8/8/8/8/8/7K b - - 0 1");
        engineInsuffisanceMaterialKing.makeMove("h8g8");

        Engine prom = new Engine("2k5/p1P3R1/1p3p2/8/7P/6P1/PP2p3/2n3K1 b - - 3 37");
        prom.executeMove("e2e1q");
        prom.printBoard();

        Engine ep = new Engine("r3k2r/p4p2/2p1p1p1/2NpPn2/5P1p/1P1P3P/P1P3P1/R4RK1 w kq - 1 19");
        ep.executeMove("g2g4");
        ep.executeMove("h4g3");
        ep.printBoard();

        Engine epcheckexe = new Engine("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        epcheckexe.executeMove("e2e4");
        epcheckexe.executeMove("f4e3");
        epcheckexe.printBoard();

//        System.out.println("Knight");
//        String Fen_N = "N7/8/8/7N/8/8/8/N7 w KQ - 0 1";
//        Engine engine_n = new Engine(Fen_N);
//        engine_n.printBoard();
//        engine_n.printBitboard(engine_n.whiteAttacks);
//
//        System.out.println("Bishop");
//        String Fen_B = "B7/8/8/7B/8/8/B7/B7 w KQ - 0 1";
//        Engine engine_b = new Engine(Fen_B);
//        engine_b.printBoard();
//        engine_b.printBitboard(engine_b.whiteAttacks);
//
//        System.out.println("Rook");
//        String Fen_R = "R7/8/8/7R/8/3R4/8/8 w KQ - 0 1";
//        Engine engine_r = new Engine(Fen_R);
//        engine_r.printBoard();
//        engine_r.printBitboard(engine_r.whiteAttacks);
//
//        System.out.println("King");
//        String Fen_K = "K4K2/8/8/K6K/8/8/8/K7 w KQ - 0 1";
//        Engine engine_k = new Engine(Fen_K);
//        engine_k.printBoard();
//        engine_k.printBitboard(engine_k.whiteAttacks);
//
//        System.out.println("Pawn");
//        String Fen_P = "P7/8/8/P6P/8/8/8/P3P3 w KQ - 0 1";
//        Engine engine_p = new Engine(Fen_P);
//        engine_p.printBoard();
//        engine_p.printBitboard(engine_p.whiteAttacks);
//
//        System.out.println("Queen");
//        String Fen_Q = "Q7/8/8/Q6Q/8/8/8/Q3PQ w KQ - 0 1";
//        Engine engine_q = new Engine(Fen_Q);
//        engine_q.printBoard();
//        engine_q.printBitboard(engine_q.whiteAttacks);
//
//        Engine enginePawnTest = new Engine("P7/P7/8/3P4/8/8/P7/8 w KQ - 0 1");
//        //enginePawnTest.printBitboard(enginePawnTest.getFileMaskForPawn(4));
//
//        enginePawnTest.printBitboard(enginePawnTest.whitePieces);
//        MinimaxIterative m = new MinimaxIterative(enginePawnTest);
//        System.out.println(m.evaluateDoubledPawn(enginePawnTest.whitePawns));
//        System.out.println(m.evaluateIsolatedPawn(enginePawnTest.whitePawns));
//        enginePawnTest.printBitboard(enginePawnTest.getAdjacentMask(56));

//        engine.printBoard();
////        engine.makeMoveCaptureOnlyGameMode("a1a8");
//        System.out.println(engine.generateLegalMoves(engine.ColorToMove, false).size());
//        System.out.println(engine.generateLegalMoves(engine.ColorToMove, false));
//        engine.makeMove("e2e4");
//        engine.printBoard();
//        engine.makeMove("f4e3");
//        engine.printBoard();
        //engine.makeMove("a5a4");
//        engine.printBitboard(engine.blackAttacks());
        //engine.printBoard();
//        System.out.println(Engine.isCaseAttacked(24, engine.blackAttacks));
//        System.out.println(Engine.isCaseAttacked(40, engine.blackAttacks));

//        engine.printBitboard(engine.whiteAttacks);
//        System.out.println(engine.generateLegalMoves(engine.ColorToMove, false).size());
//        engine.makeMove("e8g8");
//        engine.printBoard();
//        engine.makeMoveCaptureOnlyGameMode("g8h8");
//        System.out.println(engine.generateLegalMovesCaptureOnlyGameMode(engine.ColorToMove, true));
//        engine.printBoard();
//        engine.makeMoveCaptureOnlyGameMode("a8h8");
//        System.out.println(engine.generateLegalMovesCaptureOnlyGameMode(engine.ColorToMove, true));
//        engine.printBoard();

//        String RookTest = "8/1K6/8/8/1R5p/8/1P6/8 w KQkq - 0 1";
//        Engine test = new Engine(RookTest);
//        test.printBoard();
//        System.out.println(test.generateLegalMoves(1, false));
//        System.out.println(Rook.generateRookMoves(test.whiteRooks, test.whitePieces(), test.blackPieces()));
//        long key = -486317346;
//        System.out.println("retrive " + Rook.rookTable.get(new MagicBitboards.RookKey(27, 0x0000000000000000L, 0x0F0F0F0F0F0F0F0FL)));
//        System.out.println("retrive " + Rook.rookTable.get(7752547174122139398L));
//        System.out.println("nb " + Rook.rookTable.size());

    }
}

