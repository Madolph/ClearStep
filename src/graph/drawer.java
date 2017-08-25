package graph;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.junit.Test;

import java.awt.*;
import java.awt.geom.*;

@SuppressWarnings("serial")

public class drawer extends JFrame {
	
	@Test
	public void start(){
		new drawer();
	}
	
	public drawer(){
		this.setSize(1000,600);
		this.setTitle("graph");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.add(new DrawStuff(), BorderLayout.CENTER);
		this.setVisible(true);
	}
	
	 private class DrawStuff extends JComponent{
		 
		 public void paint(Graphics g){
			 Graphics2D graph2 = (Graphics2D)g;
			 graph2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			 Shape drawLine = new Line2D.Float(20, 90, 55, 250);
		 }
	 }
}