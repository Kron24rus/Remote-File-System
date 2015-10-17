package tea.tess.server;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import sun.misc.BASE64Decoder;
import tea.tess.server.exceptions.UnknownCommandException;
import tea.tess.server.exceptions.WrongCommandFormatException;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

/**
 * Created by arseniy on 12.10.15.
 */
public class Controller extends Thread {
    private String Login;
    private String Password;
    private String authorizeCommand, registerCommand;
    private String wrongParamsCountExceptionMessage;
    private Path rootDirectory;
    private Path currentDirectory;
    private BufferedReader in;
    private PrintWriter out;
    private Socket clientSocket;
    private boolean authorized = false;
    private static  String AUTHORIZE_HELP;
    private static final String FINISH_CODE = "xfdm315";
    private int clientNumber;
    private FileSystemController fileSystemController;

    Controller() {

    }

    Controller(Socket clientSocket, int clientNumber) {
        this.clientSocket = clientSocket;
        this.clientNumber = clientNumber;
    }

    Controller(String authorizeCommand, String registerCommand, String wrongParamsCountExceptionMessage) {
        this.authorizeCommand = authorizeCommand;
        this.registerCommand = registerCommand;
        this.wrongParamsCountExceptionMessage = wrongParamsCountExceptionMessage;
    }

    public void recognize (String message) throws Exception, UnknownCommandException, FileNotFoundException, NullPointerException {
        int to = message.indexOf(" ");
        String command, params = "";
        if (to == -1)
            to = message.length();
        else
            params = message.substring(to + 1);
        command = message.substring(0, to);

        fileSystemController.recognize(message);
    }

    protected void wrongParamsCount() throws WrongCommandFormatException {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {"config.xml"});
        Controller controller = (Controller) applicationContext.getBean("controller");
        throw new WrongCommandFormatException(controller.getWrongParamsCountExceptionMessage());
    }

    public String getAuthorizeCommand() {
        return authorizeCommand;
    }

    public void setAuthorizeCommand(String authorizeCommand) {
        this.authorizeCommand = authorizeCommand;
    }

    public String getRegisterCommand() {
        return registerCommand;
    }

    public void setRegisterCommand(String registerCommand) {
        this.registerCommand = registerCommand;
    }

    public String getWrongParamsCountExceptionMessage() {
        return wrongParamsCountExceptionMessage;
    }

    public void setWrongParamsCountExceptionMessage(String wrongParamsCountExceptionMessage) {
        this.wrongParamsCountExceptionMessage = wrongParamsCountExceptionMessage;
    }

    public void sendResponce(String message) {
        out.println(message);
    }

    public void sendError(String message) {
        out.println("ERROR :: " + message);
    }

    @Override
    public void run() {
        try {
            startConversation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startConversation() throws Exception {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {"config.xml"});
        Controller controller = (Controller) applicationContext.getBean("controller");
        setAuthorizeCommand(controller.getAuthorizeCommand());
        setRegisterCommand(controller.getRegisterCommand());
        AUTHORIZE_HELP = "Type '" + getAuthorizeCommand() + "' to login, or '" + getRegisterCommand() + "' to register.";

        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        String input;

        System.out.println("Wait for messages");

        while (!Thread.interrupted()) {
            input = in.readLine();
            if (input.equals("exit")) interrupt();
            else if (input.equalsIgnoreCase(getAuthorizeCommand()) && !authorized) authorize();
            else if (input.equalsIgnoreCase(getRegisterCommand()) && !authorized) register();
            else if (!authorized) {
                sendResponce(AUTHORIZE_HELP);
                System.out.println("User: " + clientNumber + " input :: "  + input);
            }
            else {
                try {
                    recognize(input);
                } catch (UnknownCommandException e) {
                    sendResponce("No such command found: " + input);
                }
            }
        }
    }

    public void register() throws Exception {
        Login = "";
        Password = "";
        KeyPair kp = null;
        while (kp == null) {
            kp = getRSAkeys();
        }
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();
        String publicString = Base64.encode(publicKey.getEncoded());

        out.println(publicString);
        out.println("Enter your login:");

        int countLines = Integer.parseInt(in.readLine());
        for (int i = 0; i < countLines; i++) Login = Login + in.readLine();

        Login = decryptRSA(Login, privateKey);
        int isMember = ServerDao.isMember(Login);

        if (isMember == -1) {
            sendError("Access denied: USER ALREADY EXISTS. " + AUTHORIZE_HELP);
            return;
        }
        else out.println("Enter your password:");

        countLines = Integer.parseInt(in.readLine());
        for (int i = 0; i < countLines; i++) Password = Password + in.readLine();

        Password = decryptRSA(Password, privateKey);

        ServerDao.addUser(Login, Password, clientSocket.getInetAddress().toString());

        authorized = true;
        Path root = Paths.get(System.getProperty("user.dir") + File.separator + Main.PROGRAM_NAME);
        if (!Files.exists(root))
            Files.createDirectory(root);
        Path user_root = Files.createDirectory(Paths.get(root + File.separator + "root_" + Login));
        setRoot(user_root);
        fileSystemController = new FileSystemController(out, user_root);
        sendResponce("Done. Now you are authorized. " + FileSystemController.FILE_HELP);
    }

    private void setRoot(Path dir) {
        rootDirectory = dir;
        currentDirectory = dir;
    }

    public void authorize() throws Exception {
        Login = "";
        Password = "";
        KeyPair kp = null;
        while (kp == null) {
            kp = getRSAkeys();
        }
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();
        String publicString = Base64.encode(publicKey.getEncoded());

        out.println(publicString);
        out.println("Enter your login:");

        int countLines = Integer.parseInt(in.readLine());
        for (int i = 0; i < countLines; i++) Login = Login + in.readLine();

        Login = decryptRSA(Login, privateKey);

        out.println("Enter your password:");

        countLines = Integer.parseInt(in.readLine());
        for (int i = 0; i < countLines; i++) Password = Password + in.readLine();

        Password = decryptRSA(Password, privateKey);

        ServerDao.updateUser(Login, Password, clientSocket.getInetAddress().toString());
        int memberStatus = ServerDao.isMember(Login, Password);

        if (memberStatus == 1) {
            authorized = true;
            Path user_root = Paths.get(System.getProperty("user.dir") + File.separator + Main.PROGRAM_NAME + File.separator + "root_" + Login);
            if (!Files.exists(user_root))
                Files.createDirectory(user_root);
            setRoot(user_root);
            fileSystemController = new FileSystemController(out, user_root);
            sendResponce("Access granted. Type commands. " + FileSystemController.FILE_HELP);
        } else if (memberStatus == -1) {
            sendError("Access denied: USER NOT FOUND. " + AUTHORIZE_HELP);
        } else if (memberStatus == 0) {
            sendError("Access denied: WRONG PASSWORD. " + AUTHORIZE_HELP);
        }
    }

    private KeyPair getRSAkeys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decryptRSA(String tmp, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] y = new BASE64Decoder().decodeBuffer(tmp);
        y = cipher.doFinal(y);
        return new String(y);
    }

}
