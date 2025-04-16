package Springboot.ai;

/**
 * @author stizr
 * @license MIT License
 *
 */
class MoveResult {
    String move;
    double score;

    MoveResult(String move, double score) {
        this.move = move;
        this.score = score;
    }

    public String getMove(){
        return move;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "Move: " + move + ", Score: " + score;
    }
}

