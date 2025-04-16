package Springboot.ai;

import Springboot.engine.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;



//import static Springboot.ai.Syzygy.MAX_PIECES;



//import static Springboot.ai.Syzygy.MAX_PIECES;

/**
 * @author stizr
 * @license MIT License
 * <p>
 * Classe Minimax
 * <p>
 * But de Minimax:
 * Pouvoir retourner le meilleur coup selon une heuristique donnée.
 * En effet, nous regardons toutes les possibilités des coups possibles et assignons une valeur
 * à l'issue d'une profondeur de coups (noir ou blanc) donné.
 * Cela va ensuite permettre de minimiser la perte maximum.
 * <p>
 * Exemple:
 * <p>
 * 5              MAXIMUM de chaque enfant
 * /  |  |  \
 * 2   1  5   2         MINIMUM de chaque enfant
 * /\  /\  /\  /\
 * 2 3 4 1 5 6 2 10       Fonction d'evaluation
 * <p>
 * [ISSUE] - OLD
 * Coup bizarre,... a1b1 / b1a1 en boucle pour une partie comme celle-ci: rnb1kb1r/1pp1np2/6p1/1p1p3p/4P3/N7/PPPP1PPP/R1B1K1NR w KQkq - 0 9
 * <p>
 * <p>
 * [AMELIORATION] - DONE
 * OrderMove -> fonction GetMoveScoreGuess à améliorer
 * Pour un FEN quelquonque, on passe de 183 sec pour une profondeur de 7
 * à 34 sec pour la même profondeur
 * [SOLUTION]
 * On régit les mouvements dans l'ordre suivant:   (Checkmate -> Capture -> Promotion -> le reste)
 * Le problème était la capture, en partant du principe de MVV/LVA, au final, un coup de capture valait -400 malgré
 * étant le meilleur. AU final je pars du principe que la pièce la plus grande en terme de valeur est la dame (1000)
 * ensuite on soustrait la valeur de la pièce joué par la pièce attrapé.
 * Soit 1000 - 900 = 100 au minimum et 1000 - (-800) = 1800 au maximum.
 * De ce fait, la capture reste toujours valuable dans l'order Moves
 * Pour le moment, cela à l'aire de fonctionner, à voir si d'autres cas posent problèmes.
 * <p>
 * [ROAD MAP]
 * -Amélioration de l'orderMoves - DONE
 * -Amélioration de la Quiescence Search - DONE
 * -Reconnaissance des Mats selon la depth (le plus tôt est le mieux en échec et mat, mat forcé,...) - DONE
 * -Amélioration de la fonction d'évaluation de la depth final - ~DONE (still in work)
 * -Structure des pions - ~DONE (ADDING PAWN TABLE)
 * -Dévelopemment des pièces - DONE
 * -Tables de Transpositions avec le hashage de Zobrist - DONE
 * -Book Opening - DONE
 * -Magics Bitboard - ... (some issue with that)
 * -...
 *<p>
 *
 * [COMPLEXITE]
 * La complexité de l'algorithme de notre moteur permet de savoir à combien de noeuds on évalue avant de passer à la profondeur supérieur.
 *
 * Historiquement, la complexité du jeu d'échec est de 35, càd, qu'à chaque coup possible, nous avons en moyenne 35 coups en réponse.
 * Le nombre de Shanon permet d'évaluer la complexité totale des échecs soit environ 10^120.
 *
 * Pour calculer la complexité de notre moteur, on utilise la formule ci-dessous
 *
 * Branching Factor : b= sqrt(N, d)
 * d = depth
 * N = Nb of nodes
 * b = branching factor
 *
 *  Exemple : b = 7sqrt 4379588
 * b ~= 8,89
 * <p>
 * Par exemple StockFish est à ~1.6
 * <p>
 * Je suis passé de ~20 à ~8,89 en implémentant différentes techniques (meilleur moveordering, killer heuristic, TT, null window, late move reduce,...)
 *
 * Actuellement, nous sommes à environ ~2.5 à 3 en terme de complexité.
 * Cela nous permet d'élaguer un maximum de branches inutiles tout en parcourant le plus de profondeur possible!
 *</p>
 */
public class MinimaxIterative {

    /*  Initialisation des variables de recherche  */
    public double time;
    public double startTime;
    public double timeQuiescence;
    public int nodeCount;
    public int quiescenceNodeCount;
    int ttHits = 0;
    int ttMisses = 0;
    int ttHitsQS = 0;
    int ttMissesQS = 0;

    /*  Valeur des pièces et différentes choses (alpha, beta, mate,...) */
    //try new value based on chess.com article -> https://www.chess.com/blog/crispychessblog/an-in-depth-look-into-how-engines-find-the-best-move
    public final int pawnValue = 100;
    public final int knightValue = 325;
    public final int bishopValue = 325;
    public final int rookValue = 500;
    public final int queenValue = 975;
    public final int bishopPairValue = 50;
    public final int rookPairValue = 5;
    public final int knightPairValue = 25;
    public final int kingValue = 20000;
    public final int mateScore = 100000;
    public final int positiveInfinity = Integer.MAX_VALUE - 1;
    public final int negativeInfinity = Integer.MIN_VALUE + 1;

    /*  Initialisation des différentes classes  */
    public Engine engine;
    public TranspositionTable transpositionTable;
    public TranspositionTable transpositionTableQS;
    public RepetitionTable repetitionTable;
    public BookOpening bookOpening;
    String bookFilePath = "src/main/resources/openings/Book.txt";
    //public NNUE nnue;
    private boolean useNNUE = false;
    private boolean fastVersion = false;
    private static final String BIG_NET_PATH = "src/main/resources/NNUE/nn-b1a57edbea57.nnue";
    private static final String SMALL_NET_PATH ="src/main/resources/NNUE/nn-baff1ede1f90.nnue";
    //public Syzygy syzygy;

    /*  Initialisation des variables utile pour la recherche  */
    private static final long TIME_LIMIT = 20_000_000_000L;
    private static final int MAX_DEPTH = 101;
    private String[][] killerMoves = new String[MAX_DEPTH][2];
    private int[][] historyTable2 = new int[64][64];
    private int[][][] historyTable = new int[6][64][64];
    private int numberOfPieces;
    private PawnHashTable[] pawnHashTable;
    private static final int PAWN_HASH_SIZE = 16384;
    private long[] pawnZobristKeys;

    //      Return the best sequence found
    //private Map<Long, List<String>> bestMoveSequences = new HashMap<>();

    /*  autres  */
    public LosAlamosEngine losAlamosEngine;

    /*  à voir  */
    public final boolean end = false;


//    static {
//        NNUEBridge.init(BIG_NET_PATH, SMALL_NET_PATH);
//    }

    /**
     * Constructeur de la classe
     *
     * @param e -> Prend un moteur de jeu en paramètre pour l'initialisation
     * @throws IOException -> retour d'erreurs possibles
     */
    public MinimaxIterative(Engine e, boolean useNNUE, boolean faster, boolean threaded) throws IOException {
        this.useNNUE = useNNUE;
        if(!threaded && useNNUE) {
            try {
                NNUEBridge.init(BIG_NET_PATH, SMALL_NET_PATH);
                this.useNNUE = true;
            } catch (Exception ex) {
                this.useNNUE = false;
            }
        }
        this.nodeCount = 0;
        this.quiescenceNodeCount = 0;
        this.engine = e;
        this.fastVersion = faster;
        this.bookOpening = new BookOpening(bookFilePath);
        //this.syzygy = new Syzygy("src/main/resources/syzygy/");
        this.transpositionTable = new TranspositionTable();
        this.transpositionTableQS = new TranspositionTable();
        this.repetitionTable = new RepetitionTable();
        this.pawnHashTable = new PawnHashTable[PAWN_HASH_SIZE];
        this.numberOfPieces = Long.bitCount(engine.blackPieces | engine.whitePieces);
    }

    public MinimaxIterative(LosAlamosEngine e) throws IOException {
        this.nodeCount = 0;
        this.quiescenceNodeCount = 0;
        this.losAlamosEngine = e;
        this.transpositionTable = new TranspositionTable();
        this.transpositionTableQS = new TranspositionTable();
        this.repetitionTable = new RepetitionTable();
    }


//    private Integer askSyzygy() throws IOException {
//        String fen = engine.getFEN();
//        System.out.println("FEN passé à Syzygy : " + fen);
//        SyzygyResult result = syzygy.probeWDL(fen);
//        int totalPieces = Long.bitCount(engine.whitePieces() | engine.blackPieces());
//        //System.out.println("Total pièces : " + totalPieces); // Débogage
//        if (totalPieces > MAX_PIECES) {
//            //System.out.println("Trop de pièces pour Syzygy");
//            return null;
//        }
//
//        if (result == null) {
//            //System.out.println("Syzygy non trouvé pour " + fen);
//            return null;
//        }
//
//        //System.out.println("Syzygy WDL : " + result.wdl); // Débogage
//        int perspective = engine.ColorToMove == 1 ? 1 : -1;
//        switch (result.wdl) {
//            case "WIN": return perspective * (mateScore - 100);
//            case "LOSS": return perspective * (-mateScore + 100);
//            case "DRAW": return 0;
//            case "CURSED_WIN": return perspective * (mateScore - 200);
//            case "BLESSED_LOSS": return perspective * (-mateScore + 200);
//            default:
//                System.out.println("Résultat Syzygy inconnu : " + result.wdl);
//                return null;
//        }
//    }
//
//    public void shutdown() throws IOException {
//        syzygy.close();
//    }

    /**
     * Fonction qui évalue la phase de la partie.
     * Un score entre 0 et 1 est retourné pour avoir l'avancement de la partie.
     *
     * @return 1 si au début, 0 si à la fin
     */
    private double getGamePhase() {
        int whiteNonPawnMaterial = Long.bitCount(engine.whiteKnights) * knightValue +
                Long.bitCount(engine.whiteBishops) * bishopValue +
                Long.bitCount(engine.whiteRooks) * rookValue +
                Long.bitCount(engine.whiteQueens) * queenValue;
        int blackNonPawnMaterial = Long.bitCount(engine.blackKnights) * knightValue +
                Long.bitCount(engine.blackBishops) * bishopValue +
                Long.bitCount(engine.blackRooks) * rookValue +
                Long.bitCount(engine.blackQueens) * queenValue;
        int totalMaterial = whiteNonPawnMaterial + blackNonPawnMaterial;
        double maxMaterial = 2 * (2 * knightValue + 2 * bishopValue + 2 * rookValue + queenValue); // 3900
        return Math.min(1.0, totalMaterial / maxMaterial); // 1.0 = début, 0.0 = fin
    }


    /**
     * Fonction d'évaluation final
     *
     * @return un score associé au plateau selon une ou plusieurs règles
     */
    public int evaluate() {
//        long pawnKey = Zobrist.computePawnZobristKey(engine.whitePawns, engine.blackPawns);
//        int index = (int)(pawnKey & (PAWN_HASH_SIZE - 1));
//        PawnHashTable entry = pawnHashTable[index];
//
//        int pawnScore, passedPawns, pawnShield;
//        if (entry != null && entry.pawnZobristKey == pawnKey) {
//            pawnScore = entry.pawnScore;
//            passedPawns = entry.passedPawns;
//            pawnShield = entry.pawnShield;
//        } else {
//            pawnScore = 0;//evaluatePawnStructure();
//            passedPawns = evaluatePassedPawns();
//            pawnShield = evaluatePawnShield(engine.ColorToMove,
//                    engine.ColorToMove == 1 ? engine.whiteKing : engine.blackKing,
//                    engine.ColorToMove == 1 ? engine.whitePawns : engine.blackPawns,
//                    engine.ColorToMove == 1 ? engine.whiteRooks : engine.blackRooks);
//
//            pawnHashTable[index] = new PawnHashTable(pawnKey, pawnScore, passedPawns, pawnShield);
//        }

        /* Pas encore d'utilité  */

//        if(engine.whitePieces == (engine.whiteQueens & engine.whiteKing) && engine.ColorToMove == 1) {
//            return KQK(engine.whiteKing, engine.whiteQueens, engine.blackKing) * perspective;
//        }
//
//        if(engine.blackPieces == (engine.blackQueens & engine.blackKing) && engine.ColorToMove == -1) {
//            return KQK(engine.blackKing, engine.blackQueens, engine.whiteKing) * perspective;
//        }
//        if(engine.whitePieces == (engine.whiteKnights & engine.whiteBishops & engine.whiteKing) && engine.ColorToMove == 1) {
//            return KNBK(engine.whiteKing, engine.whiteKnights, engine.whiteBishops, engine.blackKing) * perspective;
//        }
//
//        if(engine.blackPieces == (engine.blackKnights & engine.blackBishops & engine.blackKing) && engine.ColorToMove == -1) {
//            return KNBK(engine.blackKing, engine.blackKnights, engine.blackBishops, engine.whiteKing) * perspective;
//        }
//        if(useNNUE) {
//            return NNUEBridge.evalFen(engine.getFEN());
//        }

        if (useNNUE && fastVersion) {
            int[] pieceBoard = convertBoardToNNUEArray();
            int[] pieces = new int[32];
            int[] squares = new int[32];
            int side = (engine.ColorToMove == 1) ? 0 : 1;
            int rule50 = engine.nbMoveSinceCapture;

            int pieceAmount = convertBoardForFasterEval(pieceBoard, pieces, squares);

            try {
                //System.out.println(NNUEBridge.evalFen(engine.getFEN()));
                return NNUEBridge.fasterEvalArray(pieces, squares, pieceAmount, side, rule50);
            } catch (Exception e) {
                System.out.println("Erreur NNUE : " + e.getMessage());
                useNNUE = false;
            }
        }

        if (useNNUE) {
            int[] pieceBoard = convertBoardToNNUEArray();
            int side = (engine.ColorToMove == 1) ? 0 : 1;
            int rule50 = engine.nbMoveSinceCapture;

            try {
                int nnueScore = NNUEBridge.evalArray(pieceBoard, side, rule50);
                return nnueScore;
            } catch (Exception e) {
                System.out.println("Erreur NNUE: " + e.getMessage());
                useNNUE = false;
            }
        }

        //System.out.println("debug");

        int perspective = (engine.ColorToMove == 1) ? 1 : -1;
        double phase = getGamePhase();
        int whiteEval = 0;
        int blackEval = 0;

        int whiteMaterial = countMaterial(1, engine.whitePieces());
        int blackMaterial = countMaterial(-1, engine.blackPieces());

        int whitePositional = countPositionalValue(1, phase);
        int blackPositional = countPositionalValue(-1, phase);

        int whiteDefense = evaluateDefense(1);
        int blackDefense = evaluateDefense(-1);

        int blackKingAttacked = Long.bitCount(engine.blackKing & engine.whitePieces) * 150;
        int whiteKingAttacked = Long.bitCount(engine.whiteKing & engine.blackPieces) * 150;

        int whiteMove = movePossible(1);
        int blackMove = movePossible(-1);

        int whiteKingSafety = kingSafety(engine.ColorToMove, engine.whiteKing, engine.blackPieces(), phase);
        int blackKingSafety = kingSafety(engine.ColorToMove, engine.blackKing, engine.whitePieces(), phase);

        int whiteDoubledPawns = evaluateDoubledPawn(engine.whitePawns);
        int blackDoubledPawns = evaluateDoubledPawn(engine.blackPawns);

        int whiteIsolatedPawns = evaluateIsolatedPawn(engine.whitePawns);
        int blackIsolatedPawns = evaluateIsolatedPawn(engine.blackPawns);

        int whitePawnStructure = pawnStructure(engine.whitePawns, engine.whitePawnsDefense);
        int blackPawnStructure = pawnStructure(engine.blackPawns, engine.blackPawnsDefense);

        int whitePawnDefense = pawnDefense(engine.whitePawnsAttacksAllied, engine.whitePieces);
        int blackPawnDefense = pawnDefense(engine.blackPawnsAttacksAllied, engine.blackPieces);

        int whiteAttacks = evaluateAttack(1);//*/Long.bitCount(engine.whiteAttacks & engine.blackPieces) * 3;
        int blackAttacks = evaluateAttack(-1);//*/Long.bitCount(engine.blackAttacks & engine.whitePieces) * 3;

        int whiteControlMid = Long.bitCount(engine.whitePieces() & Engine.centerSquares) * 2;
        int blackControlMid = Long.bitCount(engine.blackPieces() & Engine.centerSquares) * 2;

        int whiteKingTropism = kingTropism(engine.whiteKing, engine.blackPieces, engine.whitePieces, Engine.centerSquares);
        int blackKingTropism = kingTropism(engine.blackKing, engine.whitePieces, engine.blackPieces, Engine.centerSquares);

        whiteEval = whiteMaterial + whiteMove + whitePositional + whiteKingSafety + whitePawnStructure + whitePawnDefense + whiteAttacks + whiteControlMid + whiteDoubledPawns + whiteIsolatedPawns + whiteKingTropism + whiteDefense + blackKingAttacked;
        blackEval = blackMaterial + blackMove + blackPositional + blackKingSafety + blackPawnStructure + blackPawnDefense + blackAttacks + blackControlMid + blackDoubledPawns + blackIsolatedPawns + blackKingTropism + blackDefense + whiteKingAttacked;

        int finalEvaluation = whiteEval - blackEval;
        return finalEvaluation * perspective;
    }


