import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private JTextArea chatTextArea;
    private JTextField inputField;
    private PrintWriter writer;
    private String nomeCliente;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Uso: java Cliente <endereço IP do servidor> <número da porta> <nome do cliente>");
            System.exit(1);
        }

        String enderecoServidor = args[0];
        int numeroPorta = Integer.parseInt(args[1]);
        String nomeCliente = args[2];

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Cliente().createAndShowGUI(enderecoServidor, numeroPorta, nomeCliente);
            }
        });
    }

    private void createAndShowGUI(String enderecoServidor, int numeroPorta, String nomeCliente) {
        this.nomeCliente = nomeCliente;

        JFrame frame = new JFrame("Cliente de Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());

        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        chatTextArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(chatTextArea);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        contentPane.add(inputField, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);

        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        connectToServer(enderecoServidor, numeroPorta);
    }

    private void connectToServer(String enderecoServidor, int numeroPorta) {
        try {
            Socket socketCliente = new Socket(enderecoServidor, numeroPorta);
            writer = new PrintWriter(socketCliente.getOutputStream(), true);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));

            new Thread(() -> {
                try {
                    String mensagemRecebida;
                    while ((mensagemRecebida = entrada.readLine()) != null) {
                        appendToChatArea(mensagemRecebida);
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao receber mensagem do servidor: " + e.getMessage());
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor " + enderecoServidor + " na porta " + numeroPorta);
            System.exit(1);
        }
    }

    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String formattedMessage = "[" + nomeCliente + "]: " + message + "\n";
                chatTextArea.append(formattedMessage);
                chatTextArea.setCaretPosition(chatTextArea.getDocument().getLength());
            }
        });
    }

    private void sendMessage() {
        String message = inputField.getText();
        appendToChatArea(message);

        writer.println(message);
        writer.flush();

        inputField.setText("");
    }
}