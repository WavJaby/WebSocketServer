package com.server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

public class AutoReload extends JFrame implements ActionListener {
    private final int windowWidth = 200;
    private final int windowHeight = 80;
    private int screenWidth;
    private int screenHeight;

    public AutoReload() {
        initWindow();
    }

    public void initWindow() {
        setVisible(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        setSize((int) (screenSize.getWidth() / 3), (int) (screenSize.getHeight() / 2));
        setSize(windowWidth, windowHeight);
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;
        setLocation(screenWidth - windowWidth, screenHeight - windowHeight - 30);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setAlwaysOnTop(true);

        setLayout(new FlowLayout());
        JButton button = new JButton("reload");
        button.addActionListener(this);
        add(button);
    }


    private Robot robot;

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("reload")) {
            Point info = MouseInfo.getPointerInfo().getLocation();
            int x = info.x, y = info.y;
            int nowX, nowY;

            Main.reloadClient();
            try {
                robot = new Robot();

//                Thread.sleep(300);
//
                nowX = screenWidth - 155;
                nowY = 50;
//                clickButton(nowX, nowY, KeyEvent.BUTTON1_MASK);

                Thread.sleep(200);
                clickButton(nowX, nowY, KeyEvent.BUTTON3_MASK);

                Thread.sleep(300);
                nowY = 230;
                clickButton(nowX, nowY, KeyEvent.BUTTON1_MASK);

//                Thread.sleep(500);
//                robot.keyPress(KeyEvent.VK_WINDOWS);
//                robot.keyPress(KeyEvent.VK_LEFT);
//                robot.keyRelease(KeyEvent.VK_LEFT);
//                robot.keyRelease(KeyEvent.VK_WINDOWS);
//                robot.keyPress(KeyEvent.VK_ESCAPE);
//                robot.keyRelease(KeyEvent.VK_ESCAPE);

//                Thread.sleep(500);
//                nowY = 40;
//                nowX = 135;
//                clickButton(nowX, nowY, KeyEvent.BUTTON1_MASK);

            } catch (AWTException | InterruptedException awtException) {
                awtException.printStackTrace();
            }
        }
    }

    private void clickButton(int x, int y, int event) {
        robot.mouseMove(x, y);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        robot.mousePress(event);
        robot.mouseRelease(event);
    }
}