    private int evaluatePassedPawns() {
        int whitePassed = 0, blackPassed = 0;
        long whitePawns = engine.whitePawns;
        long blackPawns = engine.blackPawns;

        while (whitePawns != 0) {
            int sq = Long.numberOfTrailingZeros(whitePawns);
            long fileMask = engine.getFileMaskForPawn(sq % 8);
            long frontSpan = fileMask & (0xFFFFFFFFFFFFFFL << sq);
            if ((frontSpan & blackPawns) == 0) {
                whitePassed += 20 + (sq / 8) * 10;  // Bonus augmente avec la rangée
            }
            whitePawns &= whitePawns - 1;
        }

        while (blackPawns != 0) {
            int sq = Long.numberOfTrailingZeros(blackPawns);
            long fileMask = engine.getFileMaskForPawn(sq % 8);
            long frontSpan = fileMask & (0xFFFFFFFFFFFFFFL >>> (63 - sq));
            if ((frontSpan & whitePawns) == 0) {
                blackPassed += 20 + (7 - sq / 8) * 10;
            }
            blackPawns &= blackPawns - 1;
        }

        return whitePassed - blackPassed;
    }


    /**
     * Permet d'évaluer les pièces qui défendent d'autres pièces
     *
     * @param color -> couleur du défenseur
     * @return un score selon la défense des pièces
     */
    private int evaluateDefense(int color) {
        int bonus = 0;

        long pawnDefense, kingDefense, queenDefense, rookDefense, bishopDefense, knightDefense;

        if (color == 1) {
            pawnDefense = engine.whitePawnsDefense;
            kingDefense = engine.whiteKingDefense;
            queenDefense = engine.whiteQueensDefense;
            rookDefense = engine.whiteRooksDefense;
            bishopDefense = engine.whiteBishopsDefense;
            knightDefense = engine.whiteKnightsDefense;
        } else {
            pawnDefense = engine.blackPawnsDefense;
            kingDefense = engine.blackKingDefense;
            queenDefense = engine.blackQueensDefense;
            rookDefense = engine.blackRooksDefense;
            bishopDefense = engine.blackBishopsDefense;
            knightDefense = engine.blackKnightsDefense;
        }

        long totalDefense = pawnDefense | kingDefense | queenDefense | rookDefense | bishopDefense | knightDefense;

        bonus += evaluateMultipleDefenders(totalDefense);

        bonus += evaluateProtectedPieces(totalDefense, pawnDefense, kingDefense, queenDefense, rookDefense, bishopDefense, knightDefense);

        return bonus;
    }

    private int evaluateMultipleDefenders(long totalDefense) {
        int bonus = 0;

        for (int i = 0; i < 64; i++) {
            if ((totalDefense & (1L << i)) != 0) {
                bonus += 2;
            }
        }
        return bonus;
    }

    private int evaluateProtectedPieces(long totalDefense, long... pieceDefended) {
        int bonus = 0;

        for (long piece : pieceDefended) {
            bonus += Long.bitCount(piece & totalDefense) * 3;
        }
        return bonus;
    }


    /**
     * Evalue les différents attaquants
     *
     * @param color -> couleur des pièces qui attaquent
     * @return un score selon les différents attaquants
     */
    private int evaluateAttack(int color) {
        int bonus = 0;

        long pawnAttack, kingAttack, queenAttack, rookAttack, bishopAttack, knightAttack;

        if (color == 1) {
            pawnAttack = engine.whitePawnsAttack;
            kingAttack = engine.whiteKingAttack;
            queenAttack = engine.whiteQueensAttack;
            rookAttack = engine.whiteRooksAttack;
            bishopAttack = engine.whiteBishopsAttack;
            knightAttack = engine.whiteKnightsAttack;
        } else {
            pawnAttack = engine.blackPawnsAttack;
            kingAttack = engine.blackKingAttack;
            queenAttack = engine.blackQueensAttack;
            rookAttack = engine.blackRooksAttack;
            bishopAttack = engine.blackBishopsAttack;
            knightAttack = engine.blackKnightsAttack;
        }

        long totalDefense = pawnAttack | kingAttack | queenAttack | rookAttack | bishopAttack | knightAttack;

        bonus += evaluateMultipleAttackers(totalDefense);

        bonus += evaluateAttackedPieces(totalDefense, pawnAttack, kingAttack, queenAttack, rookAttack, bishopAttack, knightAttack);

        return bonus;
    }

    private int evaluateMultipleAttackers(long totalAttack) {
        int bonus = 0;

        for (int i = 0; i < 64; i++) {
            if ((totalAttack & (1L << i)) != 0) {
                bonus += 2;
            }
        }
        return bonus;
    }

    private int evaluateAttackedPieces(long totalAttack, long... pieceAttacked) {
        int bonus = 0;

        for (long piece : pieceAttacked) {
            bonus += Long.bitCount(piece & totalAttack);
        }
        return bonus;
    }


    /**
     * Retourne la distance de Manhattan d'une pièce par rapport aux autres
     * La valeur la plus petite sera alors retourné
     *
     * @param pos1 -> Position de la pièce 1
     * @param pos2 -> Position des autres pièces
     * @return La distance de Manhattan la plus petite entre @pos1 et @pos2
     */
    private int calculateManhattanDistance(int pos1, long pos2) {
        ArrayList<Integer> list = new ArrayList<>();
        int x1 = (pos1 % 8);
        int y1 = (pos1 / 8);
        for (int i = 0; i <= Long.bitCount(pos2); i++) {
            int pos = Long.numberOfTrailingZeros(pos2);
            int x2 = (pos % 8);
            int y2 = (pos / 8);
            //System.out.print(" long : " + Long.toBinaryString(pos2) + "\n");
            pos2 &= pos2 - 1;
            //System.out.println(" Math abs:" + Math.abs(x1 - x2) + Math.abs(y1 - y2) + "\n");
            list.add(Math.abs(x1 - x2) + Math.abs(y1 - y2));
        }
        return Collections.min(list);
    }

    /**
     * Permet de connaitre la coin le plus approprié pour les fous
     *
     * @param bishop -> bitboard des fous
     * @return un bitboard où les fous doivent aller
     */
    private long getTargetCorner(long bishop) {
        boolean isBishopOnLightSquare = isLightSquare(bishop);
        return isBishopOnLightSquare ? 0x0000000000000001L : 0x8000000000000000L; // a1 ou h8
    }

    /**
     * Permet de connaitre si la case est claire ou foncée
     *
     * @param square -> bitboard de la case
     * @return true si blanc sinon false
     */
    private boolean isLightSquare(long square) {
        int x = (int) (square % 8);
        int y = (int) (square / 8);
        return (x + y) % 2 == 0;
    }

    private boolean isBishopControllingCorner(long bishop, long corner) {
        return isDiagonal(bishop, corner);
    }

    private boolean isKnightRestricting(long opponentKing, long knight) {
        int[] knightMoves = {
                -17, -15, -10, -6, 6, 10, 15, 17
        };

        int restrictedMoves = 0;

        for (int move : knightMoves) {
            long targetSquare = knight + move;

            if (isValidSquare(targetSquare) && isAroundKing(opponentKing, targetSquare)) {
                restrictedMoves++;
            }
        }

        return restrictedMoves >= 2;
    }

    private boolean isValidSquare(long square) {
        return square >= 0 && square < 64; // Cases 0 à 63
    }

    private boolean isAroundKing(long king, long square) {
        int kingX = (int) (king % 8);
        int kingY = (int) (king / 8);

        int squareX = (int) (square % 8);
        int squareY = (int) (square / 8);

        return Math.abs(kingX - squareX) <= 1 && Math.abs(kingY - squareY) <= 1;
    }

    private boolean isDiagonal(long pos1, long pos2) {
        int x1 = (int) (pos1 % 8);
        int y1 = (int) (pos1 / 8);
        int x2 = (int) (pos2 % 8);
        int y2 = (int) (pos2 / 8);

        return Math.abs(x1 - x2) == Math.abs(y1 - y2);
    }

    private boolean isRookControllingRowOrColumn(long rook, long king) {
        return (rook % 8 == king % 8) || (rook / 8 == king / 8);
    }

    private boolean isKingOnEdge(long king) {
        int x = (int) (king % 8);
        int y = (int) (king / 8);
        return x == 0 || x == 7 || y == 0 || y == 7;
    }

    private boolean isKingBlockingPawn(long king, long pawn) {
        int kingX = (int) (king % 8);
        int kingY = (int) (king / 8);

        int pawnX = (int) (pawn % 8);
        int pawnY = (int) (pawn / 8);

        return (kingX == pawnX && kingY == pawnY + 1);
    }

    public int kingTropism(long king, long enemyPiece, long allyPiece, long center) {
        int score = 0;

        //+ petite valeur à récupérer
        int distanceToEnemy = calculateManhattanDistance(Long.numberOfTrailingZeros(king), enemyPiece);
        if (distanceToEnemy <= 3) {
            score -= (4 - distanceToEnemy) * 3;
        }

        int distanceToAlly = calculateManhattanDistance(Long.numberOfTrailingZeros(king), allyPiece);
        if (distanceToAlly <= 3) {
            score += (4 - distanceToAlly) * 5;
        }

        int distanceToCenter = calculateManhattanDistance(Long.numberOfTrailingZeros(king), center);
        score += (4 - distanceToCenter) * 7;

        //faire se rapprocher des pions apssés en fin de partie.

        return score;
    }


    /**
     * KQK est utile pour les finales de type King - Queen versus King
     * Cela va permettre de forcer à mettre le Roi ennemi dans un corner afin de le mettre en
     * échec et mat avec la Reine
     *
     * @param king -> Roi
     * @param alliedQueen -> Reine alliée
     * @param oppositeKing -> Roi ennemi
     * @return un bonus selon la position de plateau actuel
     */
//    public int KQK(long king, long alliedQueen, long oppositeKing) {
//        int score = 0;
//        int distanceKings = calculateManhattanDistance(king, oppositeKing);
//        int distanceQueenToKing = calculateManhattanDistance(alliedQueen, oppositeKing);
//
//        if (distanceKings <= 2) {
//            score += 50;
//        } else {
//            score -= 10;
//        }
//
//        if (distanceQueenToKing <= 1) {
//            score += 100;
//        } else if (distanceQueenToKing <= 3) {
//            score += 50;
//        } else {
//            score -= 20;
//        }
//
//        return score;
//    }

