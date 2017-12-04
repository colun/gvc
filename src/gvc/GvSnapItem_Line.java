/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;

public class GvSnapItem_Line implements GvSnapItem {
	double x1;
	double y1;
	double x2;
	double y2;
	Color color;
	public GvSnapItem_Line(double x1, double y1, double x2, double y2, int color) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.color = new Color((color>>16)&255, (color>>8)&255, color&255, 255 - (color>>24)&255);
	}
	@Override
	public double getMinX() {
		return Math.min(x1, x2);
	}
	@Override
	public double getMinY() {
		return Math.min(y1, y2);
	}
	@Override
	public double getMaxX() {
		return Math.max(x1, x2);
	}
	@Override
	public double getMaxY() {
		return Math.max(y1, y2);
	}
	@Override
	public void paint(GvGraphics g, double scale) {
		g.g.setColor(color);
		g.g.drawLine((int)Math.round(x1*scale), (int)Math.round(y1*scale), (int)Math.round(x2*scale), (int)Math.round(y2*scale));
	}
	@Override
	public void output() {
	}
}
