/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Edgaras
 */
public class Server2 {

    public static final String ENCODING = "UTF-8";
    private static boolean run = true;
    private static final int PORT = 8080;
    private static ServerSocket ss;
    public static final String WEB_DIR = "C:\\Users\\Edgaras\\Desktop\\BIT\\JAVA\\web";
    public static String parentsPath = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Server 2");
        try {
            ss = new ServerSocket(PORT);
            start();
        } catch (IOException ex) {
            System.err.println("Failed to bind to port: " + PORT);
        }
    }

    public static void stop() {
        run = false;
        try {
            ss.close();
        } catch (Exception ex) {
            //Ignored
        } finally {
            System.exit(0);
        }
    }

    public static void start() {
        while (run) {
            try {
                Socket s = ss.accept();
                handle(s);
            } catch (IOException ex) {
                if (!run) {
                    System.err.println("Connection Failed");
                }
            }
        }
    }

    public static void handle(Socket s) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), ENCODING));) {
            String req = br.readLine();
            if (req == null) {
                sendError(s, 404, "Not found");
                return;
            }
            req = req.split(" ")[1];
            if ("/end".equals(req)) {
                send(s, "<html><body>Bye.</body></html>");
                stop();
            } else {
                if ("/".equals(req)) {
                    req = "index.html";
                } else {
                    req = req.substring(1);
                }
                sendFile(req, s);
            }
        } catch (IOException ex) {
            System.err.println("Error handling connection");
        } finally {
            try {
                s.close();
            } catch (Exception ex) {
//                Ignored
            }
        }
    }

    public static void send(Socket s, String content) throws IOException {
        send(s, content, "text/html; charset=" + ENCODING);
    }

    public static void send(Socket s, String content, String type) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), ENCODING));) {
            bw.write("HTTP/1.1 200 OK\r\n");
//            bw.newLine();
            bw.write("Content-Type: " + type + "\r\n");
//            bw.newLine();
            bw.write("Content-Length: " + content.getBytes(ENCODING).length + "\r\n");
            bw.write("\r\n");
            bw.write(content);
            bw.flush();
        }
    }

    public static void sendFile(String name, Socket s) throws IOException {
        Path p = Paths.get(WEB_DIR, name);
        File web = p.toFile();
        if (web.isDirectory()) {
            generateDir(s, web);
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(web), ENCODING));) {
                try {
                    send(s, generateContent(br, web));
                } catch (IOException ex) {
                    System.err.println("Failed to send");
                }

            } catch (FileNotFoundException ex) {
                sendError(s, 404, "File not found");
            } catch (IOException ex) {
                sendError(s, 500, "Server error");
            }
        }
    }

    public static String getParentsPath(File file) {

        File parent = file.getParentFile();
        if (!("web".equals(parent.getName()))) {
            parentsPath = "/" + parent.getName() + parentsPath;
            getParentsPath(parent);
        }
        return parentsPath;
    }

    public static void generateDir(Socket s, File file) {
//          Generuosiu naujus kelius
        String localhost = "http://localhost:8080";
        Path filePath;
        String path = getParentsPath(file);
        String[] fileNames = file.list();
        File parent = file.getParentFile();

        String content = "<a style=\"display: block\" href=\"" + localhost + path + "\">../" + parent.getName() + "</a>\r\n";
        for (int i = 0; i < fileNames.length; i++) {
            filePath = Paths.get(file.getAbsolutePath(), fileNames[i]);
            File f = filePath.toFile();
            File parent2 = f.getParentFile();
            content += "<a style=\"display: block\" href=\"" + localhost + path + "/" + parent2.getName() + "/" + f.getName() + "\">" + f.getName() + "</a>\r\n";
        }
        parentsPath = "";
        try {
            send(s, content);
        } catch (IOException ex) {
            System.err.println("Failed to send");
        }

    }

    public static String generateContent(BufferedReader br, File web) throws IOException {
        String content = "";
        String line;

        while ((line = br.readLine()) != null) {
            content += line + "\r\n";
        }
        return content;
    }

    public static void sendError(Socket s, int code, String message) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), ENCODING));) {
            bw.write("HTTP/1.1 " + code + " " + message + "\r\n");
            bw.write("\r\n");
            bw.flush();
        }
    }
}
