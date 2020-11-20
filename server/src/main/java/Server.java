import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public Server() {
        ExecutorService runner = Executors.newFixedThreadPool(4);
        try(ServerSocket server = new ServerSocket(8189)) {
            System.out.println("Server has been started.");
            while (true) {
                Socket socket = server.accept();
                System.out.println("Client accepted");
                runner.execute(new ClientHandler(socket));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Something happened in server.class.");
        }
    }

}