    /**
     * KNBK est utile pour les finales de types King - Knight - Bishop versus King
     * Cela va permettre de forcer le Roi ennemi dans un coin du plateau
     * Tout en forçant le Cavalier à bloquer un maximum de cases au Roi.
     *
     * @param allyKing -> Roi
     * @param allyKnight -> Cavaliers alliés
     * @param allyBishop -> Fous alliés
     * @param opponentKing -> Roi ennemi
     * @return un bonus selon la disposition des pièces
     */
//    public int KNBK(long allyKing, long allyKnight, long allyBishop, long opponentKing) {
//        int score = 0;
//
//        long targetCorner = getTargetCorner(allyBishop);
//        int distanceToCorner = calculateManhattanDistance(opponentKing, targetCorner);
//        score -= distanceToCorner * 10;
//
//        int distanceKings = calculateManhattanDistance(allyKing, opponentKing);
//        score += Math.max(0, 10 - distanceKings) * 5;
//
//        if (isBishopControllingCorner(allyBishop, targetCorner)) {
//            score += 50;
//        }
//
//        if (isKnightRestricting(opponentKing, allyKnight)) {
//            score += 30;
//        }
//
//        return score;
//    }

    /**
     * Utile pour les finales de type King - Bishop versus King
     * Cela va forcer le Fou à controler les coins du plateau tout en
     * essayant de pousser le Roi ennemi dans les recoins du plateau.
     *
     * @param allyKing -> Roi
     * @param allyBishop -> Fous alliés
     * @param opponentKing -> Roi ennemi
     * @return un bonus selon la position des pièces sur le plateau
     */
//    public int KBK(long allyKing, long allyBishop, long opponentKing) {
//        int score = 0;
//
//        long targetCorner = getTargetCorner(allyBishop);
//        int distanceToCorner = calculateManhattanDistance(opponentKing, targetCorner);
//        score -= distanceToCorner * 10;
//
//        if (isBishopControllingCorner(allyBishop, targetCorner)) {
//            score += 50;
//        }
//
//        int distanceKings = calculateManhattanDistance(allyKing, opponentKing);
//        score += Math.max(0, 10 - distanceKings) * 5;
//
//        return score;
//    }

    /**
     * Utile pour les finales de types Roi - Tour versus Roi
     * Cela va permettre de forcer à la Tour de controller le maximum de cases
     * et pousser le roi dans un recoin afin de le bloquer.
     *
     * @param allyKing -> Roi
     * @param alliedRook -> Tour alliée
     * @param opponentKing -> Roi ennemi
     * @return un bonus selon la disposition des pièces
     */
//    public int KRK(long allyKing, long alliedRook, long opponentKing) {
//        int score = 0;
//
//        if (isRookControllingRowOrColumn(alliedRook, opponentKing)) {
//            score += 50;
//        }
//
//        int distanceKings = calculateManhattanDistance(allyKing, opponentKing);
//        score += Math.max(0, 10 - distanceKings) * 5;
//
//        if (!isKingOnEdge(opponentKing)) {
//            score -= 30;
//        }
//
//        return score;
//    }

    /**
     *Utile pour les finales de type King - Pawn  versus King
     * Cela va permettre de forcer la poussée des pions pour une promotion
     * Le tout en évitant de faire des Zugwangs
     *
     * @param allyKing -> Roi
     * @param pawn -> Pion allié
     * @param opponentKing -> Roi ennemi
     * @return un bonus selon la disposition des pièces sur le plateau
     */
//    public int KPK(long allyKing, long pawn, long opponentKing) {
//        int score = 0;
//
//        int distancePawnToPromotion = 7 - (int) (pawn / 8);
//        score -= distancePawnToPromotion * 10;
//
//        int distanceKings = calculateManhattanDistance(allyKing, opponentKing);
//        score += Math.max(0, 10 - distanceKings) * 5;
//
//        if (isKingBlockingPawn(opponentKing, pawn)) {
//            score -= 30;
//        }
//
//        return score;
//    }


    /**
     * Focntion qui compte le nombres de chaques pièces pour une couleur et associe le nombre
     * au score de chacune des pièces (c'est pas français mais tkt)
     *
     * @param color         -> couleur du joueur
     * @param colorBitboard -> le bitboard du joueur
     * @return un score associé au bitboard du joueur
     */
    public int countMaterial(int color, long colorBitboard) {
        int material = 0;
        if (color == 1) {
            material += Long.bitCount(engine.whitePawns & colorBitboard) * pawnValue;
            material += Long.bitCount(engine.whiteKnights & colorBitboard) * knightValue;
            material += Long.bitCount(engine.whiteBishops & colorBitboard) * bishopValue;
            material += Long.bitCount(engine.whiteRooks & colorBitboard) * rookValue;
            material += Long.bitCount(engine.whiteQueens & colorBitboard) * queenValue;
            material += Long.bitCount(engine.whiteKing & colorBitboard) * kingValue;
            if (engine.haveBishopPair(1)) material += bishopPairValue;
            if (engine.haveRookPair(1)) material += rookPairValue;
            if (engine.haveKnightPair(1)) material += knightPairValue;
        } else {
            material += Long.bitCount(engine.blackPawns & colorBitboard) * pawnValue;
            material += Long.bitCount(engine.blackKnights & colorBitboard) * knightValue;
            material += Long.bitCount(engine.blackBishops & colorBitboard) * bishopValue;
            material += Long.bitCount(engine.blackRooks & colorBitboard) * rookValue;
            material += Long.bitCount(engine.blackQueens & colorBitboard) * queenValue;
            material += Long.bitCount(engine.blackKing & colorBitboard) * kingValue;
            if (engine.haveBishopPair(-1)) material += bishopPairValue;
            if (engine.haveRookPair(-1)) material += rookPairValue;
            if (engine.haveKnightPair(-1)) material += knightPairValue;
        }
        return material;
    }

    /**
     * Je ne sais pas si c'est bien mais cela favorise
     * les pièces qui peuvent se mouvoir le plus
     *
     * @param color -> couleur du joueur
     * @return un bonus selon la possibilité de mouvements
     */
    public int movePossible(int color) {
        int score = 0;
        long occupied = engine.whitePieces() | engine.blackPieces();
        for (int sq = 0; sq < 64; sq++) {
            char piece = engine.getPieceAt(sq);
            if ((color == 1 && Character.isUpperCase(piece)) || (color == -1 && Character.isLowerCase(piece))) {
                int moves = engine.numberOfMoves(sq);
                switch (Character.toLowerCase(piece)) {
                    case 'n':
                        score += moves * 3;
                        break;
                    case 'b':
                        score += moves * 3;
                        break;
                    case 'r':
                        score += moves * 4;
                        if ((engine.getFileMaskForPawn(sq % 8) & (engine.whitePawns | engine.blackPawns)) == 0)
                            score += 10;
                        break;
                    case 'q':
                        score += moves * 2;
                        break;
                    case 'p':
                        score += moves * 1;
                        break;
                }
            }
        }
        return score;
    }

    /**
     * Retourne une valeur qui permet d'évaluer la structure des pions
     * Plus ils se défendent, plus ils ont de points
     *
     * @param pawn       -> matrice des pions
     * @param pawnAttack -> matrice d'attaque des pions
     * @return une valeur selon la défense des pions.
     */
    public int pawnStructure(long pawn, long pawnAttack) {
        return Long.bitCount(pawn & pawnAttack) * 5;
    }

    public int pawnDefense(long pawnAttack, long alliedPieces) {
        return Long.bitCount(pawnAttack & alliedPieces) * 5;
    }

    public int kingSafety(int color, long king, long oppositeBitboard, double phase) {
        int bonus = 0, penalty = 0;

        bonus += evaluatePawnShield(color, king, color == 1 ? engine.whitePawns : engine.blackPawns,
                color == 1 ? engine.whiteRooks : engine.blackRooks);

        int kingFile = Long.numberOfTrailingZeros(king) % 8;
        long fileMask = engine.getFileMask(king, color);
        long alliedPawns = color == 1 ? engine.whitePawns : engine.blackPawns;
        long enemyPawns = color == 1 ? engine.blackPawns : engine.whitePawns;
        if ((fileMask & alliedPawns) == 0) {
            penalty += (fileMask & enemyPawns) != 0 ? 15 : 25;
        }
        for (int offset = -1; offset <= 1; offset += 2) {
            if (kingFile + offset >= 0 && kingFile + offset < 8) {
                long adjFile = engine.getFileMaskForPawn(kingFile + offset);
                if ((adjFile & alliedPawns) == 0) penalty += 10;
            }
        }

        long enemyAttacks = color == 1 ? engine.blackAttacks : engine.whiteAttacks;
        long kingZone = color == 1 ? engine.whiteKingAttack : engine.blackKingAttack;
        int attackers = Long.bitCount(kingZone & enemyAttacks);
        penalty += attackers * (color == 1 ?
                Long.bitCount(enemyAttacks & (engine.blackQueens | engine.blackRooks)) * 5 + 2 :
                Long.bitCount(enemyAttacks & (engine.whiteQueens | engine.whiteRooks)) * 5 + 2);

        return (int) ((bonus - penalty) * phase);
    }

    /**
     * Regarde si au moins une pièce protège le roi,
     * si c'est le cas alors on attribut des points bonus
     *
     * @param color -> Couleur du joueur
     * @param king  -> position de notre roi
     * @return bonus si plusieurs pièces protègent notre roi
     */
    private double evaluateDefenders(int color, long king) {
        int defender = 0;
        engine.updateDefenseBitboards();
        defender += Long.bitCount(color == 1 ? king & engine.whitePawnsDefense : king & engine.blackPawnsDefense);
        defender += Long.bitCount(color == 1 ? king & engine.whiteRooksDefense : king & engine.blackRooksDefense);
        defender += Long.bitCount(color == 1 ? king & engine.whiteKnightsDefense : king & engine.blackKnightsDefense);
        defender += Long.bitCount(color == 1 ? king & engine.whiteQueensDefense : king & engine.blackQueensDefense);
        defender += Long.bitCount(color == 1 ? king & engine.whiteBishopsDefense : king & engine.blackBishopsDefense);
        return defender * 1.22;
    }

    /**
     * Regarde si une ligne au delà du roi est ouverte
     *
     * @param color -> couleur du roi
     * @param king  -> position du roi
     * @return une penalité pour chaque ligne ouverte
     */
    private int evaluateOpenFile(int color, long king) {
        int penalty = 0;
        int kingFile = Long.numberOfTrailingZeros(king) % 8;
        int kingPosition = Long.numberOfTrailingZeros(king) + 1;
        long fileMask = engine.getFileMask(king, color);
        boolean isOpenFile = (fileMask & (engine.whitePawns | engine.blackPawns)) == 0;

        if (isOpenFile) {
            penalty += 10;
        }
        return penalty;
    }

    /**
     * Retourne un malus de points si les pions sont doublés ou plus.
     *
     * @param pawns -> liste des pions d'un joueur
     * @return un malus selon le nombre de pions doublés ou plus
     */
    public int evaluateDoubledPawn(long pawns) {
        int malus = 0;

        for (int i = 0; i < 8; i++) {
            long fileMask = engine.getFileMaskForPawn(i);
            int pawnCount = Long.bitCount(fileMask & pawns);

            if (pawnCount >= 2) {
                malus += (pawnCount - 1) * 5;
            }
        }

        return -malus;
    }

    /**
     * Retourne un malus pour les pions isolés (sans pions sur les colonnes adjacentes pouvant le défendre)
     *
     * @param pawns -> pions du joueur
     * @return un malus selon la position des pions
     */
    public int evaluateIsolatedPawn(long pawns) {
        int malus = 0;

        for (int i = 0; i < 8; i++) {
            long fileMask = engine.getAdjacentMask(i);
            int pawnCount = Long.bitCount(fileMask & pawns);

            if (pawnCount >= 2) {
                malus += (pawnCount - 1) * 5;
            }
        }

        return -malus;
    }


    /**
     * Permet de regrader si des pions attaquent la zone du roi
     *
     * @param color            -> couleur du roi
     * @param king             -> position du roi
     * @param oppositeBitboard -> pièces ennemies
     * @return une penalité pour chaque case autour du roi attaqué
     */
    private int evaluateEnemyAttacks(int color, long king, long oppositeBitboard) {
        int penalty = 0;
        long enemyAttacks = color == 1 ? engine.blackAttacks : engine.whiteAttacks;
        long kingZone = color == 1 ? engine.whiteKingAttack : engine.blackKingAttack;
        long kingZoneUnderAttack = kingZone & enemyAttacks;
        int attackers = Long.bitCount(kingZoneUnderAttack);
        penalty += attackers * 2;
        return penalty;
    }

    /**
     * Permet de ragrarder la protection du roi avec les pions devant lui
     *
     * @param color -> couleur du roi
     * @param king  -> position du roi
     * @return une penalité si le roi n'est pas bien protégé
     */
    public int evaluatePawnShield(int color, long king, long alliedPawns, long alliedRooks) {
        int bonus = 0;

        if (color == 1) {
            long kingZoneFront = (king & ~Engine.FILE_8) << 8;
            long kingZoneFrontLeft = (king & ~Engine.FILE_A) << 7;
            long kingZoneFrontRight = (king & ~Engine.FILE_H) << 9;
            long kingZoneLeft = (king & ~Engine.FILE_A) >>> 1;
            long kingZoneRight = (king & ~Engine.FILE_H) << 1;
            long kingZone = kingZoneFrontLeft | kingZoneFront | kingZoneFrontRight;
            long kingZoneSide = kingZoneLeft | kingZoneRight;
            bonus += Long.bitCount(kingZone & alliedPawns);// * 5;
            bonus += Long.bitCount(kingZoneSide & alliedRooks);// * 2;
        } else {
            long kingZoneFront = (king & ~Engine.FILE_1) >>> 8;
            long kingZoneFrontLeft = (king & ~Engine.FILE_H) >>> 7;
            long kingZoneFrontRight = (king & ~Engine.FILE_A) >>> 9;
            long kingZoneLeft = (king & ~Engine.FILE_H) << 1;
            long kingZoneRight = (king & ~Engine.FILE_A) >>> 1;
            long kingZone = kingZoneFrontLeft | kingZoneFront | kingZoneFrontRight;
            long kingZoneSide = kingZoneLeft | kingZoneRight;
            bonus += Long.bitCount(kingZone & alliedPawns);// * 5;
            bonus += Long.bitCount(kingZoneSide & alliedRooks);// * 2;
        }
        return bonus;
    }


