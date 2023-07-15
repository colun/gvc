/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.LinkedList;
import java.lang.Runtime;
import java.lang.InterruptedException;
import java.lang.Process;

import javax.imageio.ImageIO;

public class GvData {
	private static Charset charset = Charset.forName("UTF-8");
	private final Map<Double, List<Long>> snapMap;
	private final RandomAccessFile raf;
	private final Socket socket;
	private final BufferedWriter writer;
	private final Set<GvPanel> hookSet = new HashSet<GvPanel>();
	private final LinkedList<String> reserveInputQueue = new LinkedList<String>();
	private double minX;
	private double minY;
	private double maxX;
	private double maxY;
	private BufferedImage measureImage;
	private Graphics2D measureGraphics;
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
	private int reserveInputCount = 0;
	private int inputKeyboardFlags = 0;
	private int autoModeCount = 0;
	private int streamCount = 0;

	private byte[] rafBuf = new byte[65536];
	private long rafFP = 0;
	private int rafOff = 0;
	private int rafEnd = 0;
	private boolean rafEof = false;
	private boolean rafRF = false;
	public static void setCharset(String charset_) {
		charset = Charset.forName(charset_);
	}
	private static GvSnapItem buildSnapItem(String line, String[] tokens) throws IOException {
		if(1<=tokens.length) {
			String type = tokens[0];
			if("c".equals(type)) {//circle
				assert(3<=tokens.length);
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				int color = 4<=tokens.length ? (int)Long.parseLong(tokens[3]) : 0;
				double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
				return new GvSnapItem_Circle(x, y, color, r);
			}
			else if("p".equals(type)) {//polygon
				assert(6<=tokens.length);
				assert(tokens.length%2==0);
				int color = (int)Long.parseLong(tokens[1]);
				int ei = tokens.length/2-1;
				double[] x = new double[ei];
				double[] y = new double[ei];
				for(int i=0; i<ei; ++i) {
					x[i] = Double.parseDouble(tokens[i+i+2]);
					y[i] = Double.parseDouble(tokens[i+i+3]);
				}
				return new GvSnapItem_Polygon(x, y, color);
			}
			else if("l".equals(type)) {//line
				assert(5<=tokens.length);
				double x1 = Double.parseDouble(tokens[1]);
				double y1 = Double.parseDouble(tokens[2]);
				double x2 = Double.parseDouble(tokens[3]);
				double y2 = Double.parseDouble(tokens[4]);
				int color = 6<=tokens.length ? (int)Long.parseLong(tokens[5]) : 0;
				return new GvSnapItem_Line(x1, y1, x2, y2, color);
			}
			else if("t".equals(type)) {//text
				assert(3<=tokens.length);
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				int color = 4<=tokens.length ? (int)Long.parseLong(tokens[3]) : 0;
				double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
				StringBuilder sb = new StringBuilder();
				for(int i = 5; i < tokens.length; ++i) {
					sb.append(tokens[i]);
					sb.append(" ");
				}
				String text = sb.length() > 0 ? sb.toString() : "?";
				return new GvSnapItem_Text(x, y, color, r, text, 0);
			}
			else if("tl".equals(type)) {//text left
				assert(3<=tokens.length);
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				int color = 4<=tokens.length ? (int)Long.parseLong(tokens[3]) : 0;
				double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
				StringBuilder sb = new StringBuilder();
				for(int i = 5; i < tokens.length; ++i) {
					sb.append(tokens[i]);
					sb.append(" ");
				}
				String text = sb.length() > 0 ? sb.toString() : "?";
				return new GvSnapItem_Text(x, y, color, r, text, 1);
			}
			else if("tr".equals(type)) {//text right
				assert(3<=tokens.length);
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				int color = 4<=tokens.length ? (int)Long.parseLong(tokens[3]) : 0;
				double r = 5<=tokens.length ? Double.parseDouble(tokens[4]) : 0.5;
				StringBuilder sb = new StringBuilder();
				for(int i = 5; i < tokens.length; ++i) {
					sb.append(tokens[i]);
					sb.append(" ");
				}
				String text = sb.length() > 0 ? sb.toString() : "?";
				return new GvSnapItem_Text(x, y, color, r, text, -1);
			}
			else if("b".equals(type)) {//bitmap(image)
				assert(5<=tokens.length);
				double x = Double.parseDouble(tokens[1]) - 0.5;
				double y = Double.parseDouble(tokens[2]) - 0.5;
				double w = Double.parseDouble(tokens[3]);
				double h = Double.parseDouble(tokens[4]);
				String[] imageInfo = Arrays.copyOfRange(tokens, 5, tokens.length);
				return new GvSnapItem_Image(x, y, w, h, imageInfo);
			}
			else if("o".equals(type)) {//output
				String output = 2<=tokens.length ? line.substring(2) : "";
				return new GvSnapItem_Output(output);
			}
		}
		return null;
	}
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
		String ret =  new String(rafBuf, si, i-si, charset);
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
	private String toStringMouseEvent(String str, double time, double x, double y) {
		if(str!=null) {
			return String.format("k %f %s\n", time, str);
		}
		return String.format("%f %f %f\n", time, x, y);
	}
	public int eventMouse(GvSnap snap, double x, double y, int kind) throws IOException {
		synchronized (this) {
			String str = null;
			boolean flag = true;
			if(kind==1) {
				if(0<inputCount) {
					flag = false;
					str = snap.getInputLink(x, y);
					writer.write(toStringMouseEvent(str, snap.time, x, y));
					writer.flush();
					--inputCount;
				}
				else if(reserveInputQueue.size()<reserveInputCount) {
					flag = false;
					str = snap.getInputLink(x, y);
					reserveInputQueue.add(toStringMouseEvent(str, snap.time, x, y));
				}
			}
			if(0<inputCount || reserveInputQueue.size()<reserveInputCount) {
				if(flag) {
					str = snap.getInputLink(x, y);
				}
				return str==null ? 1 : 2;
			}
			return 0;
		}
	}
	public boolean eventKeyboard(double time, int keyCode, char keyChar) throws IOException {
		synchronized (this) {
			if(0<inputCount || reserveInputQueue.size()<reserveInputCount) {
				String str = null;
				if(KeyEvent.VK_A<=keyCode && keyCode<=KeyEvent.VK_Z) {
					if((inputKeyboardFlags & 1)!=0) {
						str = String.format("%c", keyChar);
					}
				}
				else if(KeyEvent.VK_0<=keyCode && keyCode<=KeyEvent.VK_9) {
					if((inputKeyboardFlags & 2)!=0) {
						str = String.format("%c", keyChar);
					}
				}
				else if(KeyEvent.VK_NUMPAD0<=keyCode && keyCode<=KeyEvent.VK_NUMPAD9) {
					if((inputKeyboardFlags & 4)!=0) {
						str = String.format("%c", keyChar);
					}
				}
				else if(keyCode==KeyEvent.VK_SPACE) {
					if((inputKeyboardFlags & 8)!=0) {
						str = " ";
					}
				}
				else if(keyCode==KeyEvent.VK_LEFT) {
					if((inputKeyboardFlags & 0x100)!=0) {
						str = "left";
					}
				}
				else if(keyCode==KeyEvent.VK_RIGHT) {
					if((inputKeyboardFlags & 0x100)!=0) {
						str = "right";
					}
				}
				else if(keyCode==KeyEvent.VK_UP) {
					if((inputKeyboardFlags & 0x100)!=0) {
						str = "up";
					}
				}
				else if(keyCode==KeyEvent.VK_DOWN) {
					if((inputKeyboardFlags & 0x100)!=0) {
						str = "down";
					}
				}
				else if(keyCode==KeyEvent.VK_ENTER) {
					if((inputKeyboardFlags & 0x200)!=0) {
						str = "enter";
					}
				}
				else if(keyCode==KeyEvent.VK_DELETE) {
					if((inputKeyboardFlags & 0x400)!=0) {
						str = "delete";
					}
				}
				else if(keyCode==KeyEvent.VK_BACK_SPACE) {
					if((inputKeyboardFlags & 0x800)!=0) {
						str = "backspace";
					}
				}
				if(str!=null) {
					String line = String.format("k %f %s\n", time, str);
					if(0<inputCount) {
						writer.write(line);
						writer.flush();
						--inputCount;
					}
					else if(reserveInputQueue.size()<reserveInputCount) {
						reserveInputQueue.add(line);
					}
					return true;
				}
			}
			return false;
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
					if(!reserveInputQueue.isEmpty()) {
						writer.write(reserveInputQueue.removeFirst());
						writer.flush();
					}
					else {
						++inputCount;
					}
				}
				else if("ip".equals(type)) {//input peek
					if(!reserveInputQueue.isEmpty()) {
						writer.write(reserveInputQueue.removeFirst());
						writer.flush();
					}
					else {
						writer.write("\n");
						writer.flush();
					}
				}
				else if("il".equals(type)) {//input link
					raf.seek(nowPos);
					raf.write(line.getBytes(charset));
					raf.write("\n".getBytes(charset));
					nowPos = raf.getFilePointer();
					raf.write("n\n".getBytes(charset));
				}
				else if("ir".equals(type)) {//input reserve
					if(2<=tokens.length) {
						reserveInputCount = Integer.parseInt(tokens[1]);
					}
					else {
						reserveInputCount = 10;
					}
				}
				else if("ik".equals(type)) {//input keyboard
					for(int i=1; i<tokens.length; ++i) {
						String arg = tokens[i];
						if("clear".equals(arg)) {
							inputKeyboardFlags = 0;
						}
						else if("alphabet".equals(arg)) {
							inputKeyboardFlags |= 1;
						}
						else if("number".equals(arg)) {
							inputKeyboardFlags |= 6;
						}
						else if("space".equals(arg)) {
							inputKeyboardFlags |= 8;
						}
						else if("graphic".equals(arg)) {
							inputKeyboardFlags |= 0xff;
						}
						else if("cursor".equals(arg)) {
							inputKeyboardFlags |= 0x100;
						}
						else if("enter".equals(arg)) {
							inputKeyboardFlags |= 0x200;
						}
						else if("delete".equals(arg)) {
							inputKeyboardFlags |= 0x400;
						}
						else if("backspace".equals(arg)) {
							inputKeyboardFlags |= 0x800;
						}
						else if("all".equals(arg)) {
							inputKeyboardFlags = 0xffffffff;
						}
					}
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
					raf.write(line.getBytes(charset));
					raf.write("\n".getBytes(charset));
					nowPos = raf.getFilePointer();
					raf.write("n\n".getBytes(charset));
					nowBeginPos = null;
				}
				else if("f".equals(type)) {//flush
					for(GvPanel panel : hookSet) {
						panel.updateData(true);
					}
				}
				else {
					GvSnapItem item = buildSnapItem(line, tokens);
					if(item!=null) {
						if(nowBeginPos==null) {
							nowBeginPos = nowPos;
						}
						item.updateRect(measureGraphics);
						minX = Math.min(minX, item.getMinX());
						minY = Math.min(minY, item.getMinY());
						maxX = Math.max(maxX, item.getMaxX());
						maxY = Math.max(maxY, item.getMaxY());
					}
					raf.seek(nowPos);
					raf.write(line.getBytes(charset));
					raf.write("\n".getBytes(charset));
					nowPos = raf.getFilePointer();
					raf.write("n\n".getBytes(charset));
				}
			}
			++streamCount;
			for(GvPanel panel : hookSet) {
				panel.updateData(false);
			}
		}
	}
	private void rollbackAll() throws IOException {
		snapMap.clear();
		raf.seek(0);
		savedPos = nowPos = 0;
		savedBeginPos = nowBeginPos = null;
		savedMinX = savedMinY = minX = minY = Double.MAX_VALUE;
		savedMaxX = savedMaxY = maxX = maxY = -Double.MAX_VALUE;
		savedMaxTime = maxTime = -Double.MAX_VALUE;
		savedTime = nowTime = 0;
	}
	public GvData(final Socket socket) throws IOException {
		snapMap = new TreeMap<Double, List<Long>>();
		raf = new RandomAccessFile(File.createTempFile("gvsocket_", ".gv"), "rw");
		this.socket = socket;
		this.writer = new BufferedWriter(new OutputStreamWriter(socket!=null ? socket.getOutputStream() : System.out, charset));
		measureImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		measureGraphics = measureImage.createGraphics();
		rollbackAll();
		final GvData self = this;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket!=null ? socket.getInputStream() : System.in, charset));
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
		measureImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		measureGraphics = measureImage.createGraphics();
		double maxTime = -Double.MAX_VALUE;
		double nowTime = 0;
		Long lastPos = getMyFilePointer();
		double miX = Double.MAX_VALUE;
		double miY = Double.MAX_VALUE;
		double mxX = -Double.MAX_VALUE;
		double mxY = -Double.MAX_VALUE;
		while(true) {
			String line = readMyLine();
			if(line==null) {
				break;
			}
			String[] tokens = line.split(" ");
			if(1<=tokens.length) {
				String type = tokens[0];
				if("n".equals(type)) {//new
					if(2<=tokens.length) {
						nowTime = Double.parseDouble(tokens[1]);
						maxTime = Math.max(maxTime, nowTime);
					}
					else {
						nowTime = Math.max(0, maxTime + 1);
					}
					lastPos = getMyFilePointer();
				}
				else {
					GvSnapItem item = buildSnapItem(line, tokens);
					if(item!=null) {
						if(lastPos!=null) {
							addSnapPos(nowTime, lastPos);
							lastPos = null;
							maxTime = Math.max(maxTime, nowTime);
						}
						item.updateRect(measureGraphics);
						miX = Math.min(miX, item.getMinX());
						miY = Math.min(miY, item.getMinY());
						mxX = Math.max(mxX, item.getMaxX());
						mxY = Math.max(mxY, item.getMaxY());
					}
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
						if("n".equals(type)) {//new
							break;
						}
						else if("il".equals(type)) {//input link
							if(2<=tokens.length) {
								result.addInputLink(line.substring(3));
							}
						}
						else {
							GvSnapItem item = buildSnapItem(line, tokens);
							if(item!=null) {
								result.addItem(item);
							}
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
	public void setCursorTTY(OutputStream ttyOut, int rows, int cols) throws IOException {
		ttyOut.write(String.format("%c[%d;%dH", 27, rows, cols).getBytes());
	}
	public int getCursorTTY(InputStream tty, OutputStream ttyOut) throws IOException {
		ttyOut.write(String.format("%c[6n", 27).getBytes());
		int mode = 0;
		int rows = 0;
		int cols = 0;
		while(true) {
			int inp = tty.read();
			if(mode==0) {
				if(inp==27) {
					mode = 1;
				}
			}
			else if(mode==1) {
				if(inp==91) {
					mode = 2;
				}
			}
			else if(mode==2) {
				if(48<=inp && inp<58) {
					rows = rows * 10 + (inp-48);
				}
				if(inp==59) {
					mode = 3;
				}
			}
			else if(mode==3) {
				if(48<=inp && inp<58) {
					cols = cols * 10 + (inp-48);
				}
				if(inp==82) {
					return rows * 65536 + cols;
				}
			}
		}
	}
	public void showSixel(InputStream tty, OutputStream ttyOut, String prefix, int maxWidth0, int maxHeight0) throws IOException, InterruptedException {
		int zoom = 0;
		GvGraphics gvGraphics = new GvGraphics();
		int now = 0;
		int rows = -1;
		int cols = -1;
		int lastWidth = -1;
		int lastHeight = -1;
		while(true) {
			{
				setCursorTTY(ttyOut, 127, 127);
				int packed_size = getCursorTTY(tty, ttyOut);
				int r = packed_size >> 16;
				int c = packed_size & 65535;
				if(cols!=c) {
					cols = c;
				}
				if(rows!=r) {
					rows = r;
					for(int i=0; i<rows; ++i) {
						ttyOut.write("\n".getBytes());
					}
				}
			}
			int timeListSize;
			double time = -1;
			synchronized (this) {
				double[] timeList = getTimeList();
				timeListSize = timeList.length;
				if(timeList.length==0) {
					ttyOut.write("0/0 ... N/A\n".getBytes());
				}
				else {
					now = Math.min(now, timeList.length-1);
					int maxWidth = maxWidth0;
					int maxHeight = maxHeight0;
					int zoom2 = zoom;
					while(2<=zoom2) {
						zoom2 -= 2;
						maxWidth += maxWidth;
						maxHeight += maxHeight;
					}
					while(zoom2<=-1) {
						zoom2 += 2;
						maxWidth >>= 1;
						maxHeight >>= 1;
					}
					if(0<zoom2) {
						maxWidth += (maxWidth >> 1);
						maxHeight += (maxHeight >> 1);
					}
					time = timeList[now];
					GvSnap nowSnap = getSnap(time);
					if(nowSnap==null) {
						break;
					}
					double width = Math.max(1, maxWidth);
					double height = Math.max(1, maxHeight);
					double dx = nowSnap.maxX - nowSnap.minX;
					double dy = nowSnap.maxY - nowSnap.minY;
					double maxD = Math.max(dx, dy);
					double scale;
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

					File file = new File(prefix==null ? "gv_temp.png" : (timeList.length<=1 ? String.format("%s.%d-%d.png", prefix, intWidth, intHeight) : String.format("%s.%f.%d-%d.png", prefix, time, intWidth, intHeight)));
					if(prefix==null || !file.exists()) {
						BufferedImage bi = new BufferedImage(intWidth, intHeight, BufferedImage.TYPE_INT_ARGB);
						gvGraphics.begin(intWidth, intHeight, scale, -nowSnap.minX, -nowSnap.minY);
						nowSnap.paint(gvGraphics, 256.0);
						gvGraphics.end(bi.getGraphics());
						ImageIO.write(bi, "png", file);
					}
					//ttyOut.write(new byte[] { 27, 91, 54, 110 });
					//nowSnap.output();
					boolean changedSizeFlag = false;
					if(lastWidth!=intWidth || lastHeight!=intHeight) {
						lastWidth = intWidth;
						lastHeight = intHeight;
						changedSizeFlag = true;
					}
					Process process = Runtime.getRuntime().exec(new String[] {"img2sixel", file.getPath()});
					InputStream istream = process.getInputStream();
					int max_size = 8192;
					byte[] b = new byte[max_size];
					int size = 0;
					boolean firstFlag = true;
					while(true) {
						int ret = istream.read(b, size, max_size-size);
						if(0<ret) {
							size += ret;
							if(size+size<max_size) {
								continue;
							}
						}
						if(0<size) {
							if(firstFlag) {
								firstFlag = false;
								setCursorTTY(ttyOut, 1, 1);
								if(changedSizeFlag) {
									ttyOut.write(String.format("%c[0J", 27).getBytes());
								}
							}
							ttyOut.write(b, 0, size);
							size = 0;
						}
						if(ret<=0) {
							break;
						}
					}
					process.waitFor();
					istream.close();
					ttyOut.write(String.format("%d/%d ... %f\n", now+1, timeList.length, time).getBytes());
				}
			}
			int mode = 0;
			int streamCount2;
			synchronized (this) {
				streamCount2 = streamCount;
			}
			while(true) {
				if(tty.available()==0) {
					synchronized (this) {
						if(streamCount2!=streamCount) {
							break;
						}
					}
					Thread.sleep(50);
					continue;
				}
				int inp = tty.read();
				//ttyOut.write(String.format("%d\n", inp).getBytes());
				if(inp==13) {//エンター
					eventKeyboard(time, KeyEvent.VK_ENTER, (char)0);
				}
				if(inp==32) {//スペース
					eventKeyboard(time, KeyEvent.VK_SPACE, ' ');
				}
				if(inp==127) {//バックスペース
					eventKeyboard(time, KeyEvent.VK_BACK_SPACE, (char)0);
				}
				if(mode==0) {
					if(inp==27) {//ESC
						mode = 1;
					}
					if('A'<=inp && inp<='Z') {
						eventKeyboard(time, inp - 'A' + KeyEvent.VK_A, (char)inp);
					}
					if('a'<=inp && inp<='z') {
						eventKeyboard(time, inp - 'a' + KeyEvent.VK_A, (char)inp);
					}
					if('0'<=inp && inp<='9') {
						eventKeyboard(time, inp - '0' + KeyEvent.VK_0, (char)inp);
					}
				}
				else if(mode==1) {
					if(inp==27) {//ESC
						return;
					}
					else if(inp==91) {//[
						mode = 2;
					}
					else {
						mode = 0;
					}
				}
				else if(mode==2) {
					mode = 0;
					if(inp==51) {//Del
						eventKeyboard(time, KeyEvent.VK_DELETE, (char)0);
					}
					if(inp==53) {//PgUp
						if(!eventKeyboard(time, KeyEvent.VK_PAGE_UP, (char)0)) {
							if(now!=0) {
								now = Math.max(0, now-20);
								break;
							}
						}
					}
					if(inp==54) {//PgDn
						if(!eventKeyboard(time, KeyEvent.VK_PAGE_DOWN, (char)0)) {
							if(now!=timeListSize-1) {
								now = Math.min(now+20, timeListSize-1);
								break;
							}
						}
					}
					if(inp==65) {//Up
						if(!eventKeyboard(time, KeyEvent.VK_UP, (char)0)) {
							++zoom;
							break;
						}
					}
					if(inp==66) {//Down
						if(!eventKeyboard(time, KeyEvent.VK_DOWN, (char)0)) {
							--zoom;
							break;
						}
					}
					if(inp==67) {//Right
						if(!eventKeyboard(time, KeyEvent.VK_RIGHT, (char)0)) {
							if(now!=timeListSize-1) {
								now = Math.min(now+1, timeListSize-1);
								break;
							}
						}
					}
					if(inp==68) {//Left
						if(!eventKeyboard(time, KeyEvent.VK_LEFT, (char)0)) {
							if(now!=0) {
								now = Math.max(0, now-1);
								break;
							}
						}
					}
				}
			}
		}
	}
}
