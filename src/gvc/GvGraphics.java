/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class GvGraphics {
	public Graphics2D g;
	public BufferedImage image;
	private AffineTransform atf;
	private int nowWidth;
	private int nowHeight;
	private double nowScale;
	private double nowSx;
	private double nowSy;
	public void begin(int width, int height, double scale, double sx, double sy) {
		nowWidth = width;
		nowHeight = height;
		nowScale = scale;
		nowSx = sx;
		nowSy = sy;
		if(image==null || image.getWidth()<width || image.getHeight()<height) {
			image = new BufferedImage((int)(width*1.2), (int)(height*1.2), BufferedImage.TYPE_INT_ARGB);
			g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.setBackground(Color.white);
			atf = g.getTransform();
		}
		else {
			g.setTransform(atf);
		}
		g.setClip(0, 0, width, height);
		g.clearRect(0, 0, width, height);
		g.scale(scale, scale);
		g.translate(sx, sy);
		final double sc = 1.0/256;
		g.scale(sc, sc);
	}
	public void blt(double x, double y, double width, double height, GvGraphicsBlt obj) {
		int sx = Math.max(0, (int)Math.ceil((x+nowSx)*nowScale));
		int sy = Math.max(0, (int)Math.ceil((y+nowSy)*nowScale));
		int ex = Math.min((int)Math.floor((x+width+nowSx)*nowScale), nowWidth-1);
		int ey = Math.min((int)Math.floor((y+height+nowSy)*nowScale), nowHeight-1);

		if(sx<=ex && sy<=ey) {
			int w = ex-sx+1;
			int h = ey-sy+1;
			int[] rgbArray = new int[w*h];
			image.getRGB(sx, sy, w, h, rgbArray, 0, w);
			double dsx = sx/nowScale-nowSx;
			double dsy = sy/nowScale-nowSy;
			double dex = ex/nowScale-nowSx;
			double dey = ey/nowScale-nowSy;
			obj.blt(rgbArray, w, h, dsx, dsy, dex, dey);
			image.setRGB(sx, sy, w, h, rgbArray, 0, w);
		}
	}
	public void end(Graphics g0) {
		g0.drawImage(image, 0, 0, null);
	}
}
