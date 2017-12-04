/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;

public class GvSnapItem_Polygon implements GvSnapItem {
	double[] x;
	double[] y;
	int[] ix;
	int[] iy;
	Color color;
	double r;
	public GvSnapItem_Polygon(double[] x, double[] y, int color) {
		assert(2<=x.length);
		assert(x.length==y.length);
		this.x = x;
		this.y = y;
		this.ix = new int[x.length];
		this.iy = new int[y.length];
		this.color = new Color((color>>16)&255, (color>>8)&255, color&255, 255 - (color>>24)&255);
	}
	@Override
	public double getMinX() {
		assert(2<=x.length);
		assert(x.length==y.length);
		double minX = x[0];
		for(int i=1; i<x.length; ++i) {
			minX = Math.min(minX, x[i]);
		}
		return minX;
	}
	@Override
	public double getMinY() {
		assert(2<=x.length);
		assert(x.length==y.length);
		double minY = y[0];
		for(int i=1; i<y.length; ++i) {
			minY = Math.min(minY, y[i]);
		}
		return minY;
	}
	@Override
	public double getMaxX() {
		assert(2<=x.length);
		assert(x.length==y.length);
		double maxX = x[0];
		for(int i=1; i<x.length; ++i) {
			maxX = Math.max(maxX, x[i]);
		}
		return maxX;
	}
	@Override
	public double getMaxY() {
		assert(2<=x.length);
		assert(x.length==y.length);
		double maxY = y[0];
		for(int i=1; i<y.length; ++i) {
			maxY = Math.max(maxY, y[i]);
		}
		return maxY;
	}
	@Override
	public void paint(GvGraphics g, double scale) {
		g.g.setColor(color);
		assert(2<=x.length);
		assert(x.length==y.length);
		assert(x.length==ix.length);
		assert(x.length==iy.length);
		for(int i=0; i<x.length; ++i) {
			ix[i] = (int)Math.round(x[i]*scale);
			iy[i] = (int)Math.round(y[i]*scale);
		}
		g.g.fillPolygon(ix, iy, x.length);
	}
	@Override
	public void output() {
	}
}
