/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Runtime;
import java.lang.InterruptedException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	public static void main(String[] args) throws IOException, InterruptedException {
		String mode = "file";
		String ip = "";
		String port = "";
		String outputImagePath = "";
		String charset = "";
		String inputPath = null;
		boolean pipeFlag = false;
		for(String arg : args) {
			if(ip==null) {
				ip = arg;
			}
			else if(port==null) {
				port = arg;
			}
			else if(outputImagePath==null) {
				outputImagePath = arg;
			}
			else if(charset==null) {
				charset = arg;
			}
			else if("-server".equals(arg)) {
				mode = "server";
				port = null;
				inputPath = "";
			}
			else if("-client".equals(arg)) {
				mode = "client";
				ip = null;
				port = null;
				inputPath = "";
			}
			else if("-pipe".equals(arg)) {
				pipeFlag = true;
				inputPath = "";
			}
			else if("-image".equals(arg)) {
				mode = "image";
				outputImagePath = null;
			}
			else if("-sixel".equals(arg)) {
				mode = "sixel";
			}
			else if("-charset".equals(arg)) {
				charset = null;
			}
			else if(inputPath==null) {
				inputPath = arg;
			}
			else {
				assert false : "Too many args...";
			}
		}
		if(charset!=null && !"".equals(charset)) {
			GvData.setCharset(charset);
		}
		if("server".equals(mode)) {
			ServerSocket server = new ServerSocket(port==null ? 11111 : Integer.parseInt(port));
			while(true) {
				Socket socket = server.accept();
				GvData data = new GvData(socket);
				GvPanel.newWindow(data);
			}
		}
		else if("client".equals(mode)) {
			Socket socket = new Socket(ip==null ? "127.0.0.1" : ip, port==null ? 11112 : Integer.parseInt(port));
			GvData data = new GvData(socket);
			GvPanel.newWindow(data);
		}
		else if("image".equals(mode)) {
			String name = (inputPath==null ? "sample.gv" : inputPath);
			GvData data = new GvData(name);
			if(name.endsWith(".gv")) {
				name = name.substring(0, name.length()-3) + "-gv";
				new File(name).mkdirs();
			}
			data.outputImage(outputImagePath==null ? name + "/gv" : outputImagePath, 1024, 1024);
		}
		else if("sixel".equals(mode)) {
			Runtime.getRuntime().addShutdownHook(new Thread(new SaneTTY()));
			Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "stty raw -ignbrk brkint isig opost -echo < /dev/tty" }).waitFor();
			String name = (inputPath==null ? "sample.gv" : inputPath);
			GvData data;
			if(pipeFlag) {
				data = new GvData((Socket)null);
			}
			else {
				data = new GvData(name);
			}
			String tempFolder = "gv-temp-work";
			new File(tempFolder).mkdirs();
			new File(tempFolder).deleteOnExit();
			FileInputStream tty = new FileInputStream("/dev/tty");
			FileOutputStream ttyOut = new FileOutputStream("/dev/tty");
			data.showSixel(tty, ttyOut, tempFolder + "/gv", 1024, 1024);
		}
		else if(pipeFlag) {
			GvData data = new GvData((Socket)null);
			GvPanel.newWindow(data);
		}
		else {
			GvData data = new GvData(inputPath==null ? "sample.gv" : inputPath);
			GvPanel.newWindow(data);
		}
	}
}
