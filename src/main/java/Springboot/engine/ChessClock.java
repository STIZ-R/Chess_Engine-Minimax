package Springboot.engine;

public class ChessClock {
    private long whiteTime;
    private long blackTime;
    private long startTime;
    private boolean isWhiteTurn;
    private boolean isRunning;

    public ChessClock(long initialTime) {
        this.whiteTime = initialTime;
        this.blackTime = initialTime;
        this.isWhiteTurn = true; // Blanc commence en premier
        this.isRunning = false;
    }

    public void start() {
        startTime = System.currentTimeMillis();
        isRunning = true;
    }

    public void stop() {
        update();
        isRunning = false;
    }

    public void switchTurn() {
        update();
        isWhiteTurn = !isWhiteTurn;
        start();
    }

    public void update() {
        if (isRunning) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            startTime = currentTime;

            if (isWhiteTurn) {
                whiteTime -= elapsedTime;
            } else {
                blackTime -= elapsedTime;
            }
        }
    }

    public long getWhiteTime() {
        return whiteTime;
    }

    public long getBlackTime() {
        return blackTime;
    }

    public boolean isTimeUp() {
        return whiteTime <= 0 || blackTime <= 0;
    }
}
