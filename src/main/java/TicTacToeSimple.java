import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public class TicTacToeSimple {

    public static void main(String[] args) {
//        Player ai = Player.X;
//        Scanner scanner = new Scanner(System.in);
//        while ()
    }

    public enum Player {

        X,
        O;

        public Player other() {
            Optional<Player> otherPlayer = Arrays.stream(values())
                    .filter(player -> player != this)
                    .findFirst();
            if (!otherPlayer.isPresent()) {
                throw new IllegalStateException("Other player must always be non-null");
            }
            return otherPlayer.get();
        }
    }

    public enum Result {

        LOSS(-1),
        TIE(0),
        WIN(1);

        private final int value;

        Result(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class Move {
        private final int i;
        private final int j;

        public Move(int i, int j) {
            this.i = i;
            this.j = j;
        }

        public int getJ() {
            return j;
        }

        public int getI() {
            return i;
        }
    }

    public static class Agent {

        private static final int DIMS = 3;

        private static final int WINNER_CELLS = 3;

        private final Player[][] board = new Player[DIMS][DIMS];

        private final Player maximisingPlayer;

        public Agent(Player maximisingPlayer) {
            this.maximisingPlayer = maximisingPlayer;
        }

        private Move getBestMove() {
            /* Check if the game has already completed */
            Result result = checkTerminalResult();
            if (Objects.nonNull(result)) {
                return null;
            }

            /* Try all the moves */
            int bestScore = Integer.MIN_VALUE;
            int bestI = 0;
            int bestJ = 0;
            for (int i = 0; i < DIMS; i++) {
                for (int j = 0; j < DIMS; j++) {
                    if (Objects.isNull(board[i][j])) {
                        board[i][j] = maximisingPlayer;
                        int score = maxValue();
                        board[i][j] = null;
                        if (score > bestScore) {
                            bestScore = score;
                            bestI = i;
                            bestJ = j;
                        }
                    }
                }
            }
            return new Move(bestI, bestJ);
        }

        private int maxValue() {
            Result result = checkTerminalResult();
            if (Objects.nonNull(result)) {
                return result.getValue();
            }
            int value = Integer.MIN_VALUE;
            for (int i = 0; i < DIMS; i++) {
                for (int j = 0; j < DIMS; j++) {
                    if (Objects.isNull(board[i][j])) {
                        board[i][j] = maximisingPlayer;
                        value = Integer.max(value, minValue());
                        board[i][j] = null;
                    }
                }
            }
            return value;
        }

        private int minValue() {
            Result result = checkTerminalResult();
            if (Objects.nonNull(result)) {
                return result.getValue();
            }
            int value = Integer.MAX_VALUE;
            for (int i = 0; i < DIMS; i++) {
                for (int j = 0; j < DIMS; j++) {
                    if (Objects.isNull(board[i][j])) {
                        board[i][j] = maximisingPlayer.other();
                        value = Integer.min(value, maxValue());
                        board[i][j] = null;
                    }
                }
            }
            return value;
        }

        private Result checkTerminalResult() {
            if (checkWinner(maximisingPlayer)) {
                return Result.WIN;
            }
            else if (checkWinner(maximisingPlayer.other())) {
                return Result.LOSS;
            }
            else if (isBoardFull()) {
                return Result.TIE;
            }
            return null;
        }

        private boolean checkWinner(Player player) {
            int i;
            int j;

            /* Check NW-SE diagonal */
            i = 0;
            j = 0;
            int count = 0;
            while (i < DIMS && j < DIMS) {
                if (board[i][j] == player) {
                    count++;
                }
                i++;
                j++;
            }
            if (count == WINNER_CELLS) {
                return true;
            }

            /* Check NE-SW diagonal */
            i = 0;
            j = DIMS - 1;
            count = 0;
            while (i < DIMS && j >= 0) {
                if (board[i][j] == player) {
                    count++;
                }
                i++;
                j--;
            }
            if (count == WINNER_CELLS) {
                return true;
            }

            /* Check NS */
            for (j = 0; j < DIMS; j++) {
                count = 0;
                for (i = 0; i < DIMS; i++) {
                    if (board[i][j] == player) {
                        count++;
                    }
                }
                if (count == WINNER_CELLS) {
                    return true;
                }
            }

            /* Check EW */
            for (i = 0; i < DIMS; i++) {
                count = 0;
                for (j = 0; j < DIMS; j++) {
                    if (board[i][j] == player) {
                        count++;
                    }
                }
                if (count == WINNER_CELLS) {
                    return true;
                }
            }

            /* No winner */
            return false;
        }

        private boolean isBoardFull() {
            int count = 0;
            for (int i = 0; i < DIMS; i++) {
                for (int j = 0; j < DIMS; j++) {
                    if (Objects.nonNull(board[i][j])) {
                        count++;
                    }
                }
            }
            return count == DIMS * DIMS;
        }
    }
}
