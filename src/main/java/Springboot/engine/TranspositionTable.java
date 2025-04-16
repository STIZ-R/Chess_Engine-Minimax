package Springboot.engine;

/**
 * @author stizr
 * @license MIT License
 *
 * Classe: TranspositionTable
 *
 * Les tables de Transpositions vont permettrent la mémoïsations des noeuds déjà visités.
 * En effet, nous allons stocker différentes informations afin d'éviter de recalculer le noeud.
 *
 * Cela va permettre, si bien implémenter, de gagner jusqu'à 30 à 40% en gain de temps.
 *
 * Nous allons les stocker selon la clé de zobrist, càd qu'un index sera attribué à notre noeud selon la clé.
 *
 * De ce fait, nous allouons environs 256Mb de mémoire RAM afin d'avoir 8M d'entrées dans notre tableau.
 */
public class TranspositionTable {
    /** all moves displayed are equivalent : they all seem to be good (checked at chess fen analysis website)
     *
     * No TT at all : depth 9:  22 403 881 nodes best Move c3b1 pour 119 sec
     *
     *  QSearch + Search: (QSearch tend to add a lot more TTHits and NoHits but seem faster) :/ maybe in MoveOrder
     * 27 : TT -> QSearch + Search depth 9: 9% TT pour 17 414 384 nodes soit 6,37 BF pour 105 sec best Move a2a3
     * ...
     * 25 : TT -> QSearch + Search depth 9: 9% TT pour 18 225 570 nodes soit 6,40 BF pour 104 sec best Move a2a3
     * 24 : TT -> QSearch + Search depth 9: 9% TT pour 19 570 387 nodes soit 6,45 BF pour 112 sec best Move a2a3
     * ...
     * 22 : TT -> QSearch + Search depth 9: 8.5% TT pour 19 654 501 nodes soit 6,46 BF pour 116 sec best Move a2a3
     *
     *
     *
     *  Search: (Fewer TTHits and NoHits but same percentage of hits ratio)
     * 22 : TT -> Search depth 9: 8% TT pour 22 746 398 nodes soit 6,56 BF pour 125 sec best Move c1g5
     *
     *
     */
    private static final int TT_SIZE = 1 << 25; // 8,388,608 entrées (~256 MB)
    private final Entry[] table = new Entry[TT_SIZE];

    public static final int EXACT = 0;
    public static final int LOWERBOUND = 1;
    public static final int UPPERBOUND = 2;

    public void store(long zobristKey, int depth, int score, int type, String bestMove) {
        int index = (int) (zobristKey ^ (zobristKey >>> 32)) & (TT_SIZE - 1);

        Entry existing = table[index];

        if (existing == null || depth >= existing.depth) {
            //System.out.println("Stored: Key=" + zobristKey + " Depth=" + depth + " Score=" + score + " Type=" + type);
            table[index] = new Entry(zobristKey, depth, /*correctMateScoreForStorage(score, depth)*/score, type, bestMove);
        }
    }

    public Entry retrieve(long zobristKey) {
        int index = (int) (zobristKey ^ (zobristKey >>> 32)) & (TT_SIZE - 1);

        Entry entry = table[index];

        if (entry != null && entry.zobristKey == zobristKey) {
            //System.out.println("Retrieved: Key=" + zobristKey + " StoredKey=" + entry.zobristKey +
            //        " Depth=" + entry.depth + " Score=" + entry.score + " Type=" + entry.type);
            //System.out.println("TT Hit Depth: " + entry.depth);

            return new Entry(entry.zobristKey, entry.depth, /*correctMateScoreForStorage(score, depth)*/entry.score, entry.type, entry.bestMove);
        }
        return null;
    }

    private int correctMateScoreForStorage(int score, int numPlySearched) {
        if (isMateScore(score)) {
            int sign = Integer.signum((int) score);
            return (score * sign + numPlySearched) * sign;
        }
        return score;
    }

    private int correctRetrievedMateScore(int score, int numPlySearched) {
        if (isMateScore(score)) {
            int sign = Integer.signum((int) score);
            return (score * sign - numPlySearched) * sign;
        }
        return score;
    }

    private boolean isMateScore(double score) {
        return Math.abs(score) > 60000;
    }

    /**
     * Vide toutes les entrées de la table de transposition.
     */
    public void clear() {
        for (int i = 0; i < table.length; i++) {
            table[i] = null;
        }
        System.out.println("La table de transposition a été vidée.");
    }

    /**
     * Retourne le pourcentage d'utilisation de la mémoire de la table de transposition.
     */
    public double getMemoryUsagePercentage() {
        int usedEntries = 0;

        for (Entry entry : table) {
            if (entry != null) {
                usedEntries++;
            }
        }

        return (double) usedEntries / TT_SIZE * 100;
    }


    public static class Entry {
        public final long zobristKey;
        public final int depth;
        public final int score;
        public final int type; // 0: Exact, 1: LowerBound, 2: UpperBound
        public final String bestMove;

        public Entry(long zobristKey, int depth, int score, int type, String bestMove) {
            this.zobristKey = zobristKey;
            this.depth = depth;
            this.score = score;
            this.type = type;
            this.bestMove = bestMove;
        }

        public String Affichage() {
            return "" + zobristKey + " " + depth + " " + score + " " + type + " " + bestMove;
        }
    }

    public static void main(String[] args) {
        Engine engine = new Engine("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        engine.makeMove("b1c3");
        long zobristKey = Zobrist.getKeyForBoard(engine);
        System.out.println(zobristKey);
        TranspositionTable transpositionTable = new TranspositionTable();

        // Stocker une entrée
        transpositionTable.store(zobristKey, 1, 100, 0, "e2e4");

        // Récupérer et afficher l'entrée
        Entry ttEntry = transpositionTable.retrieve(Zobrist.getKeyForBoard(engine));
        if (ttEntry != null) {
            System.out.println(ttEntry.Affichage());
        } else {
            System.out.println("Aucune entrée trouvée pour la clé : " + zobristKey);
        }
    }

}
