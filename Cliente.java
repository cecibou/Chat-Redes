import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Cliente {
    private JTextArea chatTextArea;
    private JTextField inputField;
    private PrintWriter writer;
    private String nomeCliente;
    private Set<String> participantesOnline;

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

    // Cria e exibe a interface gráfica do cliente
    private void createAndShowGUI(String enderecoServidor, int numeroPorta, String nomeCliente) {
        this.nomeCliente = nomeCliente;
        this.participantesOnline = new HashSet<>();

        JFrame frame = new JFrame(this.nomeCliente);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel contentPane = new JPanel(new BorderLayout());

        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
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

    // Conecta o cliente ao servidor usando o endereço IP e número da porta fornecidos
    private void connectToServer(String enderecoServidor, int numeroPorta) {
        try {
            Socket socketCliente = new Socket(enderecoServidor, numeroPorta);
            writer = new PrintWriter(socketCliente.getOutputStream(), true);

            BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));

            new Thread(() -> {
                try {
                    String mensagemRecebida;
                    while ((mensagemRecebida = entrada.readLine()) != null) {
                        System.out.println("mensagemRecebida: " + mensagemRecebida);
                        if (mensagemRecebida.startsWith("ONLINE:")) {
                            String participante = mensagemRecebida.substring("ONLINE:".length());
                            participantesOnline.add(participante);
                            printParticipantesOnline();
                        } else if (mensagemRecebida.startsWith("OFFLINE:")) {
                            String participante = mensagemRecebida.substring("OFFLINE:".length());
                            participantesOnline.remove(participante);
                            printParticipantesOnline();
                        } else {
                            appendToChatArea(mensagemRecebida);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao receber mensagem do servidor: " + e.getMessage());
                }
            }).start();

            sendPresenceNotification(); // Envia uma notificação de presença ao servidor

        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor " + enderecoServidor + " na porta " + numeroPorta);
            System.exit(1);
        }
    }

    // Envia uma mensagem do cliente para o servidor
    private void sendMessage() {
        String message = inputField.getText();
        appendToChatArea(message);

        writer.println(message);
        writer.flush();

        inputField.setText("");
    }

    // Envia uma notificação de presença ao servidor a cada 5 segundos
    private void sendPresenceNotification() {
        new Thread(() -> {
            while (true) {
                writer.println("PRESENCE:" + nomeCliente);
                writer.flush();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // Adiciona uma mensagem à área de chat do cliente
    private void appendToChatArea(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String formattedMessage = "[" + message + "]\n"; // Adiciona o nome do remetente entre colchetes
                chatTextArea.append(formattedMessage);
                chatTextArea.setCaretPosition(chatTextArea.getDocument().getLength());
            }
        });
    }


    private void printParticipantesOnline() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatTextArea.setText("");
                chatTextArea.append("Participantes online:\n");
                for (String participante : participantesOnline) {
                    chatTextArea.append(participante + "\n");
                }
                chatTextArea.append("\n");
            }
        });
    }
}
