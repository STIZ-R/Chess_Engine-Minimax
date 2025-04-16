package Springboot.engine;

import java.util.HashMap;
import java.util.Map;

public class RepetitionTable {
    public Map<Long, Integer> repetitionMap;

    public RepetitionTable() {
        this.repetitionMap = new HashMap<>();
    }

    public void addPosition(long zobristKey) {
        repetitionMap.put(zobristKey, repetitionMap.getOrDefault(zobristKey, 0) + 1);
    }

    public void removePosition(long zobristKey) {
        if (repetitionMap.containsKey(zobristKey)) {
            int count = repetitionMap.get(zobristKey);
            if (count > 1) {
                repetitionMap.put(zobristKey, count - 1);
            } else {
                repetitionMap.remove(zobristKey);
            }
        }
    }

    public boolean isTripleRepetition(long zobristKey) {
        return repetitionMap.getOrDefault(zobristKey, 0) >= 3;
    }

    public static void main(String[] args) {
        Engine engine = new Engine("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        RepetitionTable rep = new RepetitionTable();
        rep.addPosition(engine.getZobristKey());
        engine.makeMove("b1c3");
        engine.makeMove("b8c6");
        engine.makeMove("c3b1");
        engine.makeMove("c6b8");
        rep.addPosition(engine.getZobristKey());
        engine.makeMove("b1c3");
        engine.makeMove("b8c6");
        engine.makeMove("c3b1");
        engine.makeMove("c6b8");
        rep.addPosition(engine.getZobristKey());
        engine.makeMove("b1c3");
        engine.makeMove("b8c6");
        engine.makeMove("c3b1");
        engine.makeMove("c6b8");
        rep.addPosition(engine.getZobristKey());
        System.out.println(rep.isTripleRepetition(engine.getZobristKey()));
        System.out.println(rep.repetitionMap.getOrDefault(engine.getZobristKey(), 0));

        rep.removePosition(engine.getZobristKey());
        rep.removePosition(engine.getZobristKey());

        System.out.println(rep.isTripleRepetition(engine.getZobristKey()));
        System.out.println(rep.repetitionMap.getOrDefault(engine.getZobristKey(), 0));

    }
}
