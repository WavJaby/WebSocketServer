package com.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class Window extends JFrame implements ActionListener {
    private List<RequestHandler> clients;

    public Window(List<RequestHandler> clients) {
        this.clients = clients;
        initWindow();
    }

    public void initWindow() {
        setVisible(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize((int) (screenSize.getWidth() / 3), (int) (screenSize.getHeight() / 2));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setLayout(new FlowLayout());
        JButton button = new JButton("restart server");
        JButton button1 = new JButton("stop server");
        button.addActionListener(this);
        button1.addActionListener(this);
        add(button);
        add(button1);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String name = e.getActionCommand();
        if(name.equals("stop server")){
            Main.stopServer();
        }
        if(name.equals("restart server")){
            Main.stopServer();
            Main.initServer();
        }
    }
}
