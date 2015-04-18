/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import javax.imageio.ImageIO;

public class GvSnapItem_Image implements GvSnapItem {
	double x;
	double y;
	double width;
	double height;
	BufferedImage image;
	char mode;
	static TreeMap<String, BufferedImage> cache = new TreeMap<String, BufferedImage>();
	public GvSnapItem_Image(double x, double y, double width, double height, String[] imageInfo) throws IOException {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		mode = ' ';
		int pos = 0;
		StringBuilder builder = new StringBuilder();
		String lastId = null;
		while(pos+1<=imageInfo.length) {
			if(pos==0) {
				String fn = imageInfo[pos];
				++pos;
				builder.append(fn);
				lastId = builder.toString();
				if(!cache.containsKey(lastId)) {
					BufferedImage img = ImageIO.read(new File(fn));
					cache.put(lastId, img);
				}
			}
			else {
				String op = imageInfo[pos];
				++pos;
				if(":S".equals(op) || ":TCO14MR3S".equals(op)) {
					assert(pos+2<=imageInfo.length);
					int newWidth = Integer.parseInt(imageInfo[pos]);
					int newHeight = Integer.parseInt(imageInfo[pos+1]);
					pos += 2;
					builder.append(String.format(" %s %d %d", op, newWidth, newHeight));
					String id = builder.toString();
					if(!cache.containsKey(id)) {
						BufferedImage lastImg = cache.get(lastId);
						BufferedImage img = TCO14MR3ImageScaleDown.scaleDown(lastImg, newWidth, newHeight);
						cache.put(id, img);
					}
					lastId = id;
				}
				else if(":R".equals(op)) {
					mode = 'R';
				}
				else if(":G".equals(op)) {
					mode = 'G';
				}
				else if(":B".equals(op)) {
					mode = 'B';
				}
			}
		}
		image = cache.get(lastId);
		assert(image!=null);
	}
	@Override
	public double getMinX() {
		return x;
	}
	@Override
	public double getMinY() {
		return y;
	}
	@Override
	public double getMaxX() {
		return x+width;
	}
	@Override
	public double getMaxY() {
		return y+height;
	}
	@Override
	public void paint(GvGraphics g, double scale) {
		if(mode=='R' || mode=='G' || mode=='B') {
			final int mask = mode=='R' ? 16711680 : mode=='G' ? 65280 : 255;
			g.blt(x, y, width, height, new GvGraphicsBlt() {
				@Override
				public void blt(int[] img, int w, int h, double sx, double sy, double ex, double ey) {
					double rw = image.getWidth()/width;
					double rh = image.getHeight()/height;
					double rx = rw * (ex-sx) / Math.max(1, w-1);
					double ry = rh * (ey-sy) / Math.max(1, h-1);
					int mx = image.getWidth()-1;
					int my = image.getHeight()-1;
					int minSrcX = Math.min(Math.max(0, (int)Math.round(rw*(sx-x))), mx);
					int minSrcY = Math.min(Math.max(0, (int)Math.round(rh*(sy-y))), my);
					int maxSrcX = Math.min(Math.max(0, (int)Math.round(rw*(ex-x))), mx);
					int maxSrcY = Math.min(Math.max(0, (int)Math.round(rh*(ey-y))), my);
					double sx2 = rw * (sx-x) - minSrcX;
					double sy2 = rh * (sy-y) - minSrcY;
					int srcW_dec = maxSrcX-minSrcX;
					int srcH_dec = maxSrcY-minSrcY;
					int srcW = srcW_dec+1;
					int srcH = srcH_dec+1;
					int[] src = new int[srcW*srcH];
					image.getRGB(minSrcX, minSrcY, srcW, srcH, src, 0, srcW);
					for(int yy=0; yy<h; ++yy) {
						for(int xx=0; xx<w; ++xx) {
							int srcX = Math.min(Math.max(0, (int)Math.floor(sx2 + xx * rx)), srcW_dec);
							int srcY = Math.min(Math.max(0, (int)Math.floor(sy2 + yy * ry)), srcH_dec);
							int srcC = src[srcY*srcW+srcX];
							int dstC = img[yy*w+xx];
							img[yy*w+xx] = (((dstC)&~mask)|((srcC)&mask));
						}
					}
				}
			});
		}
		else {
			g.g.drawImage(image, (int)Math.round(x*scale), (int)Math.round(y*scale), (int)Math.round(width*scale), (int)Math.round(height*scale), null);
		}
	}
	@Override
	public void output() {
	}
}
