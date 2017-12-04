/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;

public class GvSnapItem_Circle implements GvSnapItem {
	double x;
	double y;
	Color color;
	double r;
	public GvSnapItem_Circle(double x, double y, int color, double r) {
		this.x = x;
		this.y = y;
		this.color = new Color((color>>16)&255, (color>>8)&255, color&255, 255 - (color>>24)&255);
		this.r = r;
	}
	@Override
	public double getMinX() {
		return x-r;
	}
	@Override
	public double getMinY() {
		return y-r;
	}
	@Override
	public double getMaxX() {
		return x+r;
	}
	@Override
	public double getMaxY() {
		return y+r;
	}
	@Override
	public void paint(GvGraphics g, double scale) {
		g.g.setColor(color);
		int x1 = (int)Math.round((x-r)*scale);
		int x2 = (int)Math.round((x+r)*scale);
		int y1 = (int)Math.round((y-r)*scale);
		int y2 = (int)Math.round((y+r)*scale);
		g.g.fillOval(x1-1, y1-1, x2-x1+1, y2-y1+1);
	}
	@Override
	public void output() {
	}
}
