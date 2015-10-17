package tea.tess.client;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import sun.misc.BASE64Encoder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.SQLException;

/**
 * Created by kron on 12.10.15.
 */
public class Client {

    private static BufferedReader in;
    private static PrintWriter out;
    private static BufferedReader inu;
    private static final int PORT = 12537;

    public static void main(String[] args) throws Exception {
        try {
            inu = new BufferedReader(new InputStreamReader(System.in));
            Socket fromserver = null;
            System.out.println("Enter host name to connect on port " + PORT + ":");
            String host = inu.readLine();
            fromserver = new Socket(host, PORT);
            System.out.println("Connecting...");

            in = new BufferedReader(new InputStreamReader(fromserver.getInputStream()));
            out = new PrintWriter(fromserver.getOutputStream(), true);
            System.out.println("Welcome! Type 'authorize' or 'register'.");
            String fuser, fserver;

            while (true) {
                fuser = inu.readLine();
                if (fuser.equalsIgnoreCase("exit"))
                {
                    out.println(fuser);
                    fromserver.close();
                    System.out.println("Exiting client");
                    System.exit(0);
                }
                if (fuser.equalsIgnoreCase("register")) {
                    out.println(fuser);
                    register();
                } else if (fuser.equalsIgnoreCase("authorize")) {
                    out.println(fuser);
                    authorize();
                } else {
                    out.println(fuser);
                    String line = "";
                    while (!in.ready()) {
                    }
                    while (in.ready()) {
                        line = in.readLine();
                        System.out.println(line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Oops! Connection failed. Program is shutting down. Message: " + e);
        }
    }

    private static void register() throws Exception {
        String publicString = in.readLine();
        System.out.println(in.readLine());
        String Login = inu.readLine();
        Login = encryptRSA(Login, publicString);
        out.println(Login.split("\n").length);
        out.println(Login);

        String response = in.readLine();
        if (response.substring(0,5).equalsIgnoreCase("Error")) {
            System.out.println(response);
            return;
        } else {
            System.out.println(response);
        }

        String Pass = inu.readLine();
        Pass = encryptRSA(Pass, publicString);
        out.println(Pass.split("\n").length);
        out.println(Pass);

        System.out.println(in.readLine());
    }

    private static void authorize() throws Exception {
        String publicString = in.readLine();
        System.out.println(in.readLine());
        String Login = inu.readLine();
        Login = encryptRSA(Login, publicString);
        out.println(Login.split("\n").length);
        out.println(Login);

        System.out.println(in.readLine());

        String Pass = inu.readLine();
        Pass = encryptRSA(Pass, publicString);
        out.println(Pass.split("\n").length);
        out.println(Pass);

        System.out.println(in.readLine());
    }

    private static String encryptRSA(String tmp, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");

        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(key));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pkS = kf.generatePublic(spec);

        cipher.init(Cipher.ENCRYPT_MODE, pkS);
        byte[] x = cipher.doFinal(tmp.getBytes());
        tmp = new BASE64Encoder().encode(x);
        // System.out.println(tmp);
        return tmp;
    }
}
