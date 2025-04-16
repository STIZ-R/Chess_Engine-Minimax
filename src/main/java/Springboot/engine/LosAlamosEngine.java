package Springboot.engine;

import Springboot.piece.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LosAlamosEngine {

    //MATRICES
    public long whitePawns, whiteRooks, whiteKnights, whiteQueens, whiteKing;
    public long blackPawns, blackRooks, blackKnights, blackQueens, blackKing;
    public long whitePieces, blackPieces;

    public long blackAttacks, whiteAttacks;
    public long whitePawnsAttack, whiteRooksAttack, whiteKnightsAttack, whiteQueensAttack, whiteKingAttack;
    public long blackPawnsAttack, blackRooksAttack, blackKnightsAttack, blackQueensAttack, blackKingAttack;

    public long blackDefense, whiteDefense;
    public long whitePawnsDefense, whiteRooksDefense, whiteKnightsDefense, whiteQueensDefense, whiteKingDefense;
    public long blackPawnsDefense, blackRooksDefense, blackKnightsDefense, blackQueensDefense, blackKingDefense;

    //DROITS
    public int ColorToMove;

    //COUPS
    //règle des 50 coups
    public int nbMoveSinceCapture;
    //nb coups (noir + blanc) total
    public int nbMoveTotal;
    public ChessClock clock;

    //
    public Stack<LosAlamosMoveState> moveHistory = new Stack<>();
    public static final List<String> MOVE_BUFFER = new ArrayList<>(600);


    //CONSTANTES
    public static final long FILE_A = 0x41041041L; // Colonne A
    public static final long FILE_F = 0x820820820L; // Colonne H
    public static final long FILE_AB = 0xC30C30C3L; // Colonnes A et B
    public static final long FILE_EF = 0xC30C30C30L; // Colonnes G et H
    public static final long FILE_1 = 0x3FL;
    public static final long FILE_2 = 0xFC0L;
    public static final long FILE_6 = 0xFC0000000L;
    public static final long FILE_5 = 0x3F000000L;

    public static final long FILE_56 = 0xFFF000000L; // FFF000000
    public static final long FILE_12 = 0x000000FFFL;
    public static final long OUT_OF_BOUNDS_MASK = 0xFFFFFFFFFL; // Hors échiquier
    public static final long centerSquares = 0x1818000000L;


    public LosAlamosEngine(String fen) {
        setFEN(fen);
        clock = new ChessClock(180_000_000);
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
        String capture = parts[4];
        String move = parts[5];
        nbMoveSinceCapture = Integer.parseInt(capture);
        nbMoveTotal = Integer.parseInt(move);
        ColorToMove = turn.equals("w") ? 1 : -1;
        whitePawns = whiteRooks = whiteKnights  = whiteQueens = whiteKing = whitePieces = 0L;
        blackPawns = blackRooks = blackKnights  = blackQueens = blackKing = blackPieces = 0L;
        whitePawnsAttack =  whiteKingAttack = whiteKnightsAttack = whiteRooksAttack = whiteQueensAttack = 0L;
        blackRooksAttack = blackKingAttack = blackPawnsAttack = blackQueensAttack = blackKnightsAttack = 0L;
        int square = 30;
        for (char c : board.toCharArray()) {
            if (c == '/') {
                square -= 12;
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
        //updatePieceBitboards();
        //updateAttackBitboards();
        //updateDefenseBitboards();
    }

    /**
     * Génére le FEN associé à la position actuelle
     *
     * @return le fen associé
     */
    public String getFEN() {
        StringBuilder fen = new StringBuilder();

        for (int rank = 5; rank >= 0; rank--) {
            int emptyCount = 0;

            for (int file = 0; file < 6; file++) {
                int squareIndex = rank * 6 + file;
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

        fen.append(" -");

        fen.append(" -");

        fen.append(" ").append(nbMoveSinceCapture);

        fen.append(" ").append(nbMoveTotal);

        return fen.toString();
    }

    public void executeNullMove() {
        ColorToMove = -ColorToMove;
    }

    public void unmakeNullMove() {
        ColorToMove = -ColorToMove;
    }

    public static String convertNumberToSquare(int index) {
        int row = index / 8;
        int col = index % 8;
        char file = (char) ('a' + col);
        char rank = (char) ('1' + row);
        return "" + file + rank;
    }

    /**
     * Fonction qui effectue le coup et met à jour la liste des droits
     *
     * @param move -> coup joué
     */
    public void makeMove(String move) {
        List<String> legalMoves = generateLegalMoves(ColorToMove, false);

        if (!legalMoves.contains(move)) {
            throw new IllegalArgumentException("Mouvement illégal: " + move);
        }

        int from = (move.charAt(1) - '1') * 6 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 6 + (move.charAt(2) - 'a');
        long fromBit = 1L << from;
        long toBit = 1L << to;

        int pieceType = from;
        long piece = ColorToMove == 1 ? whitePawns : blackPawns;


        if ((ColorToMove == 1 && (whitePieces() & fromBit) == 0) ||
                (ColorToMove == -1 && (blackPieces() & fromBit) == 0)) {
            throw new IllegalArgumentException("C'est au tour des " + (ColorToMove == 1 ? "Blancs" : "Noirs"));
        }

        if (nbMoveSinceCapture + 1 == 51)
            throw new IllegalArgumentException("Plus de 50 coups depuis la dernière capture.");


        if (getPieceAt(to) == '.') nbMoveSinceCapture++;
        else nbMoveSinceCapture = 0;


        if ((ColorToMove == 1 && (whitePieces() & toBit) != 0) ||
                (ColorToMove == -1 && (blackPieces() & toBit) != 0)) {
            throw new IllegalArgumentException("Vous ne pouvez pas capturer une pièce alliée.");
        }

        if ((whitePieces() & toBit) != 0) {
            whitePawns &= ~toBit;
            whiteRooks &= ~toBit;
            whiteKnights &= ~toBit;
            whiteQueens &= ~toBit;
            whiteKing &= ~toBit;
        } else if ((blackPieces() & toBit) != 0) {
            blackPawns &= ~toBit;
            blackRooks &= ~toBit;
            blackKnights &= ~toBit;
            blackQueens &= ~toBit;
            blackKing &= ~toBit;
        }


        // Promotion de pions
        if ((ColorToMove == 1 && (to / 6 == 5) && (piece & fromBit) != 0) ||
                (ColorToMove == -1 && (to / 1 == 0) && (piece & fromBit) != 0)) {
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
            } else if ((whiteQueens & fromBit) != 0) {
                whiteQueens ^= fromBit | toBit;
            } else if ((blackQueens & fromBit) != 0) {
                blackQueens ^= fromBit | toBit;
            }
        }

        System.out.println("White " + Long.toBinaryString(whiteAttacks));
        System.out.println("Black " + Long.toBinaryString(blackAttacks));

        if (ColorToMove == -1) nbMoveTotal++;
        ColorToMove = -ColorToMove;
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
        for (String moveToVerify : pseudoLegalMoves) {
            int to = (moveToVerify.charAt(3) - '1') * 6 + (moveToVerify.charAt(2) - 'a');
            char destinationPiece = getPieceAt(to);
            moveHistory.push(new LosAlamosMoveState(this));
            executeMove(moveToVerify);
            boolean kingInCheck = isKingInCheck(color);
            if (!kingInCheck) {
                if (onlyGenerateCaptures) {
                    if (destinationPiece != '.') {
                        legalMoves.add(moveToVerify);
                    }
                } else {
                    legalMoves.add(moveToVerify);
                }
            }
            unmakeMove();
        }
        return legalMoves;
    }

    /**
     * Permet de simuler un coup (moins contraignant que makeMove()
     *
     * @param move
     */
    public void executeMove(String move) {

        int from = (move.charAt(1) - '1') * 6 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 6 + (move.charAt(2) - 'a');
        long fromBit = 1L << from;
        long toBit = 1L << to;

        if ((whitePieces() & toBit) != 0) {
            whitePawns &= ~toBit;
            whiteRooks &= ~toBit;
            whiteKnights &= ~toBit;
            whiteQueens &= ~toBit;
        } else if ((blackPieces() & toBit) != 0) {
            blackPawns &= ~toBit;
            blackRooks &= ~toBit;
            blackKnights &= ~toBit;
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
        } else if ((whiteQueens & fromBit) != 0) {
            whiteQueens ^= fromBit | toBit;
        } else if ((blackQueens & fromBit) != 0) {
            blackQueens ^= fromBit | toBit;
        } else if ((whiteKing & fromBit) != 0) {
            whiteKing ^= fromBit | toBit;
        } else if ((blackKing & fromBit) != 0) {
            blackKing ^= fromBit | toBit;
        }

        ColorToMove = -ColorToMove;
    }

    /**
     * Dépush l'état de la queue afin de restaurer l'échiquier
     */
    public void unmakeMove() {
        if (!moveHistory.isEmpty()) {
            LosAlamosMoveState previousState = moveHistory.pop();

            this.whitePawns = previousState.whitePawns;
            this.whiteRooks = previousState.whiteRooks;
            this.whiteKnights = previousState.whiteKnights;
            this.whiteQueens = previousState.whiteQueens;
            this.whiteKing = previousState.whiteKing;
            this.blackPawns = previousState.blackPawns;
            this.blackRooks = previousState.blackRooks;
            this.blackKnights = previousState.blackKnights;
            this.blackQueens = previousState.blackQueens;
            this.blackKing = previousState.blackKing;
            this.ColorToMove = previousState.ColorToMove;
        }
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
        if (isSlidingPieceAttacking(kingPosition, opponentPieces & (color == 1 ? blackRooks | blackQueens : whiteRooks | whiteQueens), new int[]{1, -1, 6, -6})) {
            //System.out.println("TOur");
            return true;
        }
        if (isSlidingPieceAttacking(kingPosition, opponentPieces & (color == 1 ? blackQueens :  whiteQueens), new int[]{5, -5, 7, -7})) {
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
        long attacks;
        if (color == 1) {
            attacks = ((pawns & ~FILE_F) >>> 5) | ((pawns & ~FILE_A) >>> 7);
        } else {
            attacks = ((pawns & ~FILE_A) << 5) | ((pawns & ~FILE_F) << 7);
        }
        return (attacks & king) != 0;
    }



    public boolean isKnightAttacking(long king, long knights) {
        long attacks = ((knights & ~FILE_F & ~FILE_56) << 13) |
                ((knights & ~FILE_A & ~FILE_56) << 11) | //
                ((knights & ~FILE_EF & ~FILE_6) << 8) |
                ((knights & ~FILE_AB & ~FILE_6) << 4)  |
                ((knights & ~FILE_A & ~FILE_12) >>> 13)|
                ((knights & ~FILE_F & ~FILE_12) >>> 11)|
                ((knights & ~FILE_AB & ~FILE_1) >>> 8)|
                ((knights & ~FILE_EF & ~FILE_1) >>> 4);
        return (attacks & king) != 0;
    }


    public boolean isSlidingPieceAttacking(long king, long slidingPieces, int[] directions) {
        for (int dir : directions) {
            long current = king;

            while (true) {

                if ((dir == 1)  && (current & FILE_F) != 0) break;
                if ((dir == -1) && (current & FILE_A) != 0) break;
                if ((dir == 6)  && (current & FILE_6) != 0) break;
                if ((dir == -6) && (current & FILE_1) != 0) break;
                if ((dir == 7)  && (current & (FILE_F | FILE_6)) != 0) break;
                if ((dir == 5)  && (current & (FILE_A | FILE_6)) != 0) break;
                if ((dir == -5) && (current & (FILE_F | FILE_1)) != 0) break;
                if ((dir == -7) && (current & (FILE_A | FILE_1)) != 0) break;

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
        long attacks = ((opponentKing & ~FILE_F) << 1) |
                ((opponentKing & ~FILE_A) >>> 1) |
                ((opponentKing & ~FILE_6) << 6) |
                ((opponentKing & ~FILE_1) >>> 6) |
                ((opponentKing & ~FILE_F & ~FILE_6) << 7) |
                ((opponentKing & ~FILE_A & ~FILE_6) << 5) |
                ((opponentKing & ~FILE_A & ~FILE_1) >>> 7) |
                ((opponentKing & ~FILE_F & ~FILE_1) >>> 5);

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

    public boolean isPromotion(String move) {
        return move.length() == 5;
    }

    public boolean isCapture(String move) {
        int to = (move.charAt(3) - '1') * 6 + (move.charAt(2) - 'a');
        return getPieceAt(to) != '.';
    }

    public void checkGameStatus() {
        if (isCheckmate(1)) {
            System.out.println("Les noirs gagnent par échec et mat.");
        } else if (isCheckmate(-1)) {
            System.out.println("Les blancs gagnent par échec et mat.");
        } else {
            System.out.println("Le jeu continue.");
        }
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
            MOVE_BUFFER.addAll(Pawn.generatePawnMovesLosAlamos(whitePawns, whitePieces(), blackPieces(), true));
            MOVE_BUFFER.addAll(Rook.generateRookMovesLosAlamos(whiteRooks, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Knight.generateKnightMovesLosAlamos(whiteKnights, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Queen.generateQueenMovesLosAlamos(whiteQueens, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(King.generateKingMovesLosAlamos(whiteKing, whitePieces(), blackPieces()));
            MOVE_BUFFER.addAll(Pawn.generatePawnPromotionsLosAlamos(whitePawns, whitePieces(), blackKing, blackPieces(), true));
        } else {
            MOVE_BUFFER.addAll(Pawn.generatePawnMovesLosAlamos(blackPawns, blackPieces(), whitePieces(), false));
            MOVE_BUFFER.addAll(Rook.generateRookMovesLosAlamos(blackRooks, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Knight.generateKnightMovesLosAlamos(blackKnights, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Queen.generateQueenMovesLosAlamos(blackQueens, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(King.generateKingMovesLosAlamos(blackKing, blackPieces(), whitePieces()));
            MOVE_BUFFER.addAll(Pawn.generatePawnPromotionsLosAlamos(blackPawns, blackPieces(), whiteKing, whitePieces(), false));
        }
        return new ArrayList<>(MOVE_BUFFER);
    }


    public int squareToIndex(String square) {
        char file = square.charAt(0);
        char rank = square.charAt(1);

        int fileIndex = file - 'a';
        int rankIndex = rank - '1';

        return rankIndex * 6 + fileIndex;
    }


    // Fonction pour vérifier la présence d'une pièce à une position donnée
    private boolean isPieceAt(int position) {
        long bit = 1L << position;
        return (whitePieces() & bit) != 0 || (blackPieces() & bit) != 0;
    }

    public long whitePieces() {
        return whitePawns | whiteRooks | whiteKnights  | whiteQueens | whiteKing;
    }

    public long blackPieces() {
        return blackPawns | blackRooks | blackKnights | blackQueens | blackKing;
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
        for (int rank = 5; rank >= 0; rank--) {
            for (int file = 0; file < 6; file++) {
                int squareIndex = rank * 6 + file;
                char piece = getPieceAt(squareIndex);
                System.out.print(piece + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public void printBitboard(long bitboard) {
        for (int rank = 5; rank >= 0; rank--) {
            for (int file = 0; file < 6; file++) {
                long square = 1L << (rank * 6 + file);
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
        if ((whiteQueens & bit) != 0) return 'Q';
        if ((whiteKing & bit) != 0) return 'K';
        if ((blackPawns & bit) != 0) return 'p';
        if ((blackRooks & bit) != 0) return 'r';
        if ((blackKnights & bit) != 0) return 'n';
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



    public boolean isPromotionMove(String move) {
        return move.length() == 5 && (move.charAt(4) == 'q' || move.charAt(4) == 'r' || move.charAt(4) == 'b' || move.charAt(4) == 'n');
    }

    public static void main(String[] args) {

        String startingFEN = "rnqknr/pppppp/6/6/PPPPPP/RNQKNR w - - 0 1";
        String knight = "6/3N2/6/6/6/6 w - - 0 1";
        LosAlamosEngine engine = new LosAlamosEngine(startingFEN);
        engine.printBoard();
        System.out.println(engine.generateLegalMoves(1, false));
        System.out.println(engine.getFEN());
        engine.makeMove("d2d3");
        engine.makeMove("c5c4");
        engine.makeMove("d3c4");
        engine.printBoard();


    }
}

