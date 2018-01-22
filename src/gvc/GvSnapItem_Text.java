/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

public class GvSnapItem_Text implements GvSnapItem {
	double x;
	double y;
	Color color;
	Color color2;
	double r;
	String text;
	int align;
	public GvSnapItem_Text(double x, double y, int color, double r, String text, int align) {
		this.x = x;
		this.y = y;
		int alpha = 255 - (color>>24)&255;
		int red = (color>>16)&255;
		int green = (color>>8)&255;
		int blue = color&255;
		int maxColor = Math.max(red, Math.max(green, blue));
		int red2 = 255-red;
		int green2 = 255-green;
		int blue2 = 255-blue;
		if(maxColor<128) {
			int minColor = Math.min(red, Math.min(green, blue));
			red2 += minColor;
			green2 += minColor;
			blue2 += minColor;
		}
		else {
			red2 -= (255-maxColor);
			green2 -= (255-maxColor);
			blue2 -= (255-maxColor);
		}
		this.color = new Color(red, green, blue, alpha);
		this.color2 = new Color(red2, green2, blue2, (alpha+1)>>1);
		this.r = r;
		this.text = text;
		this.align = align;
	}
	@Override
	public double getMinX() {
		if(align==0) {
			return x-r;
		}
		else if(0<align) {
			return x;
		}
		else {
			return x-r*2;
		}
	}
	@Override
	public double getMinY() {
		return y-r;
	}
	@Override
	public double getMaxX() {
		if(align==0) {
			return x+r;
		}
		else if(0<align) {
			return x+r*2;
		}
		else {
			return x;
		}
	}
	@Override
	public double getMaxY() {
		return y+r;
	}
	@Override
	public void paint(GvGraphics g, double scale) {
		double strokeSize = r*0.1*scale;
		Font font = new Font(null, Font.CENTER_BASELINE, (int)Math.round(r*2*scale));
		Shape shape = font.createGlyphVector(g.g.getFontRenderContext(), text).getOutline();
		Rectangle2D rect = shape.getBounds2D();
		AffineTransform at;
		if(align==0) {
			at = AffineTransform.getTranslateInstance(x*scale-rect.getCenterX(), y*scale-rect.getCenterY());
		}
		else if(0<align) {
			at = AffineTransform.getTranslateInstance(x*scale-rect.getMinX()+strokeSize, y*scale-rect.getCenterY());
		}
		else {
			at = AffineTransform.getTranslateInstance(x*scale-rect.getMaxX()-strokeSize, y*scale-rect.getCenterY());
		}
		Shape shape2 = at.createTransformedShape(shape);
		Stroke stroke = new BasicStroke((float)(strokeSize*2));
		Shape strokedShape = stroke.createStrokedShape(shape2);
		g.g.setColor(color2);
		g.g.fill(strokedShape);
		g.g.setColor(color);
		g.g.fill(shape2);
	}
	@Override
	public void output() {
	}
}
