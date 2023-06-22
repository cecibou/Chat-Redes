import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JScrollBar;

public class Servidor {

    private ArrayList<PrintWriter> clientOutputStreams;
    private JTextArea chatTextArea;
    private JScrollPane scrollPane;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Servidor <número da porta>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        new Servidor().start(port);
    }

    public void start(int port) {
        clientOutputStreams = new ArrayList<>();

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Servidor iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nova conexão recebida de " + clientSocket.getInetAddress());

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket socket) {
            try {
                clientSocket = socket;
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream());
                clientOutputStreams.add(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            String message;

            try {
                while ((message = reader.readLine()) != null) {
                    System.out.println("Mensagem recebida: " + message);
                    sendToOtherClients(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientOutputStreams.remove(writer);
                    clientSocket.close();
                    System.out.println("Conexão encerrada com " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendToOtherClients(String message) {
            for (PrintWriter client : clientOutputStreams) {
                if (client != writer) {
                    client.println(message);
                    client.flush();
                }
            }
        }
    }
}
