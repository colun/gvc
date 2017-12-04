/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GvPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	final GvData data;
	double[] timeList;
	final JFrame jf;
	int now;
	GvGraphics gvGraphics;
	GvSnap nowSnap;
	double scale = 1.0;
	double cx = 0.0;
	double cy = 0.0;
	double mx = 0.0;
	double my = 0.0;
	double cursorX = 0;
	double cursorY = 0;
	int mouseX = 0;
	int mouseY = 0;
	boolean autoMode = false;
	int autoModeCount = 0;
	Timer timer = new Timer(100, this);
	Timer autoModeTimer = new Timer(200, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			autoModeTimer.stop();
			if(timeList!=null && now<timeList.length-1) {
				++now;
				try {
					updateTime();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	});
	static GvPanel newWindow(GvData data) throws IOException {
		final JFrame jf = new JFrame();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int width = dim.width / 2;
		int height = dim.height / 2;
		jf.setBounds((dim.width-width)>>1, (dim.height-height)>>1, width, height);
		final GvPanel panel = new GvPanel(jf, data);
		panel.data.hook(panel);
		jf.getContentPane().add(panel);
		jf.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
			
			@Override
			public void componentResized(ComponentEvent arg0) {
			}
			
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}
			
			@Override
			public void componentHidden(ComponentEvent arg0) {
				try {
					panel.data.unhook(panel);
				} catch (IOException e) {
					e.printStackTrace();
				}
				jf.dispose();
			}
		});
		jf.setVisible(true);
		return panel;
	}
	void updateData() {
		timer.restart();
	}
	GvPanel(JFrame jf, GvData data) throws IOException {
		final GvPanel self = this;
		this.jf = jf;
		this.data = data;
		now = 0;
		timeList = data.getTimeList();
		autoModeCount = data.getAutoModeCount();
		jf.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent arg0) {
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				
			}
			@Override
			public void keyPressed(KeyEvent arg0) {
				int keyCode = arg0.getKeyCode();
				char keyChar = arg0.getKeyChar();
				if(keyCode==KeyEvent.VK_LEFT) {
					try {
						self.onLeftKey();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_RIGHT) {
					try {
						self.onRightKey();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_PAGE_UP) {
					try {
						self.onPageUpKey();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_PAGE_DOWN) {
					try {
						self.onPageDownKey();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_UP) {
					try {
						updateSelf(null, false, 4, false, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_DOWN) {
					try {
						updateSelf(null, false, -4, false, false);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD1) {
					try {
						self.onNumpadKey(-0.7, 0.7);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD2) {
					try {
						self.onNumpadKey(0, 1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD3) {
					try {
						self.onNumpadKey(0.7, 0.7);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD4) {
					try {
						self.onNumpadKey(-1, 0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD6) {
					try {
						self.onNumpadKey(1, 0);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD7) {
					try {
						self.onNumpadKey(-0.7, -0.7);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD8) {
					try {
						self.onNumpadKey(0, -1);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyCode==KeyEvent.VK_NUMPAD9) {
					try {
						self.onNumpadKey(0.7, -0.7);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else if(keyChar=='+') {
					try {
						GvPanel clone = GvPanel.newWindow(self.data);
						clone.now = self.now;
						clone.scale = self.scale;
						clone.cx = self.cx;
						clone.cy = self.cy;
						clone.mx = self.mx;
						clone.my = self.my;
						clone.cursorX = self.cursorX;
						clone.cursorY = self.cursorY;
						clone.mouseX = self.mouseX;
						clone.mouseY = self.mouseY;
						clone.updateTime();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, true, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		this.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void mousePressed(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, 0, false, arg0.isShiftDown());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				mouseX = -1;
				mouseY = -1;
				try {
					updateSelf(null, false, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, 0, false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		this.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				mouseX = arg0.getX();
				mouseY = arg0.getY();
				try {
					updateSelf(null, false, -arg0.getWheelRotation(), false, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		updateTime();
	}

	protected void onLeftKey() throws IOException {
		autoMode = false;
		if(1<=now) {
			--now;
			updateTime();
		}
	}
	protected void onRightKey() throws IOException {
		autoMode = false;
		if(timeList!=null && now<timeList.length-1) {
			++now;
			updateTime();
		}
	}
	protected void onPageUpKey() throws IOException {
		autoMode = false;
		if(1<=now) {
			now = Math.max(0, now - 20);
			updateTime();
		}
	}
	protected void onPageDownKey() throws IOException {
		autoMode = false;
		if(timeList!=null && now<timeList.length-1) {
			now = Math.min(now + 20, timeList.length-1);
			updateTime();
		}
	}
	protected void onNumpadKey(double dx, double dy) throws IOException {
		double newCx = Math.min(Math.max(-mx, cx+dx*scale*0.25), mx);
		double newCy = Math.min(Math.max(-my, cy+dy*scale*0.25), my);
		if(cx!=newCx || cy!=newCy) {
			cx = newCx;
			cy = newCy;
			updateUI();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg) {
		timer.stop();
		double nowTime = (timeList!=null && now<timeList.length) ? timeList[now] : 0.0;
		try {
			timeList = data.getTimeList();
			assert(timeList!=null);
			double minDiff = Double.MAX_VALUE;
			now = 0;
			for(int i=0; i<timeList.length; ++i) {
				double diff = Math.abs(nowTime - timeList[i]);
				if(diff<minDiff) {
					minDiff = diff;
					now = i;
				}
			}
			updateTime();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void updateTime() throws IOException {
		if(timeList!=null && now<timeList.length) {
			double time = timeList[now];
			if(now==timeList.length-1) {
				autoMode = true;
			}
			nowSnap = data.getSnap(time);
			nowSnap.output();
			updateUI();
			int amc = data.getAutoModeCount();
			if(amc!=autoModeCount) {
				autoModeCount = amc;
				autoMode = true;
			}
			if(autoMode) {
				autoModeTimer.restart();
			}
			return;
		}
		nowSnap = null;
	}

	protected void updateCenter() {
		cx = Math.min(Math.max(-mx, cx), mx);
		cy = Math.min(Math.max(-my, cy), my);
	}
	protected void updateSelf(Graphics g, boolean mouseDown, int zoom, boolean zoom2, boolean shiftClick) throws IOException {
		if(nowSnap==null) {
			return;
		}
		double dx = nowSnap.maxX - nowSnap.minX;
		double dy = nowSnap.maxY - nowSnap.minY;
		if(dx<=0 || dy<=0) {
			return;
		}
		Dimension dim = this.getSize();
		double width = Math.max(1, dim.width);
		double height = Math.max(1, dim.height);
		double maxD = Math.max(dx, dy);
		double scale;
		double sx;
		double sy;
		if(dx*height < dy*width) {
			my = (1-this.scale)*0.5;
			scale = height/(dy*this.scale);
			if(scale*dx<=width) {
				mx = 0;
			}
			else {
				mx = (dx-width/scale)/maxD * 0.5;
			}
		}
		else {
			mx = (1-this.scale)*0.5;
			scale = width/(dx*this.scale);
			if(scale*dy<=height) {
				my = 0;
			}
			else {
				my = (dy-height/scale)/maxD * 0.5;
			}
		}
		updateCenter();
		double beforeCursorX = cursorX;
		double beforeCursorY = cursorY;
		if(zoom2) {
			cx = (cursorX-(mouseX-width*0.5)/scale-dx*0.5-nowSnap.minX) / maxD;
			cy = (cursorY-(mouseY-height*0.5)/scale-dy*0.5-nowSnap.minY) / maxD;
			updateCenter();
			return;
		}
		cursorX = (mouseX-width*0.5)/scale+dx*0.5+nowSnap.minX+maxD*cx;
		cursorY = (mouseY-height*0.5)/scale+dy*0.5+nowSnap.minY+maxD*cy;
		if(mouseDown) {
			double dcx = cursorX - beforeCursorX;
			double dcy = cursorY - beforeCursorY;
			double oldCx = cx;
			double oldCy = cy;
			cx -= dcx/maxD;
			cy -= dcy/maxD;
			updateCenter();
			if(oldCx!=cx || oldCy!=cy) {
				updateUI();
				return;
			}
		}
		if(zoom!=0) {
			double newScale = Math.min(Math.max(0.01, this.scale * Math.pow(0.8, zoom*0.25)), 1.0);
			if(this.scale!=newScale) {
				this.scale = newScale;
				updateSelf(null, false, 0, true, false);
				updateUI();
				return;
			}
		}
		double time = nowSnap.time;
		if(shiftClick) {
			data.sendInput(time, cursorX, cursorY);
		}
		if(0<=mouseX && 0<=mouseY && nowSnap.minX<=cursorX && cursorX<=nowSnap.maxX && nowSnap.minY<=cursorY && cursorY<=nowSnap.maxY) {
			jf.setTitle(String.format("time %f ( %d / %d ) (%d, %d) (%f, %f)", time, now+1, timeList.length, (int)(cursorX+0.5), (int)(cursorY+0.5), cursorX, cursorY));
		}
		else {
			jf.setTitle(String.format("time %f ( %d / %d )", time, now+1, timeList.length));
		}
		sx = (width/scale-dx)*0.5-nowSnap.minX-maxD*cx;
		sy = (height/scale-dy)*0.5-nowSnap.minY-maxD*cy;
		if(g!=null) {
			if(gvGraphics==null) {
				gvGraphics = new GvGraphics();
			}
			gvGraphics.begin(dim.width, dim.height, scale, sx, sy);
			/*
			g.clearRect(0, 0, dim.width, dim.height);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			AffineTransform atf = g2.getTransform();
			g2.scale(scale, scale);
			g2.translate(sx, sy);
			final double sc = 1.0/256;
			g2.scale(sc, sc);
			*/
			nowSnap.paint(gvGraphics, 256.0);
			//g2.setTransform(atf);
			gvGraphics.end(g);
		}
	}
	@Override
	public void paint(Graphics g) {
		try {
			updateSelf(g, false, 0, false, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