    /**
     * Fonction qui retourne une valeur selon la position des pièces (voir matrices en bas)
     *
     * @param color -> couleur du joueur
     * @return un score selon la disposition des pièces
     */
    public int countPositionalValue(int color, double phase) {
        int value = 0;
        for (int i = 0; i < 64; i++) {
            char piece = engine.getPieceAt(i);
            if (color == 1) {
                switch (piece) {
                    case 'P':
                        value += (int) (phase * PAWN_POSITION_VALUES[i] + (1 - phase) * PAWN_POSITION_END_VALUES[i]);
                        break;
                    case 'N':
                        value += KNIGHT_POSITION_VALUES[i];
                        break;
                    case 'B':
                        value += BISHOP_POSITION_VALUES[i];
                        break;
                    case 'R':
                        value += ROOK_POSITION_VALUES[i];
                        break;
                    case 'Q':
                        value += QUEEN_POSITION_VALUES[i];
                        break;
                    case 'K':
                        value += (int) (phase * KING_POSITION_VALUES[i] + (1 - phase) * KING_POSITION_END_VALUES[i]);
                        break;
                }
            } else {
                switch (piece) {
                    case 'p':
                        value += (int) (phase * PAWN_POSITION_VALUES_BLACK[i] + (1 - phase) * PAWN_POSITION_END_VALUES_BLACK[i]);
                        break;
                    case 'n':
                        value += KNIGHT_POSITION_VALUES_BLACK[i];
                        break;
                    case 'b':
                        value += BISHOP_POSITION_VALUES_BLACK[i];
                        break;
                    case 'r':
                        value += ROOK_POSITION_VALUES_BLACK[i];
                        break;
                    case 'q':
                        value += QUEEN_POSITION_VALUES_BLACK[i];
                        break;
                    case 'k':
                        value += (int) (phase * KING_POSITION_VALUES_BLACK[i] + (1 - phase) * KING_POSITION_END_VALUES_BLACK[i]);
                        break;
                }
            }
        }
        return value;
    }


    /**
     *Permet de savoir si on est en position de Zugzwang ou non
     *
     * @param color -> couleur du joueur
     * @return un boolean pour connaitre si le joueur est en position probable de zugzwang ou non.
     */
    public boolean onlyPawnsAndKing(int color) {
        long kingBB = (color == 1) ? engine.whiteKing : engine.blackKing;
        long pawnsBB = (color == 1) ? engine.whitePawns : engine.blackPawns;
        long otherPiecesBB = (color == 1) ? engine.whiteKnights | engine.whiteRooks | engine.whiteQueens | engine.whiteBishops | engine.whiteKing | engine.whitePawns : engine.blackPawns | engine.blackBishops | engine.blackRooks | engine.blackKing | engine.blackKnights | engine.blackQueens;


        return (kingBB | pawnsBB) == otherPiecesBB;
    }


