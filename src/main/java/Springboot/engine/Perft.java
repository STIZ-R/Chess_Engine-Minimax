package Springboot.engine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Perft {

    public static void main(String[] args) {

        String FEN_1 = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
//                  I write only the best times I got every runs
//                  Depth 1: 20 nodes (0,012 seconds)           -           20
//                * Depth 2: 400 nodes (0,004 seconds)          -           400
//                * Depth 3: 8902 nodes (0,016 seconds)         -           8902
//                * Depth 4: 197281 nodes (0,126 seconds)       -           197281
//                * Depth 5: 4865609 nodes (1,095 seconds)      -           4865609
//                * Depth 6: 119060496 nodes (29,314 seconds)   -           119,060,324
//                * new   6: ...538  (23,666 secondes)
//                * Depth 7: 3195902725 nodes (756,324 seconds) -           3,195,901,860

        //4,865,609	82,719	258	0	0	27,351	6	0	347
        //F2F3 node missing one move
        //F2F4 same pb


        String FEN_2 = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 0";
        /** issue e5c6 + 1 move e5g6 + 1 move e5d7 + 1 move e5f7 + 1 move
         * Depth	Nodes	Captures	E.p.	Castles	Promotions	Checks	Discovery Checks	Double Checks	Checkmates
         * 1	48	8	0	2	0	0	0	0	0
         * 2	2039	351	1	91	0	3	0	0	0
         * 3	97862	17102	45	3162	0	993	0	0	1
         * 4	4085603	757163	1929	128013	15172	25523	42	6	43
         * 5	193690690	35043416	73365	4993637	8392	3309887	19883	2637	30171
         * 6	8031647685	1558445089	3577504	184513607	56627920	92238050	568417	54948	360003
         */
        String FEN_3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        /**
         * issue node e2e4; g2g4
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

        Engine board = new Engine(FEN_1);
        //System.out.println(board.isCapture("b4f4"));
        runPerftTests(board, 6, false);
    }

    public static void runPerftTests(Engine board, int maxDepth, boolean fullSearch) {
        if(fullSearch) {
            for (int depth = 1; depth <= maxDepth; depth++) {
                long startTime = System.currentTimeMillis();
                Map<String, MoveDetails> moveCounts = perftRoot(board, depth);
                long endTime = System.currentTimeMillis();

                int total_capture = 0;
                int total_ep = 0;
                int total_roque = 0;
                int total_prom = 0;
                int total_check = 0;
                int total_checkmate = 0;

                System.out.println(" Profondeur " + depth + ":");
                long totalNodes = 0;

                for (Map.Entry<String, MoveDetails> entry : moveCounts.entrySet()) {
                    MoveDetails details = entry.getValue();
                    System.out.println("  " + entry.getKey() + ": " + details.nodes + " nœuds");
                    System.out.println("     Captures: " + details.captures);
                    System.out.println("     Captures en passant: " + details.enPassant);
                    System.out.println("     Roques: " + details.castles);
                    System.out.println("     Promotions: " + details.promotions);
                    System.out.println("     Échecs: " + details.checks);
                    System.out.println("     Échec et mat: " + details.checkmates);
                    totalNodes += details.nodes;
                    total_capture += details.captures;
                    total_ep = details.enPassant;
                    total_roque = details.castles;
                    total_prom = details.promotions;
                    total_check = details.checks;
                    total_checkmate = details.checkmates;
                }
                System.out.print("   Total: " + totalNodes + " nœuds (" + (endTime - startTime) / 1000.0 + " sec) ");
                System.out.print(" | Captures: " + total_capture);
                System.out.print(" | Captures en passant: " + total_ep);
                System.out.print(" | Roques: " + total_roque);
                System.out.print(" | Promotions: " + total_prom);
                System.out.print(" | Échecs: " + total_check);
                System.out.println(" | Échec et mat: " + total_checkmate + "\n");

            }
        }
        else {
            for (int depth = 1; depth <= maxDepth; depth++) {
                long startTime = System.currentTimeMillis();
                long nodes = perftLite(board, depth);
                long endTime = System.currentTimeMillis();

                System.out.printf("Depth %d: %d nodes (%.3f seconds)%n",
                        depth, nodes, (endTime - startTime) / 1000.0);
            }

        }
    }
    public static long perftLite(Engine board, int depth) {
        if (depth == 0) {
            return 1;
        }
        long nodes = 0;
        List<String> moves = board.generateLegalMoves(board.ColorToMove, false);

        for (String move : moves) {
            board.moveHistory.push(new MoveState(board));
            board.executeMove(move);
            long newNodes = perftLite(board, depth - 1);
            nodes += newNodes;
            board.unmakeMove();

        }
        return nodes;
    }


    public static Map<String, MoveDetails> perftRoot(Engine board, int depth) {
        Map<String, MoveDetails> moveCounts = new LinkedHashMap<>();
        List<String> moves = board.generateLegalMoves(board.ColorToMove, false);

        for (String move : moves) {
            int capture = 0;
            if (board.isCapture(move)) {
                capture = 1;
            }
            board.moveHistory.push(new MoveState(board));
            board.executeMove(move);
            MoveDetails details = new MoveDetails();
            details.nodes = perft(board, depth - 1, details, capture);
            moveCounts.put(move, details);
            board.unmakeMove();
        }
        return moveCounts;
    }

    public static long perft(Engine board, int depth, MoveDetails details, int capture) {
        if (depth == 0) return 1;
        long nodes = 0;
        details.captures += capture;
        List<String> moves = board.generateLegalMoves(board.ColorToMove, false);
        if (moves.isEmpty()) {
            if (board.isKingInCheck(board.ColorToMove)) {
                details.checkmates++;
            }
        }
        for (String move : moves) {
            if (board.isCapture(move)) {
                details.captures++;
            }
            board.moveHistory.push(new MoveState(board));
            board.executeMove(move);

            if (board.isKingInCheck(board.ColorToMove)) {
                details.checks++;
            }
            if (board.isCastlingMove(move)) {
                details.castles++;
            }
            if (board.isPromotion(move)) {
                details.promotions++;
            }
            if (board.isEnPassantMove(move)) {
                details.enPassant++;
            }
            nodes += perft(board, depth - 1, details, 0);
            board.unmakeMove();
        }
        return nodes;
    }

    static class MoveDetails {
        long nodes = 0;
        int checks = 0;
        int checkmates = 0;
        int captures = 0;
        int castles = 0;
        int promotions = 0;
        int enPassant = 0;
    }
}
