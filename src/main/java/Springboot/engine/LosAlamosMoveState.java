package Springboot.engine;

/**
 * @author stizr
 * @license MIT License
 *
 * Classe qui permet de sauvegarder l'état actuel de l'échiquier avec les droits données
 * Cela va permettre ultérieurement de pouvoir undo des moves en gardant les droits précédants
 */
public class LosAlamosMoveState {
    long whitePawns, whiteRooks, whiteKnights, whiteBishops, whiteQueens, whiteKing;
    long blackPawns, blackRooks, blackKnights, blackBishops, blackQueens, blackKing;

    long zobristKey;

    int enPassantSquare;
    int ColorToMove;
    boolean[] castlingRights;
    LosAlamosEngine engine;

    public LosAlamosMoveState(LosAlamosEngine engine) {
        this.engine = engine;
        this.whitePawns = engine.whitePawns;
        this.whiteRooks = engine.whiteRooks;
        this.whiteKnights = engine.whiteKnights;
        this.whiteQueens = engine.whiteQueens;
        this.whiteKing = engine.whiteKing;
        this.blackPawns = engine.blackPawns;
        this.blackRooks = engine.blackRooks;
        this.blackKnights = engine.blackKnights;
        this.blackQueens = engine.blackQueens;
        this.blackKing = engine.blackKing;
        this.ColorToMove = engine.ColorToMove;
    }

    public LosAlamosMoveState clone()  {
        LosAlamosMoveState clone = new LosAlamosMoveState(this.engine);
        clone.whitePawns = this.whitePawns;
        clone.whiteKing = this.whiteKing;
        clone.whiteKnights = this.whiteKnights;
        clone.whiteQueens = this.whiteQueens;
        clone.whiteRooks = this.whiteRooks;
        clone.blackPawns = this.blackPawns;
        clone.blackKing = this.blackKing;
        clone.blackKnights = this.blackKnights;
        clone.blackQueens = this.blackQueens;
        clone.blackRooks = this.blackRooks;
        clone.ColorToMove = this.ColorToMove;
        return clone;
    }
}