    /**
     * Fonction principale pour la recherche Minimax avec élagage alpha-beta.
     * L'élagage alpha-beta est une optimisation de l'algorithme Minimax
     * qui permet de réduire le nombre de noeuds évalués dans l'arbre de recherche.
     * <p>
     * ISSUE: [FIXED]
     * Implementing TranspositionTable look like it play only blunders by itself
     * Idk if it comes from the Zobrist Hash that return duplicate key or from the TT
     * that's badly implemented.
     * It only calculate like ~200 nodes before giving me the result. And the Quiescence Search
     * is returned only after 2 nodes evaluated.
     * <p>
     * We'll see after some testing if it came from the Zobrist or the TT...
     * <p>
     * Test -> DONE : checking duplicated key
     * DONE : checking evaluated function without TT
     *
     * @param depth         -> La profondeur de la recherche.
     * @param depthFromRoot -> La profondeur actuelle à partir de la racine.
     * @param alpha         -> La valeur alpha pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur maximisant).
     * @param beta          -> La valeur beta pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur minimisant).
     * @return La meilleure évaluation trouvée.
     */
    public int Search(int depth, int depthFromRoot, int alpha, int beta) throws IOException {
        nodeCount++;

//        Integer syzygyScore = askSyzygy();
//        if (syzygyScore != null) {
//            System.out.println("Syzygy utilisé : " + syzygyScore); // Débogage
//            return syzygyScore;
//        }

        if (nodeCount % 100000 == 0) {
            applyGravity();
        }

        int static_eval = evaluate();
        int originalAlpha = alpha;

        /*  Recherche de Quiescence  */
        if (depth == 0) {
            return QuiescenceSearch(alpha, beta, 10);
        }


        /*  Table de Transposition (mémoïsation)  */
        long zobristKey = engine.getZobristKey();

        TranspositionTable.Entry ttEntry = transpositionTable.retrieve(zobristKey);
        if (ttEntry != null && ttEntry.depth >= depth) {
            ttHits++;
            if (ttEntry.type == TranspositionTable.EXACT) return ttEntry.score;
            if (ttEntry.type == TranspositionTable.LOWERBOUND) alpha = Math.max(alpha, ttEntry.score);
            if (ttEntry.type == TranspositionTable.UPPERBOUND) beta = Math.min(beta, ttEntry.score);
            if (alpha >= beta) return ttEntry.score;
        } else {
            ttMisses++;
        }

        /*  Null Move Pruning  */
        if (depth >= 3 && !engine.isKingInCheck(engine.ColorToMove) && static_eval >= beta && !onlyPawnsAndKing(engine.ColorToMove)) {
            int R = 2;
            engine.executeNullMove();
            int nullMoveEval = -Search(depth - 1 - R, depthFromRoot + 1, -beta, -beta + 1);
            engine.unmakeNullMove();
            if (nullMoveEval >= beta) {
                return beta;
            }
        }

        List<String> moves = engine.generateLegalMoves(engine.ColorToMove, false);
        orderMoves(moves, depth, ttEntry != null ? ttEntry.bestMove : null);

        if (moves.isEmpty()) {
            if (engine.isCheckmate(engine.ColorToMove)) {
                return -(mateScore - (depthFromRoot * 100));
            } else {
                return 0;
            }
        }

        /*  Futility Pruning  */
        boolean futilityPruning = false;
        int futilityMargin = 0;
        if (depth <= 3 && !engine.isKingInCheck(engine.ColorToMove)) {
            if (depth == 1) futilityMargin = 100;
            else if (depth == 2) futilityMargin = 300;
            else if (depth == 3) futilityMargin = 500;
            if (static_eval + futilityMargin <= alpha) {
                futilityPruning = true;
            }
        }

        String bestMove = null;
        int evaluation = 0;

        for (int i = 0; i < moves.size(); i++) {
            String move = moves.get(i);

            if (futilityPruning && i > 0 && !engine.isCapture(move) && !engine.isPromotion(move)) {
                continue;
            }

            engine.moveHistory.push(new MoveState(engine));
            engine.executeMove(move);

            /*  Late Move Reduction  */
            int newDepth = depth - 1;

            double moveRatio = (double) i / moves.size();
            if (moves.size() > 3 && depth > 4) {
                if (moveRatio <= 1.0 / 3) newDepth -= 1;
                else if (moveRatio <= 2.0 / 3) newDepth -= 3;
                else newDepth -= 4;
            }


            /*  Principal Variation Search  */
            if (i == 0) {
                evaluation = -Search(newDepth, depthFromRoot + 1, -beta, -alpha);
            } else {
                evaluation = -Search(newDepth, depthFromRoot + 1, -alpha - 1, -alpha);
                if (evaluation > alpha && evaluation < beta) {
                    evaluation = -Search(newDepth, depthFromRoot + 1, -beta, -alpha);
                }
            }

            engine.unmakeMove();

            /*  Stockage des Killer Moves & des Table d'historique  */
            if (evaluation >= beta) {
                if (!engine.isCapture(move)) {
                    storeKillerMove(move, depth);
//                    int from = engine.squareToIndex(move.substring(0, 2));
//                    int to = engine.squareToIndex(move.substring(2, 4));
//                    //historyTable[from][to] += depth * depth;
//                    historyTable[from][to] += 10 + depth;
//                    historyTable[from][to] = Math.min(historyTable[from][to], 10000);
                    int from = engine.squareToIndex(move.substring(0, 2));
                    int to = engine.squareToIndex(move.substring(2, 4));
                    int pieceType = getPieceTypeIndex(engine.getPieceAt(from));
                    historyTable[pieceType][from][to] += 10 + depth;
                    historyTable[pieceType][from][to] = Math.min(historyTable[pieceType][from][to], 10000);
                }
                transpositionTable.store(zobristKey, depth, beta, TranspositionTable.LOWERBOUND, move);
                return beta;
            }

            if (evaluation > alpha) {
                alpha = evaluation;
                bestMove = move;
            }
        }

        int type = (alpha > originalAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPERBOUND;
        transpositionTable.store(zobristKey, depth, alpha, type, bestMove);
        return alpha;
    }


    private void storeKillerMove(String move, int depth) {
        if (killerMoves[depth][0] == null || !killerMoves[depth][0].equals(move)) {
            killerMoves[depth][1] = killerMoves[depth][0];
            killerMoves[depth][0] = move;
        }
    }

//    private void applyGravity() {
//        for (int i = 0; i < 64; i++) {
//            for (int j = 0; j < 64; j++) {
//                historyTable[i][j] /= 2;
//                // test1 : historyTable[i][j] = (int)(historyTable[i][j] * 0.75);
//                // test2 : historyTable[i][j] = Math.max(0, historyTable[i][j] - 50);
//            }
//        }
//    }

    private void applyGravity() {
        for (int piece = 0; piece < 6; piece++) {
            for (int i = 0; i < 64; i++) {
                for (int j = 0; j < 64; j++) {
                    historyTable[piece][i][j] /= 2;
                }
            }
        }
    }


    /**
     * Fonction de recherche de quiétude utilisée pour évaluer les positions jusqu'à une certaine profondeur
     * tout en s'assurant que seules les captures sont considérées.
     *
     * @param alpha -> La valeur alpha pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur maximisant).
     * @param beta  -> La valeur beta pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur minimisant).
     * @return La meilleure évaluation trouvée.
     */
    public int QuiescenceSearch(int alpha, int beta, int depth) {
        nodeCount++;
        quiescenceNodeCount++;
        int originalAlpha = alpha;

        if(depth == 0) return evaluate();

        /*  Table de Transposition  */
        long zobristKey = engine.getZobristKey();

        TranspositionTable.Entry ttEntry = transpositionTableQS.retrieve(zobristKey);

        if (ttEntry != null && ttEntry.zobristKey == zobristKey && ttEntry.depth >= depth) {
            ttHits++;
            if (ttEntry.type == TranspositionTable.EXACT) return ttEntry.score;
            if (ttEntry.type == TranspositionTable.LOWERBOUND) alpha = Math.max(alpha, ttEntry.score);
            if (ttEntry.type == TranspositionTable.UPPERBOUND) beta = Math.min(beta, ttEntry.score);
            if (alpha >= beta) {
                return ttEntry.score;
            }
        } else {
            ttMisses++;
        }

        String bestMove = null;

        int evaluation = evaluate();

        /* Delta Pruning  */
//        int deltaMargin = 800;
//        if (evaluation + deltaMargin < alpha) {
//            int type = (alpha > originalAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPERBOUND;
//            transpositionTableQS.store(zobristKey, depth, alpha, type, bestMove);
//            return alpha;
//        }

        if (evaluation >= beta) {
            transpositionTableQS.store(zobristKey, depth, beta, TranspositionTable.LOWERBOUND, null);
            return beta;
        }

        if (evaluation > alpha) alpha = evaluation;

        List<String> captureMoves = engine.generateLegalMoves(engine.ColorToMove, true);
        orderMoves(captureMoves, depth, ttEntry != null ? ttEntry.bestMove : null);

        for (String move : captureMoves) {
            engine.moveHistory.push(new MoveState(engine));
            engine.executeMove(move);

            evaluation = -QuiescenceSearch(-beta, -alpha, depth - 1);
            engine.unmakeMove();

            if (evaluation >= beta) {
                transpositionTableQS.store(zobristKey, depth, beta, TranspositionTable.LOWERBOUND, move);
                return beta;
            }
            if (evaluation > alpha) {
                alpha = evaluation;
                bestMove = move;
            }
        }

        int type = (alpha > originalAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPERBOUND;
        transpositionTableQS.store(zobristKey, depth, alpha, type, bestMove);

        return alpha;
    }


    /**[OLD]
     * Fonction pour trouver le meilleur coup à jouer en utilisant l'algorithme Minimax avec élagage alpha-beta.
     * Dans cette version threadée, on va diviser le noeud père en sous-arbres pour accélérer la recherche.
     * <p>
     * De ce fait, dans notre liste de mouvements légaux, nous allons les triés selon le possibilités
     * d'être bon. (orderMoves)
     * <p>
     * ENsuite la liste de coups va être séparé entre les threads.
     * Pour accélérer la recherche, j'ai décidé de donner un des meilleurs coups à chacun des threads afin
     * d'avoir un élagage le plus tôt possible pour chacun d'entre eux.
     * Ex: liste coup trié [e2e4, a5b6, c7c8, b3d5, f6h7, a1a8]
     * AU lieu de séparer simplement la liste en 3 parts égales (nb choisi arbitrairement pour l'exemple),
     * On va donner successivement les coups aux différents threads.
     * Cela réduits sensiblement le temps de recherche total
     * <p>
     * Un autre problème était, comme vu en SYstème Distribué, la loi d'Amdhal;
     * En effet, lors des tests j'ai mis le maximum de threads possibles en me disant que cela allait être rapide...
     * AU final, on remarque que s'il y a 20 coups ou moins, rien ne sert de threader et au plus, on peut mettre 4 threads afin
     * de ne pas réduire le nombre de nodes évaluées à la seconde;
     * <p>
     * //21 coups
     * //Depth 6 et 4 threads = 1 100 000 NPS
     * //Depth 6 et 3 threads = 1 290 000 NPS
     * //Depth 6 et 2 threads = 1 289 000 NPS
     * //Depth 6 et 1 thread  = 1 441 000 NPS
     * <p>
     * //Depth 7 et 4 threads = 1 376 000 NPS
     * //Depth 7 et 3 threads = 1 292 000 NPS
     * //Depth 7 et 2 threads = 1 290 000 NPS
     * //Depth 7 et 1 thread  = 1 278 000 NPS
     *
     * @param timeOfSearch -> Temps de recherche
     * @return Le meilleur coup trouvé.
     */
    public String findBestMove(int timeOfSearch) throws IOException {
        ttMisses = 0;
        ttHits = 0;
        ttMissesQS = 0;
        ttHitsQS = 0;
        nodeCount = 0;
        quiescenceNodeCount = 0;
        time = 0.0;
        startTime = System.nanoTime();
        int alpha = this.negativeInfinity;
        int beta = this.positiveInfinity;
        String bestMove = null;

//        Integer syzygyScore = askSyzygy();
//        if (syzygyScore != null) {
//            System.out.println("Syzygy score à la racine : " + syzygyScore);
//            List<String> movess = engine.generateLegalMoves(engine.ColorToMove, false);
//            System.out.println(movess);
//            for (String move : movess) {
//                System.out.println("Tentative de coup : " + move);
//                engine.moveHistory.push(new MoveState(engine));
//                engine.executeMove(move);
//                Integer childScore = askSyzygy();
//                engine.unmakeMove();
//                if (childScore != null && childScore.equals(syzygyScore)) {
//                    return move;
//                }
//            }
//        }


        String currentPosition = engine.getFEN();
        if (bookOpening.hasOpeningMoves(currentPosition)) {
            return bookOpening.getRecommendedMove(currentPosition);
        }

        List<String> moves = engine.generateLegalMoves(engine.ColorToMove, false);
        orderMoves(moves, 0, null);
        System.out.println(moves);

        // Vérifier Syzygy à la racine
//        int totalPieces = Long.bitCount(engine.whitePieces() | engine.blackPieces());
//        if (totalPieces <= MAX_PIECES) {
//            Integer syzygyScore = askSyzygy();
//            if (syzygyScore != null) {
//                System.out.println("Syzygy score à la racine : " + syzygyScore); // Débogage
//                SyzygyResult dtzResult = syzygy.probeDTZ(engine.getFEN());
//                if (dtzResult != null && (dtzResult.wdl.equals("WIN") || dtzResult.wdl.equals("CURSED_WIN"))) {
//                    String bestSyzygyMove = null;
//                    int minDTZ = Integer.MAX_VALUE;
//                    for (String move : moves) {
//                        engine.moveHistory.push(new MoveState(engine));
//                        engine.executeMove(move);
//                        String fenAfter = engine.getFEN();
//                        SyzygyResult nextResult = syzygy.probeDTZ(fenAfter);
//                        engine.unmakeMove();
//                        if (nextResult != null && nextResult.dtz < minDTZ) {
//                            minDTZ = nextResult.dtz;
//                            bestSyzygyMove = move;
//                        }
//                    }
//                    if (bestSyzygyMove != null) {
//                        System.out.println("Syzygy meilleur coup : " + bestSyzygyMove + ", DTZ : " + minDTZ);
//                        return bestSyzygyMove;
//                    }
//                }
//                // Si pas de DTZ ou match nul, retourner un coup par défaut
//                return moves.get(0);
//            }
//        }

//        Integer syzygyScore = askSyzygy();
//        if (syzygyScore != null) {
//            System.out.println("Syzygy score à la racine : " + syzygyScore);
//            // Ne pas appeler probeDTZ ici pour l'instant
//            // Trouver le meilleur coup parmi les coups légaux
//            for (String move : moves) {
//                engine.makeMove(move);
//                Integer childScore = askSyzygy();
//                engine.unmakeMove();
//                if (childScore != null && childScore.equals(syzygyScore)) {
//                    return move; // Retourne un coup qui conserve le score Syzygy
//                }
//            }
//        }

        // Recherche itérative si Syzygy ne s'applique pas
        String bestMoveAtDepth = null;
        int bestEvaluation = this.negativeInfinity + 100;

        for (int depth = 1; depth <= 100; depth++) {
            for (String move : moves) {
                engine.moveHistory.push(new MoveState(engine));
                engine.executeMove(move);
                int evaluation = -Search(depth - 1, 0, -beta, -alpha);
                engine.unmakeMove();

                if (evaluation > bestEvaluation) {
                    bestEvaluation = evaluation;
                    bestMoveAtDepth = move;
                }
                if (bestEvaluation > alpha) {
                    alpha = bestEvaluation;
                }
                if (alpha >= beta) {
                    break;
                }

                double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
                if (elapsedTime >= timeOfSearch) {
                    System.out.println("Temps écoulé, arrêt à profondeur " + depth);
                    System.out.println("Score " + (float) bestEvaluation/512 + " not normalized " + bestEvaluation );
                    System.out.println("TT Hits " + ttHits + " : TT Misses " + ttMisses);
                    System.out.println("%TT " + transpositionTable.getMemoryUsagePercentage() + " : %TTQS " + transpositionTableQS.getMemoryUsagePercentage());
                    return bestMove != null ? bestMove : bestMoveAtDepth;
                }
            }

            if (bestMoveAtDepth != null) {
                bestMove = bestMoveAtDepth;
                System.out.println("Profondeur " + depth + " : Meilleur coup = " + bestMove + ", Score = " + (float) bestEvaluation/512 + " not normalized " + bestEvaluation);
            }
        }
        return bestMove;
    }


    /**
     * Même fonction que findBestMove mais celle-ci retourne les 10 "meilleurs" coups
     *
     * @param timeOfSearch -> profondeur de recherche
     * @return la liste des 10 meilleurs coups
     */
    public List<String> findListBestMoves(int timeOfSearch) throws IOException {
        ttMisses = 0;
        ttHits = 0;
        ttMissesQS = 0;
        ttHitsQS = 0;
        nodeCount = 0;
        quiescenceNodeCount = 0;
        time = 0.0;
        startTime = System.nanoTime();
        int alpha = this.negativeInfinity;
        int beta = this.positiveInfinity;

        String currentPosition = engine.getFEN();
        if (bookOpening.hasOpeningMoves(currentPosition)) {
            List<String> openingMove = new ArrayList<>();
            openingMove.add(bookOpening.getRecommendedMove(currentPosition));
            return openingMove;
        }

        List<String> moves = engine.generateLegalMoves(engine.ColorToMove, false);
        orderMoves(moves, 0, null);
        System.out.println("Coups générés : " + moves);

        List<MoveEvaluation> moveEvaluations = new ArrayList<>();

        for (int depth = 1; depth <= 100; depth++) {
            moveEvaluations.clear();
            int bestEvaluation = this.negativeInfinity + 100;

            for (String move : moves) {
                engine.moveHistory.push(new MoveState(engine));
                engine.executeMove(move);
                int evaluation = -Search(depth - 1, 0, -beta, -alpha);
                engine.unmakeMove();

                moveEvaluations.add(new MoveEvaluation(move, evaluation));

                if (evaluation > bestEvaluation) {
                    bestEvaluation = evaluation;
                }
                if (bestEvaluation > alpha) {
                    alpha = bestEvaluation;
                }
                if (alpha >= beta) {
                    break;
                }

                double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
                if (elapsedTime >= timeOfSearch) {
                    System.out.println("Temps écoulé, arrêt à profondeur " + depth);
                    return finalizeMoveList(moveEvaluations, depth);
                }
            }
        }
        return finalizeMoveList(moveEvaluations, 100);
    }


    /**
     * Permet de trier une liste de coups dans un ordre précis (voir getScoreMoveGuess())
     *
     * @param moves -> liste de coups à trier
     */
    public void orderMoves(List<String> moves, int depth, String ttMove) {
        Map<String, Integer> moveScores = new HashMap<>();

        for (String move : moves) {

            int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');

            int score = 0;

            // 1. Coup de la table de transposition (Priorité absolue)
            if (move.equals(ttMove)) {
                score += 100_000;
                moveScores.put(move, score);
                continue;
            }

            // 2. Captures : MVV-LVA + SEE
            if (engine.isCapture(move)) {
                score += evaluateCapture(move);
                score += getSEE(move) * 10; // SEE pondéré
                moveScores.put(move, score);
                continue;
            }


            // 3. Killer Moves (historique des bons coups non-captures)
            if (!engine.isCapture(move)) {
                if (killerMoves[depth][0] != null && killerMoves[depth][0].equals(move)) {
                    score += 9500;
                    moveScores.put(move, score);
                    continue;
                } else if (killerMoves[depth][1] != null && killerMoves[depth][1].equals(move)) {
                    score += 9000;
                    moveScores.put(move, score);
                    continue;
                }
            }

            // 4. Promotions (les promotions en Dame sont prioritaires)
            if (engine.isPromotion(move)) {
                if (move.endsWith("Q") || move.endsWith("q")) {
                    score += 8500; // Promotion en Dame
                    moveScores.put(move, score);
                    continue;
                } else {
                    score += 5000; // Autres promotions
                    moveScores.put(move, score);
                    continue;
                }
            }

            // 4. Coups donnant échec (coûte trop cher)
//            if (engine.givesCheck(move)) {
//                score += 2000; // Échecs = haute priorité
//            }

            //score += getPieceValue(engine.getPieceAt(from));

            // 5. History Heuristic (priorise les coups ayant souvent bien fonctionné)
            //score += historyTable[engine.squareToIndex(move.substring(0, 2))][engine.getDestinationSquare(move)];
            //score += historyTable[engine.squareToIndex(move.substring(0, 2))][engine.getDestinationSquare(move)] / 10;
            score += historyTable[getPieceTypeIndex(engine.getPieceAt(from))][engine.squareToIndex(move.substring(0, 2))][engine.getDestinationSquare(move)] / 10;
            moveScores.put(move, score);
        }
        moves.sort((m1, m2) -> Integer.compare(moveScores.get(m2), moveScores.get(m1)));
    }

    private int getPieceTypeIndex(char piece) {
        switch (Character.toLowerCase(piece)) {
            case 'p': return 0;
            case 'n': return 1;
            case 'b': return 2;
            case 'r': return 3;
            case 'q': return 4;
            case 'k': return 5;
            default: return -1;
        }
    }

    /**
     * Permet d'évaluer une série d'échange de pièces
     *
     * @param move -> coup joué
     * @return un entier évaluant la série d'échange
     */
    public int getSEE(String move) {
        int targetSquare = engine.getDestinationSquare(move);
        int pieceValue = getPieceValue(engine.getPieceAt(targetSquare));

        List<Integer> attackers = engine.getAttackers(targetSquare, engine.ColorToMove);

        if (attackers.isEmpty()) return 0; // Pas d'attaquants, pas d'échange

        Collections.sort(attackers, (a, b) -> Integer.compare(getPieceValue(engine.getPieceAt(a)), getPieceValue(engine.getPieceAt(b))));

        int balance = 0;
        boolean isWhiteTurn = engine.ColorToMove == 1;

        while (!attackers.isEmpty()) {
            int attacker = attackers.remove(0);
            balance = pieceValue - balance;
            if (balance < 0) break;
            pieceValue = getPieceValue(engine.getPieceAt(attacker));
        }

        return balance;
    }

    /**
     * On trie selon les scores suivant:
     * Echec et Mat > Capture > Promotions > Autres
     *
     * @param move -> coup à evaluer
     * @return un score associé au coup
     */
    private int getMoveScoreGuess(String move, int depth, String bestMove) {
        int score = 0;
        if (move.equals(bestMove)) score += 100000;

        if (engine.isCapture(move)) {
            score += evaluateCapture(move);
            score += getSEE(move) * 10;
        }

        if (!engine.isCapture(move)) {
            if (killerMoves[depth][0] != null && killerMoves[depth][0].equals(move)) score += 10000;
            if (killerMoves[depth][1] != null && killerMoves[depth][1].equals(move)) score += 8000;
        }

        if (engine.isPromotion(move)) {
            if (move.endsWith("Q")) {
                score += 9000;
            } else {
                score += 5000;
            }
        }
        score += historyTable2[engine.squareToIndex(move.substring(0, 2))][engine.getDestinationSquare(move)];

        return score;
    }

    /**
     * Focntion permettant d'évaluer une capture en prenant en compte la pièce qui capture et celle capturée
     *
     * @param move -> coup joué
     * @return un score associé à la capture
     */
    private int evaluateCapture(String move) {
        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        char s = engine.getPieceAt(from);
        char d = engine.getPieceAt(to);
        //Q take n == 10000 + (300 - 800) = 10000 - 500 = 9500
        //p take Q == 10000 + 800 - 100 = 10000 + 900 = 10900
        return 10000 + (getPieceValue(d) - getPieceValue(s));
    }

    public int pieceValue(char piece) {
        switch (piece) {
            case 'p':
            case 'P':
                return pawnValue;
            case 'n':
            case 'N':
                return knightValue;
            case 'b':
            case 'B':
                return bishopValue;
            case 'r':
            case 'R':
                return rookValue;
            case 'q':
            case 'Q':
                return queenValue;
            case 'k':
            case 'K':
                return kingValue;
            default:
                return 0;
        }
    }


    private double getPositionValue(String move, int color) {
        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');


        char piece = engine.getPieceAt(to);
        double value;
        switch (piece) {
            case 'P':
                value = PAWN_POSITION_VALUES[to];
                break;
            case 'p':
                value = PAWN_POSITION_VALUES_BLACK[to];
                break;
            case 'B':
                value = BISHOP_POSITION_VALUES[to];
                break;
            case 'b':
                value = BISHOP_POSITION_VALUES_BLACK[to];
                break;
            case 'N':
                value = KNIGHT_POSITION_VALUES[to];
                break;
            case 'n':
                value = KNIGHT_POSITION_VALUES_BLACK[to];
                break;
            case 'R':
                value = ROOK_POSITION_VALUES[to];
                break;
            case 'r':
                value = ROOK_POSITION_VALUES_BLACK[to];
                break;
            case 'Q':
                value = QUEEN_POSITION_VALUES[to];
                break;
            case 'q':
                value = QUEEN_POSITION_VALUES_BLACK[to];
                break;
            case 'K':
                value = engine.nbMoveTotal >= 20 ? KING_POSITION_END_VALUES[to] : KING_POSITION_VALUES[to];
                break;
            case 'k':
                value = engine.nbMoveTotal >= 20 ? KING_POSITION_END_VALUES_BLACK[to] : KING_POSITION_VALUES_BLACK[to];
                break;
            default:
                value = 0;
                break;
        }
        //value += isPieceInDanger(to, color) ? value - getPieceValue(engine.getPieceAt(from)) : value;
        //System.out.println("Move: " + move + ", Piece: " + piece + ", Position Value: " + value);
        return value;
    }


    private boolean isPieceInDanger(int position, int color) {
        long piecePosition = 1L << position;
        long opponentPieces = (color == 1) ? engine.blackPieces() : engine.whitePieces();

        return isAttackedBySlidingPieces(piecePosition, color) ||
                isAttackedByKnights(piecePosition, color) ||
                isAttackedByPawns(piecePosition, color) ||
                isAttackedByKing(piecePosition, color);
    }

    public int getPieceValue(char piece) {
        switch (piece) {
            case 'Q':
            case 'q':
                return queenValue;
            case 'R':
            case 'r':
                return rookValue;
            case 'B':
            case 'b':
                return bishopValue;
            case 'N':
            case 'n':
                return knightValue;
            case 'P':
            case 'p':
                return pawnValue;
            default:
                return 0;
        }
    }

    private int[] convertBoardToNNUEArray() {
        int[] pieceBoard = new int[64];

        for (int square = 0; square < 64; square++) {
            char piece = engine.getPieceAt(square);
            switch (piece) {
                case 'P': pieceBoard[square] = 1; break;
                case 'N': pieceBoard[square] = 2; break;
                case 'B': pieceBoard[square] = 3; break;
                case 'R': pieceBoard[square] = 4; break;
                case 'Q': pieceBoard[square] = 5; break;
                case 'K': pieceBoard[square] = 6; break;

                case 'p': pieceBoard[square] = 9; break;
                case 'n': pieceBoard[square] = 10; break;
                case 'b': pieceBoard[square] = 11; break;
                case 'r': pieceBoard[square] = 12; break;
                case 'q': pieceBoard[square] = 13; break;
                case 'k': pieceBoard[square] = 14; break;

                default: pieceBoard[square] = 0; break;
            }
        }
        return pieceBoard;
    }

    private int convertBoardForFasterEval(int[] pieceBoard, int[] pieces, int[] squares) {
        int index = 0;
        for (int square = 0; square < 64; square++) {
            if (pieceBoard[square] != 0) {
                pieces[index] = pieceBoard[square];
                squares[index] = square;
                index++;
            }
        }
        return index;
    }



    private boolean isAttackedBySlidingPieces(long position, int color) {
        long rooksAndQueens = (color == 1) ? (engine.blackRooks | engine.blackQueens) : (engine.whiteRooks | engine.whiteQueens);
        long bishopsAndQueens = (color == 1) ? (engine.blackBishops | engine.blackQueens) : (engine.whiteBishops | engine.whiteQueens);

        return engine.isSlidingPieceAttacking(position, rooksAndQueens, new int[]{1, -1, 8, -8}) ||
                engine.isSlidingPieceAttacking(position, bishopsAndQueens, new int[]{7, -7, 9, -9});
    }

    private boolean isAttackedByKnights(long position, int color) {
        long knights = (color == 1) ? engine.blackKnights : engine.whiteKnights;
        return engine.isKnightAttacking(position, knights);
    }

    private boolean isAttackedByPawns(long position, int color) {
        long pawns = (color == 1) ? engine.blackPawns : engine.whitePawns;
        return engine.isPawnAttacking(position, pawns, color);
    }

    private boolean isAttackedByKing(long position, int color) {
        long king = (color == 1) ? engine.blackKing : engine.whiteKing;
        return engine.isKingAttacking(position, king);
    }

    public static final int[] KING_POSITION_VALUES = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    public static final int[] KING_POSITION_END_VALUES = {
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
    };


    public static final int[] KING_POSITION_END_VALUES_BLACK = {
            -50, -30, -30, -30, -30, -30, -30, -50,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -50, -40, -30, -20, -20, -30, -40, -50
    };

    public static final int[] KING_POSITION_VALUES_BLACK = {
            20, 30, 10, 0, 0, 10, 30, 20,
            20, 20, 0, 0, 0, 0, 20, 20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30
    };


    public static final int[] QUEEN_POSITION_VALUES = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -5, 0, 5, 5, 5, 5, 0, -5,
            0, 0, 5, 5, 5, 5, 0, -5,
            -10, 5, 5, 5, 5, 5, 0, -10,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -20, -10, -10, -5, -5, -10, -10, -20
    };

    public static final int[] QUEEN_POSITION_VALUES_BLACK = {
            -20, -10, -10, -5, -5, -10, -10, -20,
            -10, 0, 5, 0, 0, 0, 0, -10,
            -10, 5, 5, 5, 5, 5, 0, -10,
            0, 0, 5, 5, 5, 5, 0, -5,
            -5, 0, 5, 5, 5, 5, 0, -5,
            -10, 0, 5, 5, 5, 5, 0, -10,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -20, -10, -10, -50, -50, -10, -10, -20
    };


    public static final int[] ROOK_POSITION_VALUES = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, 10, 10, 10, 10, 5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            0, 0, 0, 5, 5, 0, 0, 0
    };

