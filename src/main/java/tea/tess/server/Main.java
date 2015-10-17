package tea.tess.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by arseniy on 12.10.15.
 */
public class Main {
    public static final int PORT = 12537;

    private static ServerSocket serverSocket;
    public static String responce = "";
    public static final String PROGRAM_NAME = "Remote file system";
    public static int clientNumbers = 0;

    public static void main(String[] args) throws Exception {
        ServerDao.Connect();
        ServerDao.CreateDB();
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                new Controller(serverSocket.accept(), clientNumbers++).start();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (SecurityException exception) {
            exception.printStackTrace();
        }
        ServerDao.CloseDB();
    }
}

