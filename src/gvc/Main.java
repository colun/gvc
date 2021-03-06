/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
	public static void main(String[] args) throws IOException {
		String mode = "file";
		String ip = "";
		String port = "";
		String outputImagePath = "";
		String charset = "";
		String inputPath = null;
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
				mode = "pipe";
				inputPath = "";
			}
			else if("-image".equals(arg)) {
				mode = "image";
				outputImagePath = null;
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
		else if("pipe".equals(mode)) {
			GvData data = new GvData((Socket)null);
			GvPanel.newWindow(data);
		}
		else if("image".equals(mode)) {
			String name = (inputPath==null ? "sample.gv" : inputPath);
			GvData data = new GvData(name);
			if(name.endsWith(".gv")) {
				name = name.substring(0, name.length()-3);
			}
			data.outputImage(outputImagePath==null ? name : outputImagePath, 1024, 1024);
		}
		else {
			GvData data = new GvData(inputPath==null ? "sample.gv" : inputPath);
			GvPanel.newWindow(data);
		}
	}
}
