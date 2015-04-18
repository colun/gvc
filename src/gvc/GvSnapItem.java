/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;


public interface GvSnapItem {

	double getMinX();
	double getMinY();
	double getMaxX();
	double getMaxY();
	void paint(GvGraphics g, double scale);
	void output();

}
