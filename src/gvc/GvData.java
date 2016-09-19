/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import com.sun.prism.Graphics;

public class GvData {
	private final static Charset utf8 = Charset.forName("UTF-8");
	private final Map<Double, List<Long>> snapMap;
	private final RandomAccessFile raf;
	private final Socket socket;
	private final BufferedWriter writer;
	private final Set<GvPanel> hookSet = new HashSet<GvPanel>();
	private double minX;
	private double minY;
	private double maxX;
	private double maxY;
	private double savedMinX;
	private double savedMinY;
	private double savedMaxX;
	private double savedMaxY;
	private long nowPos;
	private long savedPos;
	private Long nowBeginPos;
	private Long savedBeginPos;
	private double savedMaxTime;
	private double maxTime;
	private double savedTime;
	private double nowTime;
	private int inputCount = 0;
	private int autoModeCount = 0;

	private byte[] rafBuf = new byte[65536];
	private long rafFP = 0;
	private int rafOff = 0;
	private int rafEnd = 0;
	private boolean rafEof = false;
	private boolean rafRF = false;
	private void seekMy(long fp) throws IOException {
		rafOff = 0;
		rafEnd = 0;
		rafEof = false;
		rafRF = false;
		rafFP = fp;
		raf.seek(fp);
	}
	private long getMyFilePointer() throws IOException {
		if(rafRF && rafOff<rafEnd && rafBuf[rafOff]==10) {
			++rafOff;
		}
		return rafFP - rafEnd + rafOff;
	}
	private String readMyLine() throws IOException {
		int len = rafEnd-rafOff;
		if(!rafEof && len<32768) {
			for(int i=0; i<len; ++i) {
				rafBuf[i] = rafBuf[rafOff+i];
			}
			rafOff = 0;
			rafEnd = len;
			int ret = raf.read(rafBuf, len, rafBuf.length-len);
			if(ret==-1) {
				rafEof = true;
			}
			else {
				rafEnd += ret;
				rafFP += ret;
			}
		}
		int si = rafOff;
		int i;
		for(i=rafOff; i<rafEnd; ++i) {
			byte b = rafBuf[i];
			if(b==13) {
				rafOff = i + 1;
				rafRF = true;
				break;
			}
			else if(b==10) {
				if(rafRF && i==rafOff) {
					si = i+1;
					rafRF = false;
					continue;
				}
				rafOff = i + 1;
				rafRF = false;
				break;
			}
		}
		if(i==rafEnd) {
			rafOff = i;
			rafRF = false;
		}
		if(si==rafEnd && rafEof) {
			return null;
		}
		String ret =  new String(rafBuf, si, i-si);
		return ret;
	}
	public void hook(GvPanel panel) {
		synchronized (this) {
			hookSet.add(panel);
		}
	}
	public void unhook(GvPanel panel) throws IOException {
		synchronized (this) {
			hookSet.remove(panel);
			if(hookSet.isEmpty()) {
				close();
			}
		}
	}
	private void addSnapPos(double time, Long pos) {
		List<Long> posList = snapMap.get(time);
		if(posList==null) {
			posList = new ArrayList<Long>();
			snapMap.put(time, posList);
		}
		posList.add(pos);
	}
	public double[] getTimeList() {
		synchronized (this) {
			Set<Double> set = snapMap.keySet();
			int size = set.size();
			if(savedBeginPos!=nowBeginPos && nowBeginPos!=null && !set.contains(nowTime)) {
				++size;
			}
			double[] ret = new double[size];
			int i = 0;
			for(double d : set) {
				ret[i] = d;
				++i;
			}
			if(i!=size) {
				ret[i] = nowTime;
				Arrays.sort(ret);
			}
			return ret;
		}
	}
	private void close() throws IOException {
		if(socket!=null) {
			socket.close();
		}
		raf.close();
	}
	public void sendInput(double time, double x, double y) throws IOException {
		synchronized (this) {
			if(0<inputCount) {
				writer.write(String.format("%f %f %f\n", time, x, y));
				writer.flush();
				--inputCount;
			}
		}
	}
	public int getAutoModeCount() {
		synchronized (this) {
			return autoModeCount;
		}
	}
	private void addLine(String line) throws IOException {
		synchronized (this) {
			String[] tokens = line.split(" ");
			if(1<=tokens.length) {
				String type = tokens[0];
				if("r".equals(type)) {//rollback
					minX = savedMinX;
					minY = savedMinY;
					maxX = savedMaxX;
					maxY = savedMaxY;
					nowPos = savedPos;
					nowTime = savedTime;
					nowBeginPos = savedBeginPos;
					maxTime = savedMaxTime;
				}
				else if("ra".equals(type)) {//rollback all
					rollbackAll();
				}
				else if("i".equals(type)) {//input
					++inputCount;
				}
				else if("a".equals(type)) {//input
					++autoModeCount;
				}
				else if("n".equals(type)) {//new
					savedMaxTime = maxTime;
					if(savedBeginPos!=nowBeginPos && nowBeginPos!=null) {
						addSnapPos(nowTime, nowBeginPos);
						maxTime = Math.max(maxTime, nowTime);
					}
					savedMinX = minX;
					savedMinY = minY;
					savedMaxX = maxX;
					savedMaxY = maxY;
					savedPos = nowPos;
					savedTime = nowTime;
					savedBeginPos = nowBeginPos;
					if(2<=tokens.length) {
						nowTime = Double.parseDouble(tokens[1]);
						maxTime = Math.max(maxTime, nowTime);
					}
					else {
						nowTime = Math.max(0, maxTime + 1);
					}
					raf.seek(nowPos);
					raf.write(line.getBytes(utf8));
					raf.write("\n".getBytes(utf8));
					nowPos = raf.getFilePointer();
					raf.write("n\n".getBytes(utf8));
					nowBeginPos = null;
				}
				else {
					if("c".equals(type)) {//circle
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(3<=tokens.length);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
						minX = Math.min(minX, x-r);
						minY = Math.min(minY, y-r);
						maxX = Math.max(maxX, x+r);
						maxY = Math.max(maxY, y+r);
					}
					else if("p".equals(type)) {//polygon
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(6<=tokens.length);
						assert(tokens.length%2==0);
						for(int i=2; i<tokens.length; i+=2) {
							double x = Double.parseDouble(tokens[i]);
							double y = Double.parseDouble(tokens[i+1]);
							minX = Math.min(minX, x);
							minY = Math.min(minY, y);
							maxX = Math.max(maxX, x);
							maxY = Math.max(maxY, y);
						}
					}
					else if("l".equals(type)) {//line
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(5<=tokens.length);
						double x1 = Double.parseDouble(tokens[1]);
						double y1 = Double.parseDouble(tokens[2]);
						double x2 = Double.parseDouble(tokens[3]);
						double y2 = Double.parseDouble(tokens[4]);
						minX = Math.min(minX, Math.min(x1, x2));
						minY = Math.min(minY, Math.min(y1, y2));
						maxX = Math.max(maxX, Math.max(x1, x2));
						maxY = Math.max(maxY, Math.max(y1, y2));
					}
					else if("t".equals(type)) {//text
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(3<=tokens.length);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
						minX = Math.min(minX, x-r);
						minY = Math.min(minY, y-r);
						maxX = Math.max(maxX, x+r);
						maxY = Math.max(maxY, y+r);
					}
					else if("tl".equals(type)) {//text left
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(3<=tokens.length);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
						minX = Math.min(minX, x);
						minY = Math.min(minY, y-r);
						maxX = Math.max(maxX, x+r+r);
						maxY = Math.max(maxY, y+r);
					}
					else if("tr".equals(type)) {//text right
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(3<=tokens.length);
						double x = Double.parseDouble(tokens[1]);
						double y = Double.parseDouble(tokens[2]);
						double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
						minX = Math.min(minX, x-r-r);
						minY = Math.min(minY, y-r);
						maxX = Math.max(maxX, x);
						maxY = Math.max(maxY, y+r);
					}
					else if("b".equals(type)) {//bitmap(image)
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						assert(5<=tokens.length);
						double x = Double.parseDouble(tokens[1]) - 0.5;
						double y = Double.parseDouble(tokens[2]) - 0.5;
						double w = Double.parseDouble(tokens[3]);
						double h = Double.parseDouble(tokens[4]);
						assert(0<=w && 0<=h);
						minX = Math.min(minX, x);
						minY = Math.min(minY, y);
						maxX = Math.max(maxX, x+w);
						maxY = Math.max(maxY, y+h);
					}
					else if("o".equals(type)) {//output
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
					}
					raf.seek(nowPos);
					raf.write(line.getBytes(utf8));
					raf.write("\n".getBytes(utf8));
					nowPos = raf.getFilePointer();
					raf.write("n\n".getBytes(utf8));
				}
			}
			for(GvPanel panel : hookSet) {
				panel.updateData();
			}
		}
	}
	private void rollbackAll() throws IOException {
		snapMap.clear();
		raf.seek(0);
		savedPos = nowPos = 0;
		savedBeginPos = nowBeginPos = null;
		savedMinX = savedMinY = minX = minY = Double.MAX_VALUE;
		savedMaxX = savedMaxY = maxX = maxY = Double.MIN_VALUE;
		savedMaxTime = maxTime = Double.MIN_VALUE;
		savedTime = nowTime = 0;
	}
	public GvData(final Socket socket) throws IOException {
		snapMap = new TreeMap<Double, List<Long>>();
		raf = new RandomAccessFile(File.createTempFile("gvsocket_", ".gv"), "rw");
		this.socket = socket;
		this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		rollbackAll();
		final GvData self = this;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					while(true) {
						String line = reader.readLine();
						if(line==null) {
							break;
						}
						self.addLine(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	public GvData(String path) throws IOException {
		snapMap = new TreeMap<Double, List<Long>>();
		raf = new RandomAccessFile(path, "r");
		socket = null;
		writer = null;
		double maxTime = Double.MIN_VALUE;
		double nowTime = 0;
		Long lastPos = getMyFilePointer();
		double miX = Double.MAX_VALUE;
		double miY = Double.MAX_VALUE;
		double mxX = Double.MIN_VALUE;
		double mxY = Double.MIN_VALUE;
		while(true) {
			String line = readMyLine();
			if(line==null) {
				break;
			}
			String[] tokens = line.split(" ");
			if(1<=tokens.length) {
				String type = tokens[0];
				if("c".equals(type)) {//circle
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(3<=tokens.length);
					double x = Double.parseDouble(tokens[1]);
					double y = Double.parseDouble(tokens[2]);
					double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
					miX = Math.min(miX, x-r);
					miY = Math.min(miY, y-r);
					mxX = Math.max(mxX, x+r);
					mxY = Math.max(mxY, y+r);
				}
				else if("p".equals(type)) {//polygon
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(6<=tokens.length);
					assert(tokens.length%2==0);
					for(int i=2; i<tokens.length; i+=2) {
						double x = Double.parseDouble(tokens[i]);
						double y = Double.parseDouble(tokens[i+1]);
						miX = Math.min(miX, x);
						miY = Math.min(miY, y);
						mxX = Math.max(mxX, x);
						mxY = Math.max(mxY, y);
					}
				}
				else if("l".equals(type)) {//line
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(5<=tokens.length);
					double x1 = Double.parseDouble(tokens[1]);
					double y1 = Double.parseDouble(tokens[2]);
					double x2 = Double.parseDouble(tokens[3]);
					double y2 = Double.parseDouble(tokens[4]);
					miX = Math.min(miX, Math.min(x1, x2));
					miY = Math.min(miY, Math.min(y1, y2));
					mxX = Math.max(mxX, Math.max(x1, x2));
					mxY = Math.max(mxY, Math.max(y1, y2));
				}
				else if("t".equals(type)) {//text
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(3<=tokens.length);
					double x = Double.parseDouble(tokens[1]);
					double y = Double.parseDouble(tokens[2]);
					double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
					miX = Math.min(miX, x-r);
					miY = Math.min(miY, y-r);
					mxX = Math.max(mxX, x+r);
					mxY = Math.max(mxY, y+r);
				}
				else if("tl".equals(type)) {//text left
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(3<=tokens.length);
					double x = Double.parseDouble(tokens[1]);
					double y = Double.parseDouble(tokens[2]);
					double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
					miX = Math.min(miX, x);
					miY = Math.min(miY, y-r);
					mxX = Math.max(mxX, x+r+r);
					mxY = Math.max(mxY, y+r);
				}
				else if("tr".equals(type)) {//text right
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(3<=tokens.length);
					double x = Double.parseDouble(tokens[1]);
					double y = Double.parseDouble(tokens[2]);
					double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
					miX = Math.min(miX, x-r-r);
					miY = Math.min(miY, y-r);
					mxX = Math.max(mxX, x);
					mxY = Math.max(mxY, y+r);
				}
				else if("b".equals(type)) {//bitmap(image)
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
					assert(5<=tokens.length);
					double x = Double.parseDouble(tokens[1]) - 0.5;
					double y = Double.parseDouble(tokens[2]) - 0.5;
					double w = Double.parseDouble(tokens[3]);
					double h = Double.parseDouble(tokens[4]);
					assert(0<=w && 0<=h);
					miX = Math.min(miX, x);
					miY = Math.min(miY, y);
					mxX = Math.max(mxX, x+w);
					mxY = Math.max(mxY, y+h);
				}
				else if("o".equals(type)) {//output
					if(lastPos!=null) {
						addSnapPos(nowTime, lastPos);
						lastPos = null;
						maxTime = Math.max(maxTime, nowTime);
					}
				}
				else if("n".equals(type)) {//new
					if(2<=tokens.length) {
						nowTime = Double.parseDouble(tokens[1]);
						maxTime = Math.max(maxTime, nowTime);
					}
					else {
						nowTime = Math.max(0, maxTime + 1);
					}
					lastPos = getMyFilePointer();
				}
			}
		}
		minX = miX;
		minY = miY;
		maxX = mxX;
		maxY = mxY;
	}
	public GvSnap getSnap(double d) throws IOException {
		synchronized (this) {
			List<Long> posList = snapMap.get(d);
			if(savedBeginPos!=nowBeginPos && nowBeginPos!=null && nowTime==d) {
				posList = (posList==null) ? new ArrayList<Long>() : new ArrayList<Long>(posList);
				posList.add(nowBeginPos);
			}
			assert(posList!=null);
			GvSnap result = new GvSnap(d, minX, minY, maxX, maxY);
			for (Long pos : posList) {
				seekMy(pos);
				while(true) {
					String line = readMyLine();
					if(line==null) {
						break;
					}
					String[] tokens = line.split(" ");
					if(1<=tokens.length) {
						String type = tokens[0];
						if("c".equals(type)) {//circle
							assert(3<=tokens.length);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							int color = 4<=tokens.length ? Integer.parseInt(tokens[3]) : 0;
							double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
							result.addItem(new GvSnapItem_Circle(x, y, color, r));
						}
						else if("p".equals(type)) {//polygon
							assert(6<=tokens.length);
							assert(tokens.length%2==0);
							int color = Integer.parseInt(tokens[1]);
							int ei = tokens.length/2-1;
							double[] x = new double[ei];
							double[] y = new double[ei];
							for(int i=0; i<ei; ++i) {
								x[i] = Double.parseDouble(tokens[i+i+2]);
								y[i] = Double.parseDouble(tokens[i+i+3]);
							}
							result.addItem(new GvSnapItem_Polygon(x, y, color));
						}
						else if("l".equals(type)) {//line
							assert(5<=tokens.length);
							double x1 = Double.parseDouble(tokens[1]);
							double y1 = Double.parseDouble(tokens[2]);
							double x2 = Double.parseDouble(tokens[3]);
							double y2 = Double.parseDouble(tokens[4]);
							int color = 6<=tokens.length ? Integer.parseInt(tokens[5]) : 0;
							result.addItem(new GvSnapItem_Line(x1, y1, x2, y2, color));
						}
						else if("t".equals(type)) {//text
							assert(3<=tokens.length);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							int color = 4<=tokens.length ? Integer.parseInt(tokens[3]) : 0;
							double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
							StringBuilder sb = new StringBuilder();
							for(int i = 5; i < tokens.length; ++i) {
								sb.append(tokens[i]);
								sb.append(" ");
							}
							String text = sb.length() > 0 ? sb.toString() : "?";
							result.addItem(new GvSnapItem_Text(x, y, color, r, text, 0));
						}
						else if("tl".equals(type)) {//text left
							assert(3<=tokens.length);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							int color = 4<=tokens.length ? Integer.parseInt(tokens[3]) : 0;
							double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
							StringBuilder sb = new StringBuilder();
							for(int i = 5; i < tokens.length; ++i) {
								sb.append(tokens[i]);
								sb.append(" ");
							}
							String text = sb.length() > 0 ? sb.toString() : "?";
							result.addItem(new GvSnapItem_Text(x, y, color, r, text, 1));
						}
						else if("tr".equals(type)) {//text right
							assert(3<=tokens.length);
							double x = Double.parseDouble(tokens[1]);
							double y = Double.parseDouble(tokens[2]);
							int color = 4<=tokens.length ? Integer.parseInt(tokens[3]) : 0;
							double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
							StringBuilder sb = new StringBuilder();
							for(int i = 5; i < tokens.length; ++i) {
								sb.append(tokens[i]);
								sb.append(" ");
							}
							String text = sb.length() > 0 ? sb.toString() : "?";
							result.addItem(new GvSnapItem_Text(x, y, color, r, text, -1));
						}
						else if("b".equals(type)) {//bitmap(image)
							assert(5<=tokens.length);
							double x = Double.parseDouble(tokens[1]) - 0.5;
							double y = Double.parseDouble(tokens[2]) - 0.5;
							double w = Double.parseDouble(tokens[3]);
							double h = Double.parseDouble(tokens[4]);
							String[] imageInfo = Arrays.copyOfRange(tokens, 5, tokens.length);
							result.addItem(new GvSnapItem_Image(x, y, w, h, imageInfo));
						}
						else if("o".equals(type)) {//output
							String output = 2<=tokens.length ? line.substring(2) : "";
							result.addItem(new GvSnapItem_Output(output));
						}
						else if("n".equals(type)) {//new
							break;
						}
					}
				}
			}
			return result;
		}
	}
	public void outputImage(String prefix, int maxWidth, int maxHeight) throws IOException {
		GvGraphics gvGraphics = new GvGraphics();
		Set<Double> keys = snapMap.keySet();
		for(Double d : keys) {
			GvSnap nowSnap = getSnap(d);
			if(nowSnap==null) {
				continue;
			}
			double width = Math.max(1, maxWidth);
			double height = Math.max(1, maxHeight);
			double dx = nowSnap.maxX - nowSnap.minX;
			double dy = nowSnap.maxY - nowSnap.minY;
			double maxD = Math.max(dx, dy);
			double scale;
			double sx;
			double sy;
			if(dx*height < dy*width) {
				scale = height/dy;
				width = dx * scale;
			}
			else {
				scale = width/dx;
				height = dy * scale;
			}
			int intWidth = Math.max((int)Math.ceil(width), 1);
			int intHeight = Math.max((int)Math.ceil(height), 1);

			BufferedImage bi = new BufferedImage(intWidth, intHeight, BufferedImage.TYPE_INT_ARGB);
			gvGraphics.begin(intWidth, intHeight, scale, -nowSnap.minX, -nowSnap.minY);
			nowSnap.paint(gvGraphics, 256.0);
			gvGraphics.end(bi.getGraphics());
			File file = new File(keys.size()==1 ? prefix + ".png" : String.format("%s.%f.png", prefix, d));
			ImageIO.write(bi, "png", file);
		}
	}
}
