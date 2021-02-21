/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Graphics2D;

public interface GvSnapItem {

	double getMinX();
	double getMinY();
	double getMaxX();
	double getMaxY();
	void updateRect(Graphics2D g);
	void paint(GvGraphics g, double scale);
	void output();
	void addInputLink(String inputLink);
	String getInputLink(double x, double y);
}
