package Springboot.engine;

public class PawnHashTable {
    public long pawnZobristKey;
    public int pawnScore;
    public int passedPawns;
    public int pawnShield;

    public PawnHashTable(long key, int score, int passed, int shield) {
        this.pawnZobristKey = key;
        this.pawnScore = score;
        this.passedPawns = passed;
        this.pawnShield = shield;
    }
}

