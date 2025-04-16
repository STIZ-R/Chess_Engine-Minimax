package Springboot.engine;

/**
 * @author stizr
 * @license MIT License
 *
 * Classe qui permet de sauvegarder l'état actuel de l'échiquier avec les droits données
 * Cela va permettre ultérieurement de pouvoir undo des moves en gardant les droits précédants
 */
public class MoveState {
    long whitePawns, whiteRooks, whiteKnights, whiteBishops, whiteQueens, whiteKing;
    long blackPawns, blackRooks, blackKnights, blackBishops, blackQueens, blackKing;

    long zobristKey;

    int enPassantSquare;
    int ColorToMove;
    boolean[] castlingRights;
    Engine engine;

    public MoveState(Engine engine) {
        this.engine = engine;
        this.whitePawns = engine.whitePawns;
        this.whiteRooks = engine.whiteRooks;
        this.whiteKnights = engine.whiteKnights;
        this.whiteBishops = engine.whiteBishops;
        this.whiteQueens = engine.whiteQueens;
        this.whiteKing = engine.whiteKing;
        this.blackPawns = engine.blackPawns;
        this.blackRooks = engine.blackRooks;
        this.blackKnights = engine.blackKnights;
        this.blackBishops = engine.blackBishops;
        this.blackQueens = engine.blackQueens;
        this.blackKing = engine.blackKing;
        this.enPassantSquare = engine.enPassantSquare;
        this.ColorToMove = engine.ColorToMove;
        this.castlingRights = engine.castlingRights.clone();
        this.zobristKey = engine.getZobristKey();
    }

    public MoveState clone()  {
        MoveState clone = new MoveState(this.engine);
        clone.whitePawns = this.whitePawns;
        clone.whiteBishops = this.whiteBishops;
        clone.whiteKing = this.whiteKing;
        clone.whiteKnights = this.whiteKnights;
        clone.whiteQueens = this.whiteQueens;
        clone.whiteRooks = this.whiteRooks;
        clone.blackPawns = this.blackPawns;
        clone.blackBishops = this.blackBishops;
        clone.blackKing = this.blackKing;
        clone.blackKnights = this.blackKnights;
        clone.blackQueens = this.blackQueens;
        clone.blackRooks = this.blackRooks;
        clone.enPassantSquare = this.enPassantSquare;
        clone.ColorToMove = this.ColorToMove;
        clone.castlingRights = this.castlingRights;
        clone.zobristKey = this.zobristKey;
        return clone;
    }

    public void saveState(Engine engine) {
        this.whitePawns = engine.whitePawns;
        this.whiteRooks = engine.whiteRooks;
        this.whiteKnights = engine.whiteKnights;
        this.whiteBishops = engine.whiteBishops;
        this.whiteQueens = engine.whiteQueens;
        this.whiteKing = engine.whiteKing;
        this.blackPawns = engine.blackPawns;
        this.blackRooks = engine.blackRooks;
        this.blackKnights = engine.blackKnights;
        this.blackBishops = engine.blackBishops;
        this.blackQueens = engine.blackQueens;
        this.blackKing = engine.blackKing;
        this.enPassantSquare = engine.enPassantSquare;
        this.ColorToMove = engine.ColorToMove;
        this.castlingRights = engine.castlingRights.clone();
        this.zobristKey = engine.getZobristKey();
    }

    public void restoreState(Engine engine) {
        engine.whitePawns = this.whitePawns;
        engine.whiteRooks = this.whiteRooks;
        engine.whiteKnights = this.whiteKnights;
        engine.whiteBishops = this.whiteBishops;
        engine.whiteQueens = this.whiteQueens;
        engine.whiteKing = this.whiteKing;
        engine.blackPawns = this.blackPawns;
        engine.blackRooks = this.blackRooks;
        engine.blackKnights = this.blackKnights;
        engine.blackBishops = this.blackBishops;
        engine.blackQueens = this.blackQueens;
        engine.blackKing = this.blackKing;
        engine.enPassantSquare = this.enPassantSquare;
        engine.ColorToMove = this.ColorToMove;
        engine.castlingRights = this.castlingRights.clone();
    }
}

