package test;

import java.awt.*;
import javax.swing.*;
import java.util.Scanner;
import java.util.Random;
public class Main {
	public static void main(String[] args) {
		JFrame frame = new JFrame(); Scanner s = new Scanner(System.in); int x, y, r, R;
		frame.setSize(640, 480); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		x = 200;y=200;r=50;R=200;
		for(;;) {
			System.out.println("输入圆环的圆心坐标、内半径、外半径。输入单个-1退出。");
			DrawAnnulus drawAnnulus = new DrawAnnulus(x, y, r, R);
			frame.add(drawAnnulus, BorderLayout.CENTER);
			frame.setVisible(true);
			x = s.nextInt(); if(x == -1) { s.close(); System.exit(0); }
			y = s.nextInt(); r = s.nextInt(); R = s.nextInt();
		}
	}
}
class DrawAnnulus extends JPanel {
	int x, y, r, R, w, h; Random rnd = new Random(); Color c;
	DrawAnnulus(int x, int y, int r, int R) {
		this.x = x; this.y = y; this.r = r; this.R = R;
	}
	public void paint(Graphics g) {
		w = getWidth(); h = getHeight(); super.paint(g);
		if (r >= R) { System.out.println("内半径大于等于外半径。"); return; }
//		if (x < R + 10 || y < R + 10 || w - x < R + 10 || h - y < R + 10) { 
//			System.out.println("外圆离边缘太近（距离 < 10）或超出边缘。"); return;
//		}
		c = new Color(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)); g.setColor(c);
		g.fillOval(x - R, y - R, 2 * R, 2 * R);
		c = new Color(238, 238, 238); g.setColor(c);
		g.fillOval(x - r, y - r, 2 * r, 2 * r);
	}
}