/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

public class GvSnapItem_Text implements GvSnapItem {
	double x;
	double y;
	Color color;
	double r;
	String text;
	int align;
	public GvSnapItem_Text(double x, double y, int color, double r, String text, int align) {
		this.x = x;
		this.y = y;
		this.color = new Color((color>>16)&255, (color>>8)&255, color&255, 255 - (color>>24)&255);
		this.r = r;
		this.text = text;
		this.align = align;
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
		Font beforeFont = g.g.getFont();
		Font font = new Font(null, Font.CENTER_BASELINE, (int)Math.round(r*2*scale));
		g.g.setFont(font);
		Rectangle2D rect = font.getStringBounds(text, g.g.getFontRenderContext());
		if(align==0) {
			g.g.drawString(text, (int)Math.round(x*scale-rect.getCenterX()), (int)Math.round(y*scale-rect.getCenterY()));
		}
		else if(0<align) {
			g.g.drawString(text, (int)Math.round(x*scale-rect.getMinX()), (int)Math.round(y*scale-rect.getCenterY()));
		}
		else {
			g.g.drawString(text, (int)Math.round(x*scale-rect.getMaxX()), (int)Math.round(y*scale-rect.getCenterY()));
		}
		g.g.setFont(beforeFont);
	}
	@Override
	public void output() {
	}
}
