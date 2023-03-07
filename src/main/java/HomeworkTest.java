import java.io.IOException;

public class HomeworkTest {

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        homework.FileHandler fileHandler = new homework.FileHandler();

        homework.Configuration configuration = fileHandler.loadConfiguration();
        System.out.println(configuration);

        homework.MoveManager moveManager = new homework.MoveManager(configuration);
        homework.PenteAgent agent = new homework.PenteAgent(moveManager, configuration.getPlayer());
        homework.Move move = agent.getBestMove();

        moveManager.commit(move);
        fileHandler.writeMove(move);
        fileHandler.writeBoard(moveManager.getBoard());
        fileHandler.updatePlayData(configuration);

        System.out.printf("Time taken: %fs%n", (System.currentTimeMillis()-start)/1000F);
    }
}