    public static final int[] ROOK_POSITION_VALUES_BLACK = {
            0, 0, 0, 5, 5, 0, 0, 0,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            -5, 0, 0, 0, 0, 0, 0, -5,
            5, 10, 10, 10, 10, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };


    public static final int[] BISHOP_POSITION_VALUES = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    public static final int[] BISHOP_POSITION_VALUES_BLACK = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };


    public static final int[] KNIGHT_POSITION_VALUES = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 25, 15, 15, 25, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    public static final int[] KNIGHT_POSITION_VALUES_BLACK = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -30, 5, 25, 15, 15, 25, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    public static final int[] PAWN_POSITION_VALUES = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 25, 30, 30, 25, 10, 10,
            5, 5, 15, 25, 25, 15, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    public static final int[] PAWN_POSITION_VALUES_BLACK = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 10, 10, -20, -20, 10, 10, 5,
            5, -5, -10, 0, 0, -10, -5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, 5, 15, 25, 25, 15, 5, 5,
            10, 10, 25, 30, 30, 25, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    public static final int[] PAWN_POSITION_END_VALUES = {
            0, 0, 0, 0, 0, 0, 0, 0,
            80, 80, 80, 80, 80, 80, 80, 80,
            70, 70, 70, 70, 70, 75, 70, 70,
            60, 60, 60, 60, 60, 60, 60, 60,
            50, 50, 50, 50, 50, 50, 50, 50,
            40, 40, 40, 40, 40, 40, 40, 40,
            5, 5, 5, 5, 5, 5, 5, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    public static final int[] PAWN_POSITION_END_VALUES_BLACK = {
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 5, 5, 5, 5, 5, 5, 5,
            40, 40, 40, 40, 40, 40, 40, 40,
            50, 50, 50, 50, 50, 50, 50, 50,
            60, 60, 60, 60, 60, 60, 60, 60,
            70, 70, 70, 70, 70, 70, 70, 70,
            80, 80, 80, 80, 80, 80, 80, 80,
            0, 0, 0, 0, 0, 0, 0, 0
    };


    //__________________________________________________________________________________________________________________
    //MODE DE JEU CAPTURE ONLY

    public String findBestMoveCaptureOnly(int timeOfSearch) {
        ttMisses = 0;
        ttHits = 0;
        ttMissesQS = 0;
        ttHitsQS = 0;
        nodeCount = 0;
        quiescenceNodeCount = 0;
        time = 0.0;
        startTime = System.nanoTime();
        int alpha = this.negativeInfinity;
        int beta = this.positiveInfinity;
        String bestMove = null;

        List<String> moves = engine.generateLegalMovesCaptureOnlyGameMode(engine.ColorToMove, false);
        orderMoves(moves, 0, null);
        System.out.println(moves);
        String bestMoveAtDepth = null;
        int bestEvaluation = this.negativeInfinity + 100;

        for (int depth = 1; depth <= 100; depth++) {

            for (String move : moves) {
                engine.moveHistory.push(new MoveState(engine));
                engine.executeMoveCaptureOnlyGameMode(move);
                int evaluation = -SearchCaptureOnlyGameMode(depth - 1, 0, -beta, -alpha);
                engine.unmakeMoveCaptureOnlyGameMode();
                if (evaluation > bestEvaluation) {
                    bestEvaluation = evaluation;
                    bestMoveAtDepth = move;
                }

                if (bestEvaluation > alpha) {
                    alpha = bestEvaluation;
                }

                if (alpha >= beta) {
                    break;
                }

                double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
                if (elapsedTime >= timeOfSearch) {
                    System.out.println("New Temps écoulé, arrêt à profondeur " + depth);
                    System.out.println("Score " + bestEvaluation);
                    System.out.println("TT Hits " + ttHits + " : TT Misses " + ttMisses + "\nTT percentage: " + transpositionTable.getMemoryUsagePercentage() + "%\nTT QS percentage: " + transpositionTableQS.getMemoryUsagePercentage() + "%");
                    return bestMove != null ? bestMove : bestMoveAtDepth;
                }

            }

            if (bestMoveAtDepth != null) {
                bestMove = bestMoveAtDepth;
                System.out.println("Profondeur " + depth + " : Meilleur coup = " + bestMove + ", Score = " + bestEvaluation);
            }
        }
        return bestMove;
    }

    public int SearchCaptureOnlyGameMode(int depth, int depthFromRoot, int alpha, int beta) {
        nodeCount++;

        int static_eval = evaluate();
        int originalAlpha = alpha;


        /*  Recherche de Quiescence  */
        if (depth == 0) {
            return evaluateCaptureOnlyGameMode();
        }


        /*  Table de Transposition (mémoïsation)  */
        long zobristKey = engine.getZobristKey();

        TranspositionTable.Entry ttEntry = transpositionTable.retrieve(zobristKey);
        if (ttEntry != null && ttEntry.depth >= depth) {
            ttHits++;
            if (ttEntry.type == TranspositionTable.EXACT) return ttEntry.score;
            if (ttEntry.type == TranspositionTable.LOWERBOUND) alpha = Math.max(alpha, ttEntry.score);
            if (ttEntry.type == TranspositionTable.UPPERBOUND) beta = Math.min(beta, ttEntry.score);
            if (alpha >= beta) return ttEntry.score;
        } else {
            ttMisses++;
        }

        /*  Null Move Pruning  */
        if (depth >= 3 && !engine.isKingInCheck(engine.ColorToMove) && static_eval >= beta && !onlyPawnsAndKing(engine.ColorToMove)) {
            int R = 2;
            engine.executeNullMove();
            int nullMoveEval = -SearchCaptureOnlyGameMode(depth - 1 - R, depthFromRoot + 1, -beta, -beta + 1);
            engine.unmakeNullMove();
            if (nullMoveEval >= beta) {
                return beta;
            }
        }

        List<String> moves = engine.generateLegalMoves(engine.ColorToMove, false);
        orderMoves(moves, depth, ttEntry != null ? ttEntry.bestMove : null);

        if (moves.isEmpty()) {
            if (engine.isCheckmate(engine.ColorToMove)) {
                return -(mateScore - (depthFromRoot * 100));
            } else {
                return 0;
            }
        }

        /*  Futility Pruning  */
        boolean futilityPruning = false;
        int futilityMargin = 0;
        if (depth <= 3 && !engine.isKingInCheck(engine.ColorToMove)) {
            if (depth == 1) futilityMargin = 100;
            else if (depth == 2) futilityMargin = 300;
            else if (depth == 3) futilityMargin = 500;
            if (static_eval + futilityMargin <= alpha) {
                futilityPruning = true;
            }
        }

        String bestMove = null;
        int evaluation = 0;

        for (int i = 0; i < moves.size(); i++) {
            String move = moves.get(i);

            if (futilityPruning && i > 0 && !engine.isCapture(move) && !engine.isPromotion(move)) {
                continue;
            }

            engine.moveHistory.push(new MoveState(engine));
            engine.executeMove(move);

            /*  Late Move Reduction  */
            int newDepth = depth - 1;

            double moveRatio = (double) i / moves.size();
            if (moves.size() > 4 && depth > 4) {
                if (moveRatio <= 1.0 / 3) newDepth -= 1;
                else if (moveRatio <= 2.0 / 3) newDepth -= 3;
                else newDepth -= 4;
            }


            /*  Principal Variation Search  */
            if (i == 0) {
                evaluation = -SearchCaptureOnlyGameMode(newDepth, depthFromRoot + 1, -beta, -alpha);
            } else {
                evaluation = -SearchCaptureOnlyGameMode(newDepth, depthFromRoot + 1, -alpha - 1, -alpha);
                if (evaluation > alpha && evaluation < beta) {
                    evaluation = -SearchCaptureOnlyGameMode(newDepth, depthFromRoot + 1, -beta, -alpha);
                }
            }

            engine.unmakeMove();

            /*  Stockage des Killer Moves & des Table d'historique  */
            if (evaluation >= beta) {
                if (!engine.isCapture(move)) {
                    storeKillerMove(move, depth);
                    int from = engine.squareToIndex(move.substring(0, 2));
                    int to = engine.squareToIndex(move.substring(2, 4));
                    historyTable2[from][to] += depth * depth;
                }
                transpositionTable.store(zobristKey, depth, beta, TranspositionTable.LOWERBOUND, move);
                return beta;
            }

            if (evaluation > alpha) {
                alpha = evaluation;
                bestMove = move;
            }
        }

        int type = (alpha > originalAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPERBOUND;
        transpositionTable.store(zobristKey, depth, alpha, type, bestMove);
        return alpha;
    }

    public int evaluateCaptureOnlyGameMode() {
        int evaluation = 0;
        int perspective = (engine.ColorToMove == 1) ? 1 : -1;
        int whiteMaterial = countMaterial(1, engine.whitePieces());
        int blackMaterial = countMaterial(-1, engine.blackPieces());
        evaluation += whiteMaterial - blackMaterial;
        return evaluation * perspective;
    }


    /* <------------------------------------------------------------------------------------------------------------------>*/

    //MODE DE JEU LOS ALAMOS

    public String findBestMoveLosAlamos(int timeOfSearch) {
        ttMisses = 0;
        ttHits = 0;
        ttMissesQS = 0;
        ttHitsQS = 0;
        nodeCount = 0;
        quiescenceNodeCount = 0;
        time = 0.0;
        startTime = System.nanoTime();
        int alpha = this.negativeInfinity;
        int beta = this.positiveInfinity;
        String bestMove = null;

        List<String> moves = losAlamosEngine.generateLegalMoves(losAlamosEngine.ColorToMove, false);
        orderMovesLosAlamos(moves, 0, null);
        System.out.println(moves);
        String bestMoveAtDepth = null;
        int bestEvaluation = this.negativeInfinity + 100;

        for (int depth = 1; depth <= 100; depth++) {

            for (String move : moves) {
                losAlamosEngine.moveHistory.push(new LosAlamosMoveState(losAlamosEngine));
                losAlamosEngine.executeMove(move);
                int evaluation = -SearchLosAlamos(depth - 1, 0, -beta, -alpha);
                losAlamosEngine.unmakeMove();
                if (evaluation > bestEvaluation) {
                    bestEvaluation = evaluation;
                    bestMoveAtDepth = move;
                }

                if (bestEvaluation > alpha) {
                    alpha = bestEvaluation;
                }

                if (alpha >= beta) {
                    break;
                }

                double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
                if (elapsedTime >= timeOfSearch) {
                    System.out.println("New Temps écoulé, arrêt à profondeur " + depth);
                    System.out.println("Score " + bestEvaluation);
                    System.out.println("TT Hits " + ttHits + " : TT Misses " + ttMisses + "\nTT percentage: " + transpositionTable.getMemoryUsagePercentage() + "%\nTT QS percentage: " + transpositionTableQS.getMemoryUsagePercentage() + "%");
                    return bestMove != null ? bestMove : bestMoveAtDepth;
                }

            }

            if (bestMoveAtDepth != null) {
                bestMove = bestMoveAtDepth;
                System.out.println("Profondeur " + depth + " : Meilleur coup = " + bestMove + ", Score = " + bestEvaluation);
            }
        }
        return bestMove;
    }

    public int SearchLosAlamos(int depth, int depthFromRoot, int alpha, int beta) {
        nodeCount++;

        int static_eval = evaluateLosAlamos();
        int originalAlpha = alpha;


        /*  Recherche de Quiescence  */
        if (depth == 0) {
            return QuiescenceSearchLosAlamos(alpha, beta, 6);
        }

        /*  Null Move Pruning  */
        if (depth >= 3 && !losAlamosEngine.isKingInCheck(losAlamosEngine.ColorToMove) && static_eval >= beta/* && !onlyPawnsAndKing(losAlamosEngine.ColorToMove)*/) {
            int R = 2;
            losAlamosEngine.executeNullMove();
            int nullMoveEval = -SearchLosAlamos(depth - 1 - R, depthFromRoot + 1, -beta, -beta + 1);
            losAlamosEngine.unmakeNullMove();
            if (nullMoveEval >= beta) {
                return beta;
            }
        }

        List<String> moves = losAlamosEngine.generateLegalMoves(losAlamosEngine.ColorToMove, false);
        orderMovesLosAlamos(moves, depth, null);

        if (moves.isEmpty()) {
            if (losAlamosEngine.isCheckmate(losAlamosEngine.ColorToMove)) {
                return -(mateScore - (depthFromRoot * 100));
            } else {
                return 0;
            }
        }

        /*  Futility Pruning  */
        boolean futilityPruning = false;
        int futilityMargin = 0;
        if (depth <= 3 && !losAlamosEngine.isKingInCheck(losAlamosEngine.ColorToMove)) {
            if (depth == 1) futilityMargin = 100;
            else if (depth == 2) futilityMargin = 300;
            else if (depth == 3) futilityMargin = 500;
            if (static_eval + futilityMargin <= alpha) {
                futilityPruning = true;
            }
        }

        String bestMove = null;
        int evaluation = 0;

        for (int i = 0; i < moves.size(); i++) {
            String move = moves.get(i);

            if (futilityPruning && i > 0 && !losAlamosEngine.isCapture(move) && !losAlamosEngine.isPromotion(move)) {
                continue;
            }

            losAlamosEngine.moveHistory.push(new LosAlamosMoveState(losAlamosEngine));
            losAlamosEngine.executeMove(move);

            /*  Late Move Reduction  */
            int newDepth = depth - 1;

            double moveRatio = (double) i / moves.size();
            if (moves.size() > 4 && depth > 4) {
                if (moveRatio <= 1.0 / 3) newDepth -= 1;
                else if (moveRatio <= 2.0 / 3) newDepth -= 3;
                else newDepth -= 4;
            }


            /*  Principal Variation Search  */
            if (i == 0) {
                evaluation = -SearchLosAlamos(newDepth, depthFromRoot + 1, -beta, -alpha);
            } else {
                evaluation = -SearchLosAlamos(newDepth, depthFromRoot + 1, -alpha - 1, -alpha);
                if (evaluation > alpha && evaluation < beta) {
                    evaluation = -SearchLosAlamos(newDepth, depthFromRoot + 1, -beta, -alpha);
                }
            }

            losAlamosEngine.unmakeMove();

            /*  Stockage des Killer Moves & des Table d'historique  */
            if (evaluation >= beta) {
                if (!losAlamosEngine.isCapture(move)) {
                    storeKillerMove(move, depth);
                    int from = losAlamosEngine.squareToIndex(move.substring(0, 2));
                    int to = losAlamosEngine.squareToIndex(move.substring(2, 4));
                    historyTable2[from][to] += depth * depth;
                }
                //transpositionTable.store(zobristKey, depth, beta, TranspositionTable.LOWERBOUND, move);
                return beta;
            }

            if (evaluation > alpha) {
                alpha = evaluation;
                bestMove = move;
            }
        }

//        int type = (alpha > originalAlpha) ? TranspositionTable.EXACT : TranspositionTable.UPPERBOUND;
//        transpositionTable.store(zobristKey, depth, alpha, type, bestMove);
        return alpha;
    }



    /**
     * Fonction de recherche de quiétude utilisée pour évaluer les positions jusqu'à une certaine profondeur
     * tout en s'assurant que seules les captures sont considérées.
     *
     * @param alpha -> La valeur alpha pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur maximisant).
     * @param beta  -> La valeur beta pour l'élagage alpha-beta (meilleure valeur trouvée pour le joueur minimisant).
     * @return La meilleure évaluation trouvée.
     */
    public int QuiescenceSearchLosAlamos(int alpha, int beta, int depth) {
        nodeCount++;
        quiescenceNodeCount++;
        int originalAlpha = alpha;

        String bestMove = null;

        int evaluation = evaluateLosAlamos();

        /* Delta Pruning  */
        int deltaMargin = 950;
        if (evaluation + deltaMargin < alpha) {
            return alpha;
        }

        if (evaluation >= beta) {
            return beta;
        }

        if (evaluation > alpha) alpha = evaluation;

        List<String> captureMoves = losAlamosEngine.generateLegalMoves(losAlamosEngine.ColorToMove, true);
        orderMovesLosAlamos(captureMoves, depth, null);

        for (String move : captureMoves) {
            losAlamosEngine.moveHistory.push(new LosAlamosMoveState(losAlamosEngine));
            losAlamosEngine.executeMove(move);

            evaluation = -QuiescenceSearchLosAlamos(-beta, -alpha, depth - 1);
            losAlamosEngine.unmakeMove();

            if (evaluation >= beta) {
                return beta;
            }
            if (evaluation > alpha) {
                alpha = evaluation;
                bestMove = move;
            }
        }

        return alpha;
    }

    public int evaluateLosAlamos() {
        int perspective = (losAlamosEngine.ColorToMove == 1) ? 1 : -1;
        double phase = 1.;
        int whiteEval = 0;
        int blackEval = 0;

        int whiteMaterial = countMaterialLosAlamos(1, losAlamosEngine.whitePieces());
        int blackMaterial = countMaterialLosAlamos(-1, losAlamosEngine.blackPieces());

        int whitePositional = countPositionalValueLosAlamos(1, phase);
        int blackPositional = countPositionalValueLosAlamos(-1, phase);

        whiteEval = whiteMaterial + whitePositional;
        blackEval = blackMaterial + blackPositional;

        int finalEvaluation = whiteEval - blackEval;
        return finalEvaluation * perspective;
    }

    public void orderMovesLosAlamos(List<String> moves, int depth, String ttMove) {
        Map<String, Integer> moveScores = new HashMap<>();

        for (String move : moves) {

            int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');

            int score = 0;

            // 1. Coup de la table de transposition (Priorité absolue)
            if (move.equals(ttMove)) {
                score += 100_000;
                moveScores.put(move, score);
                continue;
            }

            // 2. Captures : MVV-LVA + SEE
            if (losAlamosEngine.isCapture(move)) {
                score += evaluateCaptureLosAlamos(move);
                //score += getSEE(move) * 10; // SEE pondéré
                moveScores.put(move, score);
                continue;
            }


            // 3. Killer Moves (historique des bons coups non-captures)
            if (!losAlamosEngine.isCapture(move)) {
                if (killerMoves[depth][0] != null && killerMoves[depth][0].equals(move)) {
                    score += 9500;
                    moveScores.put(move, score);
                    continue;
                } else if (killerMoves[depth][1] != null && killerMoves[depth][1].equals(move)) {
                    score += 9000;
                    moveScores.put(move, score);
                    continue;
                }
            }

            // 4. Promotions (les promotions en Dame sont prioritaires)
            if (losAlamosEngine.isPromotion(move)) {
                if (move.endsWith("Q") || move.endsWith("q")) {
                    score += 8500; // Promotion en Dame
                    moveScores.put(move, score);
                    continue;
                } else {
                    score += 5000; // Autres promotions
                    moveScores.put(move, score);
                    continue;
                }
            }

            // 4. Coups donnant échec (coûte trop cher)
//            if (engine.givesCheck(move)) {
//                score += 2000; // Échecs = haute priorité
//            }

            //score += getPieceValue(engine.getPieceAt(from));

            // 5. History Heuristic (priorise les coups ayant souvent bien fonctionné)
            //score += historyTable[losAlamosEngine.squareToIndex(move.substring(0, 2))][losAlamosEngine.getDestinationSquare(move)];

            moveScores.put(move, score);
        }
        moves.sort((m1, m2) -> Integer.compare(moveScores.get(m2), moveScores.get(m1)));
    }

    public int countMaterialLosAlamos(int color, long colorBitboard) {
        int material = 0;
        if (color == 1) {
            material += Long.bitCount(losAlamosEngine.whitePawns & colorBitboard) * pawnValue;
            material += Long.bitCount(losAlamosEngine.whiteKnights & colorBitboard) * knightValue;
            material += Long.bitCount(losAlamosEngine.whiteRooks & colorBitboard) * rookValue;
            material += Long.bitCount(losAlamosEngine.whiteQueens & colorBitboard) * queenValue;
            material += Long.bitCount(losAlamosEngine.whiteKing & colorBitboard) * kingValue;
        } else {
            material += Long.bitCount(losAlamosEngine.blackPawns & colorBitboard) * pawnValue;
            material += Long.bitCount(losAlamosEngine.blackKnights & colorBitboard) * knightValue;
            material += Long.bitCount(losAlamosEngine.blackRooks & colorBitboard) * rookValue;
            material += Long.bitCount(losAlamosEngine.blackQueens & colorBitboard) * queenValue;
            material += Long.bitCount(losAlamosEngine.blackKing & colorBitboard) * kingValue;
        }
        return material;
    }

    public int countPositionalValueLosAlamos(int color, double phase) {
        int value = 0;
        for (int i = 0; i < 64; i++) {
            char piece = losAlamosEngine.getPieceAt(i);
            if (color == 1) {
                switch (piece) {
                    case 'P':
                        value += (int) (phase * PAWN_POSITION_VALUES[i] + (1 - phase) * PAWN_POSITION_END_VALUES[i]);
                        break;
                    case 'N':
                        value += KNIGHT_POSITION_VALUES[i];
                        break;
                    case 'B':
                        value += BISHOP_POSITION_VALUES[i];
                        break;
                    case 'R':
                        value += ROOK_POSITION_VALUES[i];
                        break;
                    case 'Q':
                        value += QUEEN_POSITION_VALUES[i];
                        break;
                    case 'K':
                        value += (int) (phase * KING_POSITION_VALUES[i] + (1 - phase) * KING_POSITION_END_VALUES[i]);
                        break;
                }
            } else {
                switch (piece) {
                    case 'p':
                        value += (int) (phase * PAWN_POSITION_VALUES_BLACK[i] + (1 - phase) * PAWN_POSITION_END_VALUES_BLACK[i]);
                        break;
                    case 'n':
                        value += KNIGHT_POSITION_VALUES_BLACK[i];
                        break;
                    case 'b':
                        value += BISHOP_POSITION_VALUES_BLACK[i];
                        break;
                    case 'r':
                        value += ROOK_POSITION_VALUES_BLACK[i];
                        break;
                    case 'q':
                        value += QUEEN_POSITION_VALUES_BLACK[i];
                        break;
                    case 'k':
                        value += (int) (phase * KING_POSITION_VALUES_BLACK[i] + (1 - phase) * KING_POSITION_END_VALUES_BLACK[i]);
                        break;
                }
            }
        }
        return value;
    }

    private int evaluateCaptureLosAlamos(String move) {
        int from = (move.charAt(1) - '1') * 8 + (move.charAt(0) - 'a');
        int to = (move.charAt(3) - '1') * 8 + (move.charAt(2) - 'a');
        char s = losAlamosEngine.getPieceAt(from);
        char d = losAlamosEngine.getPieceAt(to);
        //Q take n == 10000 + (300 - 800) = 10000 - 500 = 9500
        //p take Q == 10000 + 800 - 100 = 10000 + 900 = 10900
        return 10000 + (getPieceValue(d) - getPieceValue(s));
    }


    /* <----------------------------------------------------------------------------------------------------------------->*/




    /**
     * Fonction qui permet de sauvegarder dans un fichier l'évaluation d'un fen (d'une partie en total)
     *
     * @param numGames -> nombre de parties à jouer
     * @param seconds -> nombre de secondes de réfléxion
     * @param outputFile -> fichier de sortie
     * @throws IOException retourne les erreurs possibles
     */
    public void generateTrainingData(int numGames, int seconds, String outputFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (int game = 0; game < numGames; game++) {
                //engine.resetToInitialPosition();
                engine.setFEN("r3k2r/ppp3pp/3b4/6B1/4P1n1/2N5/PPP3PP/R3K2R w KQkq - 0 1");
                int moveCount = 0;
                while (!engine.isCheckmate(engine.ColorToMove) && moveCount < 100) {
                    String fen = engine.getFEN();
                    System.out.println("FEN: " + fen);
                    engine.printBoard();
                    int score = evaluate();
                    writer.write(fen + " | " + score + "\n");

                    String bestMove = findBestMove(seconds);
                    if (bestMove == null) break;
                    engine.makeMove(bestMove);
                    engine.switchTurn();
                    moveCount++;
                }
                System.out.println("Partie " + (game + 1) + " terminée");
            }
        }
    }


    public static void main(String args[]) throws IOException {
        String startingFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        String cstartingFEN = "rnb1kb1r/1pp1np2/6p1/1p1p3p/4P3/N7/PPPP1PPP/R1B1K1NR w KQkq - 0 9";
        String FEN = "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1";
        String FENpartielong = "r1bqkb1r/1p3ppp/p1np1n2/4p3/4P3/2N1B3/PPP2PPP/RN1QKB1R w KQkq - 9 9";
        String FENpartielongissuequeen = "rnb1k1nr/pppp1p1p/5p2/8/8/8/PPP1PPPP/RN2KBNR w KQkq - 0 7";
        String fentest = "r3k2r/pppq1ppp/2n2n2/1B1p4/3P4/5N2/PPP2PPP/R1BQK2R w KQkq - 0 1";
        String fentest1 = "rnbqkbnr/ppp2ppp/4p3/3p4/3P4/2N2N2/PPP1PPPP/R1BQKB1R b KQkq - 0 1";
        String blunder = "r1bqkbnr/ppp2ppp/2n1p3/3p4/P2P4/2N2N2/1PP1PPPP/R1BQKB1R b KQkq a3 2 2";
        String FENNN = "r1bqk2r/1pp2ppp/p1n5/2bnp3/2B5/3P1N2/PPPN1PPP/R1B1QRK1 b kq - 3 8";
        //issue r1b2k1r/2p3pp/8/1p1n2B1/6n1/3P2N1/PPP2PPK/4RR2 w - - 2 20
        /**r . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n n . B .
         . . . . . . . .
         . . . P . . N .
         P P P . . P P K
         . . . . R R . .

         Entrez votre coup : e5g4
         true
         false

         r . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . n .
         . . . P . . N .
         P P P . . P P K
         . . . . R R . .


         r . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . n .
         . . . P . . N .
         P P P . . P P K
         . . . . R R . .

         r1b2k1r/2p3pp/8/1p1n2B1/6n1/3P2N1/PPP2PPK/4RR2 w - - 2 20
         [h2h3, h2h1]
         [h2h3, h2h1]*/

        /**Second issue h1a2
         *
         . . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . . .
         . . . P . P N .
         r P P . . . P .
         . . . . . R . K


         . . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . . .
         . . . P . P N .
         r P P . . . P .
         . . . . . R . K

         2b2k1r/2p3pp/8/1p1n2B1/8/3P1PN1/rPP3P1/5R1K w - - 0 24
         [b2b3, c2c3, d3d4, f3f4, b2b4, c2c4, f1f2, f1g1, f1e1, f1d1, f1c1, f1b1, f1a1, g3e2, g3e4, g3f5, g3h5, g5h6, g5f4, g5e3, g5d2, g5c1, g5f6, g5e7, g5d8, g5h4, h1h2, h1a2]
         [g5e7, h1a2, b2b3, c2c3, d3d4, f3f4, b2b4, c2c4, f1f2, f1g1, f1e1, f1d1, f1c1, f1b1, f1a1, g3e2, g3e4, g3f5, g3h5, g5h6, g5f4, g5e3, g5d2, g5c1, g5f6, g5d8, g5h4, h1h2]
         10.0 eval
         0
         FEN actuelle : 2b2k1r/2p3pp/8/1p1n2B1/8/3P1PN1/rPP3P1/5R1K w - - 0 24
         Meilleur coup du moteur : h1a2 Nb de nodes total: 1687171 nb de nodes Quiescence Search: 865260 nb de nodes sans quiescence: 821911 temps total: 7.0582283 sec, temps quiescence search: 0.0 sec
         Temps restant: Blancs: 179927 sec, Noirs: 179938 sec
         Entrez votre coup h1a2 : Entrez votre coup : h1a2
         false
         false

         . . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . . .
         . . . P . P N .
         K P P . . . P .
         . . . . . R . .


         . . b . . k . r
         . . p . . . p p
         . . . . . . . .
         . p . n . . B .
         . . . . . . . .
         . . . P . P N .
         K P P . . . P .
         . . . . . R . .
         */

        /*longue search

        r . b q k . . r
p p p . . p p p
. . n . . n . .
. . b p p . . .
. . B . P . . .
. . . P . N . .
P P P . . P P P
R N B Q . R K .

r1bqk2r/ppp2ppp/2n2n2/2bpp3/2B1P3/3P1N2/PPP2PPP/RNBQ1RK1 w kq d6 10 6
[a2a3, b2b3, c2c3, g2g3, h2h3, d3d4, a2a4, b2b4, g2g4, h2h4, e4d5, f1e1, b1d2, b1a3, b1c3, f3e1, f3d2, f3d4, f3h4, f3e5, f3g5, c1d2, c1e3, c1f4, c1g5, c1h6, c4d5, c4b3, c4b5, c4a6, d1d2, d1e1, d1e2, g1h1]
[f3e5, c4d5, e4d5, a2a3, b2b3, c2c3, g2g3, h2h3, d3d4, a2a4, b2b4, g2g4, h2h4, f1e1, b1d2, b1a3, b1c3, f3e1, f3d2, f3d4, f3h4, f3g5, c1d2, c1e3, c1f4, c1g5, c1h6, c4b3, c4b5, c4a6, d1d2, d1e1, d1e2, g1h1]

         */
        //"2kr1bnr/pp5p/n1p5/5q2/8/P1NP1N2/1PP3QP/R1B2K1R w - - 0 19"

        // a tester 2kr1b1r/pp5p/n1p2n2/5q2/8/P1NP1NQ1/1PP4P/R1B2K1R w - - 2 20 retour g3f4
        //2kr1bnr/pp5p/n1p5/5q2/8/P1NP1N2/1PP3QP/R1B2K1R w - - 0 19
        //Mouvement illégal d1c2 rnb1k1nr/pp1p1ppp/8/2b1P3/4P2q/8/PP3PPP/RNBQKBNR w - - 0 19

        //Souldnt do the bishop in c4 f1c4 if i remember well
        //blunder r3k1nr/pb1p1ppp/1pn3q1/2b1P3/4PB2/PQN2N1P/1P3PP1/R3KB1R w - - 3 26
        //illegal 2kr3r/1b1p4/1p4p1/2b1P3/4PB2/PnN3NP/1P3PP1/1R2K2R w K - 2 36

        //KNBK final 4k3/8/8/4NK2/2B5/8/8/8 b - - 0 1
        //KQK final k7/8/3K4/8/8/8/5Q2/8 b - - 2 2
        //cant capture quenn "rnb1kbnr/pppq1ppp/4P3/8/8/8/PPPP1PPP/RNBQKBNR w KQkq - 1 4"
//issue still can roque 2rqkb1r/pp3ppp/2n2n2/2p2bB1/3P4/4NNP1/PP2PPBP/R2QK2R b KQkq - 2 10
//issue dont detect ep 8/8/1p2p3/1B4k1/P4Pp1/4P3/8/7K b - f3 2 41
        Engine game = new Engine("r3k1nr/pb1p1ppp/1pn3q1/2b1P3/4PB2/PQN2N1P/1P3PP1/R3KB1R w - - 3 26");//"r2r2k1/pp1b1p1p/6p1/5p2/3qP2Q/1BN2R2/PP4PP/7K w - - 0 23");

        //LosAlamosEngine game = new LosAlamosEngine("rnqknr/pppppp/6/6/PPPPPP/RNQKNR w - - 0 1");
        MinimaxIterative minimax = new MinimaxIterative(game, true, false, false);
//        game.makeMove("f3f5");
//        System.out.println((float)NNUEBridge.evalFen(game.getFEN())/256);
        //MinimaxIterative minimax = new MinimaxIterative(game);

//        minimax.generateTrainingData(1, 20, "training_data.txt");

//        Engine game = new Engine("1r2kb1r/3bpp1p/pBnp1np1/1p5q/4P3/1B1P1N1P/PPPQ1PP1/R4RK1 w k - 2 16");//"r3k1nr/pb1p1ppp/1pn3q1/2b1P3/4PB2/PQN2N1P/1P3PP1/R3KB1R w - - 3 26");
//
//        MinimaxIterative minimax = new MinimaxIterative(game);
//        game.printBitboard(game.blackKing);
//        game.printBitboard(game.blackPawns);
//        game.printBitboard(game.blackPieces);
//        game.printBitboard(game.whitePawns);
//        game.printBitboard(game.whitePawnsAttacksAllied);
//        game.printBitboard(game.whitePawns & game.whitePawnsAttacksAllied);
        Scanner scanner = new Scanner(System.in);
//        game.printBoard();
//       // System.out.println(minimax.onlyPawnsAndKing(-1));
        game.startClock(); // Démarrer l'horloge pour le premier joueur

        while (!game.isCheckmate(game.ColorToMove) && !game.isTimeUp()) {
            game.printBoard();
            if (game.ColorToMove == 1 /*|| game.ColorToMove == -1*/) {
                String bestMove = minimax.findBestMove(20);
                //List<String> listBestMove = minimax.findListBestMoves(20);

                //System.out.println(game.numberOfMoves(0));
                System.out.println("FEN actuelle : " + game.getFEN());
                System.out.println("Meilleur coup du moteur : " + bestMove +
                        " Nb de nodes total: " + minimax.nodeCount +
                        " nb de nodes Quiescence Search: " + minimax.quiescenceNodeCount +
                        " nb de nodes sans quiescence: " + (minimax.nodeCount - minimax.quiescenceNodeCount) +
                        " temps total: " + minimax.time + " sec, temps quiescence search: " + minimax.timeQuiescence + " sec");
                //System.out.println(bestMove + " : " + listBestMove);

                game.makeMove(bestMove);
                game.printBoard();
//                game.printBitboard(game.whiteAttacks);
//                game.printBitboard(game.blackAttacks);
//                game.printBitboard(game.whiteAttacks & game.blackPieces);
//                System.out.println(Long.bitCount(game.whiteAttacks & game.blackPieces));
                System.out.println(game.getFEN());
                System.out.println("Temps restant: Blancs: " + game.getWhiteTime() / 1000 + " sec, Noirs: " + game.getBlackTime() / 1000 + " sec");

//                System.out.print("" + bestMove);
//                System.out.print("Entrez votre coup : ");
                //String userMove = scanner.nextLine();
                System.out.println(game.getFEN());
                game.stopClock();
                //game.makeMove(userMove);
                game.switchTurn();
//                game.stopClock();
//                game.switchTurn();
            } else {
                System.out.print("Entrez votre coup : ");
                //System.out.println(game.generateLegalMoves(-1, false));
                System.out.println(game.getFEN());
//                System.out.println(game.isKingInCheck(-1));
                String userMove = scanner.nextLine();
                game.stopClock();
                game.makeMove(userMove);
                game.switchTurn();
            }
            System.out.println(game.isKingInCheck(game.ColorToMove));
            System.out.println(game.isCheckmate(game.ColorToMove));
            //game.printBoard();
            if (game.isCheckmate(game.ColorToMove)) {
                break;
            }
            if (game.isTimeUp()) {
                System.out.println("Le temps est écoulé !");
                break;
            }


        }

        System.out.println("La partie est terminée. " + game.getFEN());
        scanner.close();

    }


    private static class MoveEvaluation {
        String move;
        int evaluation;

        MoveEvaluation(String move, int evaluation) {
            this.move = move;
            this.evaluation = evaluation;
        }
    }

    private List<String> finalizeMoveList(List<MoveEvaluation> moveEvaluations, int depth) {
        boolean isWhiteToMove = engine.ColorToMove == 1;

        moveEvaluations.sort(isWhiteToMove
                ? Comparator.comparingInt((MoveEvaluation me) -> me.evaluation).reversed()
                : Comparator.comparingInt(me -> me.evaluation));

        List<String> result = new ArrayList<>();
        int limit = Math.min(10, moveEvaluations.size());
        for (int i = 0; i < limit; i++) {
            result.add(moveEvaluations.get(i).move);
        }

        System.out.println("Score du meilleur coup : " + (float) moveEvaluations.get(0).evaluation / 256);
        System.out.println("TT Hits : " + ttHits + " | TT Misses : " + ttMisses);
        System.out.println("%TT : " + transpositionTable.getMemoryUsagePercentage() +
                " | %TTQS : " + transpositionTableQS.getMemoryUsagePercentage());
        System.out.println("Meilleurs coups à profondeur " + depth + " : " + result);

        return result;
    }


}

