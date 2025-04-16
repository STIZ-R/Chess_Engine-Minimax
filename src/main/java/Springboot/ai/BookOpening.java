package Springboot.ai;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 * Classe permettant de donner à l'algorithme Minimax une liste
 * d'ouvertures afin d'éviter d'avoir toujoyrs les mêmes ouvertures de la part
 * de celui-ci.
 */
public class BookOpening {
    private final Map<String, Map<String, Integer>> openingBook;

    public BookOpening(String resourcePath) throws IOException {
        openingBook = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(resourcePath))) {
            String line;
            String currentPosition = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("pos")) {
                    currentPosition = normalizeFEN(line.substring(4));
                } else if (currentPosition != null) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 2) {
                        String move = parts[0];
                        int frequency = Integer.parseInt(parts[1]);
                        openingBook.computeIfAbsent(currentPosition, k -> new HashMap<>()).put(move, frequency);
                    }
                }
            }
        }
    }

    /**
     * Permet de normaliser le FEN en omitant les dernières parties comme
     * le "En Passant" ou même les coups depuis la dernière capture et le nombre de coups totaux.
     *
     * @param fen -> prend le fen afin de le normaliser
     * @return un fen sans les dernières parties
     */
    private String normalizeFEN(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length >= 4) {
            return String.join(" ", parts[0], parts[1], parts[2]);
        }
        return fen;
    }

    /**
     * Regarde dans la map, voir si une key correspond au fen.
     * Si c'est le cas, elle regarde les coups recommandés (souvent 3, 4)
     * Elle associe ensuite un pourcentage à chaque coup (selon les nombres de personnes l'ayant joué)
     * Et ensuite un random choisit un coup en suivant le pourcentage.
     *
     * @param position -> Fen de la partie
     * @return un coup suivant la position de l'échiquier
     */
    public String getRecommendedMove(String position) {
        String normalizedPosition = normalizeFEN(position);
        if (!openingBook.containsKey(normalizedPosition)) {
            return null;
        }

        Map<String, Integer> moves = openingBook.get(normalizedPosition);
        int totalFrequency = moves.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = new Random().nextInt(totalFrequency);
        int cumulativeFrequency = 0;

        for (Map.Entry<String, Integer> entry : moves.entrySet()) {
            cumulativeFrequency += entry.getValue();
            if (randomValue < cumulativeFrequency) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean hasOpeningMoves(String position) {
        return openingBook.containsKey(normalizeFEN(position));
    }
}

