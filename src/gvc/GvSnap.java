/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.util.ArrayList;
import java.util.List;

public class GvSnap {
	final double time;
	final double minX;
	final double minY;
	final double maxX;
	final double maxY;
	public GvSnap(double time, double minX, double minY, double maxX, double maxY) {
		this.time = time;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		items = new ArrayList<GvSnapItem>();
	}

	List<GvSnapItem> items;
	public void addItem(GvSnapItem item) {
		items.add(item);
	}
	public void paint(GvGraphics g, double scale) {
		for(GvSnapItem item : items) {
			item.paint(g, scale);
		}
	}
	public void output() {
		System.out.printf("<time %f>\n", time);
		for(GvSnapItem item : items) {
			item.output();
		}
		System.out.println("</time>");
	}

}
