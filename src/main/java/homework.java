import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class homework {

    public static void main(String[] args) throws IOException {
        FileHandler fileHandler = new FileHandler();

        Configuration configuration = fileHandler.loadConfiguration();

        MoveManager moveManager = new MoveManager(configuration);
        PenteAgent agent = new PenteAgent(moveManager, configuration.getPlayer());
        Move move = agent.getBestMove();

        moveManager.commit(move);
        fileHandler.writeMove(move);
        fileHandler.writeBoard(moveManager.board);
        fileHandler.updatePlayData(configuration);
    }

    public static class Constants {

        public static final int MAX_MOVES_SIZE = 10;
        private static final String INPUT_PATH = "input.txt";

        private static final String PLAYDATA_PATH = "playdata.txt";

        private static final String OUTPUT_PATH = "output.txt";

        private static final String BOARD_PATH = "board.txt";

        private static final int DIMS = 19;
        private static final int WIN_CAPTURES_NEEDED = 5;
        private static final int WIN_COINS_NEEDED = 5;
    }

    public static class FileHandler {

        public Configuration loadConfiguration() throws IOException {
            File file = new File(Constants.INPUT_PATH);
            BufferedReader reader = new BufferedReader(new FileReader(file));

            String line = reader.readLine();
            Player player = Player.valueOf(line);

            line = reader.readLine();
            float timeRemaining = Float.parseFloat(line);

            line = reader.readLine();
            int capturesByWhite = Integer.parseInt(line.split(",")[0]);
            int capturesByBlack = Integer.parseInt(line.split(",")[1]);

            Player[][] board = new Player[Constants.DIMS][Constants.DIMS];
            for (int i = 0; i < Constants.DIMS; i++) {
                line = reader.readLine();
                String[] cells = line.split("");
                for (int j = 0; j < Constants.DIMS; j++) {
                    board[i][j] = Player.fromLabel(cells[j]);
                }
            }

            int turn = loadPlayData();

            reader.close();

            return new Configuration(board, player, timeRemaining, capturesByWhite, capturesByBlack, turn);
        }

        private int loadPlayData() {
            try {
                File file = new File(Constants.PLAYDATA_PATH);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                reader.close();
                return Integer.parseInt(line);
            }
            catch (Exception ex) {
                /* If play-data is not present, this is the first move */
                return 1;
            }
        }

        public void updatePlayData(Configuration configuration) throws IOException {
            int lastTurn = configuration.getTurn();
            BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.PLAYDATA_PATH, false));
            writer.write(String.valueOf(lastTurn + 1));
            writer.close();
        }

        public void writeMove(Move move) throws IOException {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.OUTPUT_PATH, false));
            writer.write(move.getString());
            writer.close();
        }

        public void writeBoard(Player[][] board) throws IOException {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Constants.DIMS; i++) {
                String row = Arrays.stream(board[i])
                        .map(Player::getLabel)
                        .collect(Collectors.joining(""));
                row += "\n";
                builder.append(row);
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.BOARD_PATH, false));
            writer.write(builder.toString());
            writer.close();
        }
    }

    public static class Configuration {

        private final Player player;

        private final float timeRemaining;

        private final int capturesByWhite;

        private final int capturesByBlack;

        private final Player[][] board;

        private final int turn;

        public Configuration(Player[][] board, Player player, float timeRemaining, int capturesByWhite, int capturesByBlack, int turn) {
            this.board = board;
            this.player = player;
            this.timeRemaining = timeRemaining;
            this.capturesByWhite = capturesByWhite;
            this.capturesByBlack = capturesByBlack;
            this.turn = turn;
        }

        public Player getPlayer() {
            return player;
        }

        public float getTimeRemaining() {
            return timeRemaining;
        }

        public int getCapturesByWhite() {
            return capturesByWhite;
        }

        public int getCapturesByBlack() {
            return capturesByBlack;
        }

        public Player[][] getBoard() {
            return board;
        }

        public int getTurn() {
            return turn;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append(String.format("%s\n", this.player));
            builder.append(String.format("%f\n", this.timeRemaining));
            builder.append(String.format("%d,%d\n", this.capturesByWhite, this.capturesByBlack));
            for (int i = 0; i < Constants.DIMS; i++) {
                String row = Arrays.stream(board[i])
                        .map(Player::getLabel)
                        .collect(Collectors.joining());
                builder.append(String.format("%s\n", row));
            }

            return builder.toString();
        }
    }

    public enum Player {

        WHITE("w"),
        BLACK("b"),
        NONE(".");

        private final String label;

        private static final Map<String, Player> labelsMap = new HashMap<>();

        static {
            Arrays.stream(values()).forEach(player -> labelsMap.put(player.getLabel(), player));
        }

        Player(String label) {
            this.label = label;
        }

        public static Player fromLabel(String label) {
            Player player = labelsMap.get(label);
            if (Objects.isNull(player)) {
                throw new IllegalArgumentException(String.format("Player does not exist for the label: %s", label));
            }
            return player;
        }

        public Player opponent() {
            if (this == NONE) {
                throw new IllegalStateException("Other player does not exist for an empty intersection");
            }
            if (this == WHITE) {
                return BLACK;
            }
            return WHITE;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class PenteAgent {

        private static final int MINIMAX_MAX_DEPTH = 2;

        private final MoveManager moveManager;

        private final Player us;

        public PenteAgent(MoveManager moveManager, Player us) {
            this.us = us;
            this.moveManager = moveManager;
        }

        public Move getBestMove() {
            Move bestMove = null;
            long bestScore = Integer.MIN_VALUE;

            List<Move> moves = moveManager.getNextMoves(us);

            for (Move move : moves) {
                moveManager.commit(move);
                long score = minValue(move, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
                moveManager.rollback(move);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
            return bestMove;
        }

        private long minValue(Move previousMove, int depth, long alpha, long beta) {
            /* Assert that the previous move was made by us */
            if (previousMove.getPlayer() != us) {
                throw new IllegalStateException("Previous move on a minValue call must be made by us");
            }
            /* If this is game winning move, return the winning score */
            if (moveManager.haveWeWon(previousMove)) {
                return Result.WIN.getScore();
            }
            /* Else if the game is tied after this move, return the tieing score  */
            else if (moveManager.isGameTied()) {
                return Result.TIE.getScore();
            }
            /* Else if the recursion depth has been reached, return its eval */
            else if (depth > MINIMAX_MAX_DEPTH) {
                return previousMove.getScore();
            }

            /*
            Generate the next moves by our opponent. Since this is the minValue call, it's better to evaluate the moves
            and sort them in the increasing order of eval.
            */
            List<Move> moves = moveManager.getNextMoves(us.opponent());
            moves.sort(Move::compareTo);
            Collections.reverse(moves);

            /* Call maxValue on the above moves */
            for (Move move : moves) {
                moveManager.commit(move);
                beta = Math.min(beta, maxValue(move, depth + 1, alpha, beta));
                moveManager.rollback(move);
                if (alpha >= beta) {
                    return beta;
                }
            }
            return beta;
        }

        private long maxValue(Move previousMove, int depth, long alpha, long beta) {
            /* Assert that the previous move was made by the opponent */
            if (previousMove.getPlayer() != us.opponent()) {
                throw new IllegalStateException("Previous move on a minValue call must be made by us");
            }
            /* If this is game losing move, return the losing score */
            if (moveManager.haveTheyWon(previousMove)) {
                return Result.LOSS.getScore();
            }
            /* Else if the game is tied after this move, return the tieing score  */
            else if (moveManager.isGameTied()) {
                return Result.TIE.getScore();
            }
            /* Else if the recursion depth has been reached, return its eval */
            else if (depth > MINIMAX_MAX_DEPTH) {
                return previousMove.getScore();
            }

            /*
            Generate our next moves. Since this is the maxValue call, it's better to evaluate the moves and sort them
            in decreasing order of score.
            */
            List<Move> moves = moveManager.getNextMoves(us);
            moves.sort(Move::compareTo);

            /* Try out the moves one by one */
            for (Move move : moves) {
                moveManager.commit(move);
                alpha = Math.max(alpha, minValue(move, depth + 1, alpha, beta));
                moveManager.rollback(move);
                if (alpha >= beta) {
                    return alpha;
                }
            }
            return alpha;
        }
    }

    public static class MoveManager {

        private final Player[][] board;

        private final Player us;

        private int ourCaptures;

        private int theirCaptures;

        private int ourTurnNumber;

        private int theirTurnNumber;

        public MoveManager(Configuration configuration) {
            this.board = configuration.getBoard();
            this.us = configuration.getPlayer();

            /* Initialise turn numbers and captures */
            if (configuration.getPlayer() == Player.WHITE) {
                this.ourTurnNumber = configuration.getTurn();
                this.theirTurnNumber = configuration.getTurn();
                this.ourCaptures = configuration.getCapturesByWhite();
                this.theirCaptures = configuration.getCapturesByBlack();
            }
            else if (configuration.getPlayer() == Player.BLACK) {
                this.ourTurnNumber = configuration.getTurn();
                this.theirTurnNumber = configuration.getTurn() + 1;
                this.ourCaptures = configuration.getCapturesByBlack();
                this.theirCaptures = configuration.getCapturesByBlack();
            }
        }

        public List<Move> getNextMoves(Player currentPlayer) {
            List<Move> moves = new ArrayList<>();
            for (int i = 0; i < Constants.DIMS; i++) {
                for (int j = 0; j < Constants.DIMS; j++) {
                    int turnNumber = currentPlayer == us ? ourTurnNumber : theirTurnNumber;
                    if (allowed(currentPlayer, turnNumber, i, j)) {
                        moves.add(initMove(currentPlayer, i, j));
                    }
                }
            }

            /* Sort and clip moves */
            moves.sort(Move::compareTo);
            if (currentPlayer == us.opponent()) {
                Collections.reverse(moves);
            }
            return moves.stream().limit(Constants.MAX_MOVES_SIZE).collect(Collectors.toList());
        }

        /**
         * 1. The move must be within the bounds of the board
         * 2. If it's the white player's 1st turn, it must be on the centre intersection
         * 3. If it's the white player's 2nd turn, the coin must be placed on or outside the centre 3x3 square
         *    i.e., return false if !(i > 6 and i < 12 and j > 6 and j < 12)
         * 4. Else, players are free to place their coin on any unoccupied intersection
         */
        private boolean allowed(Player currentPlayer, int turn, int i, int j) {
            if (outOfBounds(i, j)) {
                return false;
            }
            if (board[i][j] != Player.NONE) {
                return false;
            }
            if (currentPlayer == Player.WHITE) {
                if (turn == 1) {
                    return i == 9 && j == 9;
                }
                else if (turn == 2) {
                    return !(i > 6 && i < 12 && j > 6 && j < 12);
                }
            }
            return true;
        }

        public Move initMove(Player player, int i, int j) {
            /* Cannot apply a move on an occupied intersection */
            if (board[i][j] != Player.NONE) {
                throw new IllegalArgumentException(String.format("Cannot play a move on an occupied intersection (%d,%d)", i, j));
            }

            /* Count the number of captures in each direction */
            List<homework.Direction> captureDirections = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                if (checkPossibleCapture(board, player, i, j, direction)) {
                    captureDirections.add(direction);
                }
            }

            Move move = new Move(player, i, j, captureDirections);
            move.setScore(evaluateMove(move));

            return move;
        }

        public long evaluateMove(Move move) {
            if (move.isApplied()) {
                throw new IllegalStateException("Cannot evaluate an already applied move");
            }

            tryMove(move);

            int ourCapturesInThisMove = 0;
            int theirCapturesInThisMove = 0;
            if (move.getPlayer() == us) {
                ourCapturesInThisMove = move.getCaptureDirections().size();
            }
            else {
                theirCapturesInThisMove = move.getCaptureDirections().size();
            }

            int score = getScore(board, us, ourCapturesInThisMove, ourCaptures) - getScore(board, us.opponent(), theirCapturesInThisMove, theirCaptures);

            undoTryMove(move);

            return score;
        }

        private int getScore(Player[][] board, Player player, int capturesInThisMove, int capturesSoFar) {
            /*
            How close are we to a win by capture? I know the number of captures made by the move. I also if those captures
            were made by us or the opponent. The more captures I make on this move, the closer I get to the goal
            */
            int capturesNeeded = Constants.WIN_CAPTURES_NEEDED - capturesSoFar;
            int captureScore = 0;
            if (capturesInThisMove >= capturesNeeded) {
                captureScore = Integer.MAX_VALUE;
            }
            else if (capturesInThisMove > 0) {
                captureScore = 10_000 * (capturesSoFar + capturesInThisMove);
            }
            /* Get the number of possible captures*/
            captureScore += 1_000 * countPossibleCaptures(board, player);

            /* Get the number of possible open 4s */
            int open4Score = 1_000 * countPossibleOpenKs(board, player, 4);

            /* Get the number of possible open 3s */
            int open3Score = 100 * countPossibleOpenKs(board, player, 3);

            /* Get the number of open 2s */
            int open2Score = 10 * countPossibleOpenKs(board, player, 2);

            /* Get the number of player pieces on the board */
            int piecesScore = countPieces(board, player);

            return captureScore + open4Score + open3Score + open2Score + piecesScore;
        }

        private int countPossibleCaptures(Player[][] board, Player player) {
            int count = 0;
            for (int i = 0; i < Constants.DIMS; i++) {
                for (int j = 0; j < Constants.DIMS; j++) {
                    if (board[i][j] == player) {
                        for (Direction direction : Direction.values()) {
                            if (checkPossibleCapture(board, player, i, j, direction)) {
                                count++;
                            }
                        }
                    }
                }
            }
            return count;
        }
        
        private int countPossibleOpenKs(Player[][] board, Player player, int K) {
            int count = 0;
            for (int i = 0; i < Constants.DIMS; i++) {
                for (int j = 0; j < Constants.DIMS; j++) {
                    for (Direction direction : Direction.values()) {
                        if (checkOpenK(board, player, K, i, j, direction)) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        /**
         * Returns true if there's an open K starting from position i, j in the given direction. K intersections including
         * the current one must be occupied by the current player and there must be atleast 5 - K succeeding empty intersections
         */
        private boolean checkOpenK(Player[][] board, Player player, int K, int i, int j, Direction direction) {
            int targetI = direction.moveI(i, K + 1);
            int targetJ = direction.moveJ(j, K + 1);

            if (board[i][j] != player) {
                return false;
            }
            if (outOfBounds(targetI, targetJ)) {
                return false;
            }
            if (board[targetI][targetJ] != Player.NONE) {
                return false;
            }
            if (!checkKInARow(player, K, i, j, direction)) {
                return false;
            }

            int needed = Constants.WIN_COINS_NEEDED - K;
            return checkKInARow(Player.NONE, needed, targetI, targetJ, direction);
        }

        /**
         * Returns true if there are K consecutive player pieces in a direction, starting from the current coordinate
         */
        private boolean checkKInARow(Player player, int K, int i, int j, Direction direction) {
            if (outOfBounds(i, j)) {
                return false;
            }
            if (board[i][j] != player) {
                return false;
            }
            while (K > 0 && !outOfBounds(i, j) && board[i][j] == player) {
                i = direction.moveI(i, 1);
                j = direction.moveJ(j, 1);
                K--;
            }
            return K == 0;
        }

        private int countPieces(Player[][] board, Player player) {
            int count = 0;
            for (int i = 0; i < Constants.DIMS; i++) {
                for (int j = 0; j < Constants.DIMS; j++) {
                    if (board[i][j] == player) {
                        count++;
                    }
                }
            }
            return count;
        }

        public void commit(Move move) {
            tryMove(move);
            if (move.getPlayer() == us) {
                ourCaptures += move.getCaptureDirections().size();
                ourTurnNumber += 1;
            }
            else {
                theirCaptures += move.getCaptureDirections().size();
                theirTurnNumber += 1;
            }
        }

        public void rollback(Move move) {
            undoTryMove(move);
            if (move.getPlayer() == us) {
                ourCaptures -= move.getCaptureDirections().size();
                ourTurnNumber -= 1;
            }
            else {
                theirCaptures -= move.getCaptureDirections().size();
                theirTurnNumber -= 1;
            }
        }

        private void tryMove(Move move) {
            placePiece(move);
            removeCapturedPieces(move);
            move.commit();
        }

        private void printBoard() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Constants.DIMS; i++) {
                String row = Arrays.stream(board[i])
                        .map(Player::getLabel)
                        .collect(Collectors.joining(""));
                row += "\n";
                builder.append(row);
            }
            System.out.println(builder);
        }

        private void undoTryMove(Move move) {
            removePiece(move);
            placeCapturedPieces(move);
            move.revoke();
        }

        private void placePiece(Move move) {
            if (move.isApplied()) {
                throw new IllegalArgumentException("Cannot place piece for an applied move");
            }

            /* Place our piece */
            Player player = move.getPlayer();
            int i = move.getI();
            int j = move.getJ();
            board[i][j] = player;
        }

        private void removeCapturedPieces(Move move) {
            if (move.isApplied()) {
                throw new IllegalArgumentException("Cannot remove captured pieces for an applied move");
            }

            /* Apply captures */
            int i = move.getI();
            int j = move.getJ();

            int nextI;
            int nextJ;
            for (Direction captureDirection : move.getCaptureDirections()) {
                nextI = captureDirection.moveI(i, 1);
                nextJ = captureDirection.moveJ(j, 1);
                board[nextI][nextJ] = Player.NONE;

                nextI = captureDirection.moveI(i, 2);
                nextJ = captureDirection.moveJ(j, 2);
                board[nextI][nextJ] = Player.NONE;
            }
        }

        private void removePiece(Move move) {
            if (!move.isApplied()) {
                throw new IllegalArgumentException("Cannot remove the piece of an unapplied move");
            }

            int i = move.getI();
            int j = move.getJ();

            /* Remove our piece */
            board[i][j] = Player.NONE;
        }

        private void placeCapturedPieces(Move move) {
            if (!move.isApplied()) {
                throw new IllegalArgumentException("Cannot add the captured pieces of an unapplied move");
            }

            Player player = move.getPlayer();
            int i = move.getI();
            int j = move.getJ();

            /* Undo captures */
            int nextI;
            int nextJ;
            for (Direction captureDirection : move.getCaptureDirections()) {
                nextI = captureDirection.moveI(i, 1);
                nextJ = captureDirection.moveJ(j, 1);
                board[nextI][nextJ] = player.opponent();

                nextI = captureDirection.moveI(i, 2);
                nextJ = captureDirection.moveJ(j, 2);
                board[nextI][nextJ] = player.opponent();
            }
        }

        public boolean haveWeWon(Move move) {
            if (move.getPlayer() != us) {
                throw new IllegalArgumentException("Our winning move check cannot be made on the opponent's move");
            }
            return isWinningMove(move, ourCaptures);
        }

        public boolean haveTheyWon(Move move) {
            if (move.getPlayer() == us) {
                throw new IllegalArgumentException("The opponent's winning move check cannot be made on our move");
            }
            return isWinningMove(move, theirCaptures);
        }

        /**
         * A player has one if they have
         *     1. Made the required number of captures
         *     2. Made a tessera (4-in-a-row not surrounded by the other player's pieces) NOTE: CHECK THIS AGAIN
         *     3. Made a 5-in-a-row
         */
        private boolean isWinningMove(Move move, int capturesSoFar) {
            /* Check if the player has made at least 5 captures with this move */
            if (capturesSoFar >= Constants.WIN_CAPTURES_NEEDED) {
                return true;
            }

            /* Check if the player has a 5-in-a-row through this move */
            for (int index = 0; index < 4; index++) {
                Direction direction = Direction.values()[index];
                int front = 0;
                int back = 0;

                int i = direction.moveI(move.getI(), 1);
                int j = direction.moveJ(move.getJ(), 1);
                while (!outOfBounds(i, j) && board[i][j] == move.getPlayer()) {
                    front++;
                    i = direction.moveI(i, 1);
                    j = direction.moveJ(j, 1);
                }

                i = direction.moveI(move.getI(), -1);
                j = direction.moveJ(move.getJ(), -1);
                while (!outOfBounds(i, j) && board[i][j] == move.getPlayer()) {
                    back++;
                    i = direction.moveI(i, -1);
                    j = direction.moveJ(j, -1);
                }

                if (front + back + 1 >= Constants.WIN_COINS_NEEDED) {
                    return true;
                }
            }

            return false;
        }

        /**
         * The game is tied if no more intersections are left to make moves onto
         * IMPORTANT: This must be called after isWinningMove, as the last move on the board can be a winning move
         */
        public boolean isGameTied() {
            int count = 0;
            for (int i = 0; i < Constants.DIMS; i++) {
                for (int j = 0; j < Constants.DIMS; j++) {
                    if (board[i][j] != Player.NONE) {
                        count++;
                    }
                }
            }
            return count == Constants.DIMS * Constants.DIMS;
        }

        /**
         * A capture occurs when we surround 2 consecutive opponent pieces by 2 of our own (i.e. X00X or 0XX0)
         * This method returns true if a capture can occur in a given direction if we place our piece on (i, j).
         * Let target be our own piece, 3 intersections away from (i, j)
         *     1. The coordinates to the target will be (i + 3 * direction-delta, j + 3 * direction-delta)
         *     2. If the current coordinates aren't empty
         *     3. If the target coordinates are out of bounds, return false
         *     4. Else if the piece on the target coordinates isn't ours, return false
         *     5. Else if the piece on the target coordinates is ours, if both the middle pieces are the opponent's, return true
         */
        private boolean checkPossibleCapture(Player[][] board, Player player, int i, int j, Direction direction) {
            int targetI = direction.moveI(i, 3);
            int targetJ = direction.moveJ(j, 3);

            if (board[i][j] != Player.NONE) {
                return false;
            }
            if (outOfBounds(targetI, targetJ)) {
                return false;
            }
            if (board[targetI][targetJ] != player) {
                return false;
            }

            boolean firstCaptured = board[direction.moveI(i, 1)][direction.moveJ(j, 1)] == player.opponent();
            boolean secondCaptured = board[direction.moveI(i, 2)][direction.moveJ(j, 2)] == player.opponent();
            return firstCaptured && secondCaptured;
        }

        private boolean outOfBounds(int i, int j) {
            return i < 0 || j < 0 || i >= Constants.DIMS || j >= Constants.DIMS;
        }
    }

    public enum Direction {

        N(-1, 0),
        NE(-1, +1),
        E(0,+1),
        SE(+1, +1),
        S(+1,0),
        SW(+1, -1),
        W(0,-1),
        NW(-1, -1);

        private final int deltaI;

        private final int deltaJ;

        Direction(int deltaI, int deltaJ) {
            this.deltaI = deltaI;
            this.deltaJ = deltaJ;
        }

        public int moveI(int i, int distance) {
            if (outOfBounds(i)) {
                throw new IllegalArgumentException(String.format("I (%d) cannot be outside bounds", i));
            }
            return i + distance * getDeltaI();
        }

        public int moveJ(int j, int distance) {
            if (outOfBounds(j)) {
                throw new IllegalArgumentException(String.format("J (%d) cannot be outside bounds", j));
            }
            return j + distance * getDeltaJ();
        }

        public int getDeltaI() {
            return deltaI;
        }

        public int getDeltaJ() {
            return deltaJ;
        }

        private boolean outOfBounds(int p) {
            return p < 0 || p >= Constants.DIMS;
        }
    }

    public static class Move implements Comparable<Move> {

        private static final String[] columns = {"A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"};

        private final Player player;

        private final int i;

        private final int j;

        private final List<Direction> captureDirections;

        private long score;
        
        private boolean applied;

        public Move(Player player, int i, int j, List<Direction> captureDirections) {
            if (j >= Constants.DIMS || j < 0 || i >= Constants.DIMS || i < 0) {
                throw new IllegalArgumentException("Move is out of bounds");
            }
            if (player == Player.NONE) {
                throw new IllegalArgumentException("Player NONE cannot make a move");
            }
            this.player = player;
            this.i = i;
            this.j = j;
            this.captureDirections = captureDirections;
            this.applied = false;
            this.score = 0;
        }

        public Player getPlayer() {
            return player;
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public List<Direction> getCaptureDirections() {
            return captureDirections;
        }

        public boolean isApplied() {
            return applied;
        }

        public void commit() {
            this.applied = true;
        }
        
        public void revoke() {
            this.applied = false;
        }

        public long getScore() {
            return score;
        }

        public void setScore(long score) {
            this.score = score;
        }

        public String getString() {
            return String.valueOf(Constants.DIMS - i) + columns[j];
        }

        @Override
        public int compareTo(Move move) {
            if (getScore() > move.getScore()) {
                return -1;
            }
            else if (getScore() < move.getScore()) {
                return 1;
            }
            return 0;
        }
    }

    public enum Result {

        WIN(Integer.MAX_VALUE),
        TIE(Integer.MAX_VALUE - 1000),
        LOSS(Integer.MIN_VALUE);

        private final int score;

        Result(int score) {
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }

    public static class CapturePair {

        private final int white;

        private final int black;

        private CapturePair(int first, int second) {
            this.white = first;
            this.black = second;
        }

        public static CapturePair of(int white, int black) {
            return new CapturePair(white, black);
        }

        public int getCaptures(Player player) {
            switch (player) {
                case WHITE:
                    return white;
                case BLACK:
                    return black;
                default:
                    throw new IllegalArgumentException(String.format("Player %s cannot have any captures", player));
            }
        }
    }
}
