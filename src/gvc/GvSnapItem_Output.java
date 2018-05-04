/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

public class GvSnapItem_Output implements GvSnapItem {
	public final String output;
	public GvSnapItem_Output(String output) {
		this.output = output;
	}
	@Override
	public double getMinX() {
		return Double.MAX_VALUE;
	}
	@Override
	public double getMinY() {
		return Double.MAX_VALUE;
	}
	@Override
	public double getMaxX() {
		return -Double.MAX_VALUE;
	}
	@Override
	public double getMaxY() {
		return -Double.MAX_VALUE;
	}
	@Override
	public void paint(GvGraphics g, double scale) {
	}
	@Override
	public void output() {
		System.err.println(output);
	}
	@Override
	public void addInputLink(String inputLink) {
	}
	@Override
	public String getInputLink(double x, double y) {
		return null;
	}
}
