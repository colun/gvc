/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.image.BufferedImage;

public class TCO14MR3ImageScaleDown {
	public static BufferedImage scaleDown(BufferedImage src, int dstWidth, int dstHeight) {
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int srcWH = srcWidth*srcHeight;
		int srcHalfWH = srcWH>>1;
		int[] srcData = new int[srcWH];
		src.getRGB(0, 0, srcWidth, srcHeight, srcData, 0, srcWidth);
		int[][] integralR = new int[srcHeight+1][srcWidth+1];
		int[][] integralG = new int[srcHeight+1][srcWidth+1];
		int[][] integralB = new int[srcHeight+1][srcWidth+1];
		for(int y=0; y<srcHeight; ++y) {
			for(int x=0; x<srcWidth; ++x) {
				integralR[y+1][x+1] = ((srcData[y*srcWidth+x]>>16)&255) - integralR[y][x] + integralR[y][x+1] + integralR[y+1][x];
				integralG[y+1][x+1] = ((srcData[y*srcWidth+x]>>8)&255) - integralG[y][x] + integralG[y][x+1] + integralG[y+1][x];
				integralB[y+1][x+1] = (srcData[y*srcWidth+x]&255) - integralB[y][x] + integralB[y][x+1] + integralB[y+1][x];
			}
		}
		int dstWH = dstWidth*dstHeight;
		int[] dstData = new int[dstWH];
		for(int y=0; y<dstHeight; ++y) {
			int psy = y * srcHeight;
			int sy = psy / dstHeight;
			int sym = psy % dstHeight;
			int symi = dstHeight-sym;
			int pey = psy + srcHeight;
			int ey = pey / dstHeight;
			int eym = pey % dstHeight;
			for(int x=0; x<dstWidth; ++x) {
				int psx = x * srcWidth;
				int sx = psx / dstWidth;
				int sxm = psx % dstWidth;
				int sxmi = dstWidth-sxm;
				int pex = psx + srcWidth;
				int ex = pex / dstWidth;
				int exm = pex % dstWidth;
				int sumR = ((srcData[sy*srcWidth+sx]>>16)&255) * (sxmi*symi);
				int sumG = ((srcData[sy*srcWidth+sx]>>8)&255) * (sxmi*symi);
				int sumB = (srcData[sy*srcWidth+sx]&255) * (sxmi*symi);
				if(exm!=0) {
					sumR += ((srcData[sy*srcWidth+ex]>>16)&255) * (exm*symi);
					sumG += ((srcData[sy*srcWidth+ex]>>8)&255) * (exm*symi);
					sumB += (srcData[sy*srcWidth+ex]&255) * (exm*symi);
				}
				if(eym!=0) {
					sumR += ((srcData[ey*srcWidth+sx]>>16)&255) * (sxmi*eym);
					sumG += ((srcData[ey*srcWidth+sx]>>8)&255) * (sxmi*eym);
					sumB += (srcData[ey*srcWidth+sx]&255) * (sxmi*eym);
					if(exm!=0) {
						sumR += ((srcData[ey*srcWidth+ex]>>16)&255) * (exm*eym);
						sumG += ((srcData[ey*srcWidth+ex]>>8)&255) * (exm*eym);
						sumB += (srcData[ey*srcWidth+ex]&255) * (exm*eym);
					}
				}
				if(sx+1<ex) {
					sumR += (integralR[sy][sx+1]-integralR[sy][ex]-integralR[sy+1][sx+1]+integralR[sy+1][ex]) * (dstWidth*symi);
					sumG += (integralG[sy][sx+1]-integralG[sy][ex]-integralG[sy+1][sx+1]+integralG[sy+1][ex]) * (dstWidth*symi);
					sumB += (integralB[sy][sx+1]-integralB[sy][ex]-integralB[sy+1][sx+1]+integralB[sy+1][ex]) * (dstWidth*symi);
					if(sy+1<ey) {
						sumR += (integralR[sy+1][sx+1]-integralR[sy+1][ex]-integralR[ey][sx+1]+integralR[ey][ex]) * dstWH;
						sumG += (integralG[sy+1][sx+1]-integralG[sy+1][ex]-integralG[ey][sx+1]+integralG[ey][ex]) * dstWH;
						sumB += (integralB[sy+1][sx+1]-integralB[sy+1][ex]-integralB[ey][sx+1]+integralB[ey][ex]) * dstWH;
					}
					if(eym!=0) {
						sumR += (integralR[ey][sx+1]-integralR[ey][ex]-integralR[ey+1][sx+1]+integralR[ey+1][ex]) * (dstWidth*eym);
						sumG += (integralG[ey][sx+1]-integralG[ey][ex]-integralG[ey+1][sx+1]+integralG[ey+1][ex]) * (dstWidth*eym);
						sumB += (integralB[ey][sx+1]-integralB[ey][ex]-integralB[ey+1][sx+1]+integralB[ey+1][ex]) * (dstWidth*eym);
					}
				}
				if(sy+1<ey) {
					sumR += (integralR[sy+1][sx]-integralR[sy+1][sx+1]-integralR[ey][sx]+integralR[ey][sx+1]) * (sxmi*dstHeight);
					sumG += (integralG[sy+1][sx]-integralG[sy+1][sx+1]-integralG[ey][sx]+integralG[ey][sx+1]) * (sxmi*dstHeight);
					sumB += (integralB[sy+1][sx]-integralB[sy+1][sx+1]-integralB[ey][sx]+integralB[ey][sx+1]) * (sxmi*dstHeight);
					if(exm!=0) {
						sumR += (integralR[sy+1][ex]-integralR[sy+1][ex+1]-integralR[ey][ex]+integralR[ey][ex+1]) * (exm*dstHeight);
						sumG += (integralG[sy+1][ex]-integralG[sy+1][ex+1]-integralG[ey][ex]+integralG[ey][ex+1]) * (exm*dstHeight);
						sumB += (integralB[sy+1][ex]-integralB[sy+1][ex+1]-integralB[ey][ex]+integralB[ey][ex+1]) * (exm*dstHeight);
					}
				}
				int cR = (sumR+srcHalfWH) / srcWH;
				int cG = (sumG+srcHalfWH) / srcWH;
				int cB = (sumB+srcHalfWH) / srcWH;
				dstData[y*dstWidth+x] = (cR<<16) | (cG<<8) | cB;
			}
		}
		BufferedImage img = new BufferedImage(dstWidth, dstHeight, src.getType());
		img.setRGB(0, 0, dstWidth, dstHeight, dstData, 0, dstWidth);
		return img;
	}
}
