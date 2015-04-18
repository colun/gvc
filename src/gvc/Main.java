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
		if(1<=args.length && "-server".equals(args[0])) {
			ServerSocket server = new ServerSocket(2<=args.length ? Integer.parseInt(args[1]) : 11111);
			while(true) {
				Socket socket = server.accept();
				GvData data = new GvData(socket);
				GvPanel.newWindow(data);
			}
		}
		else if(2<=args.length && "-client".equals(args[0])) {
			Socket socket = new Socket(args[1], 3<=args.length ? Integer.parseInt(args[2]) : 11112);
			GvData data = new GvData(socket);
			GvPanel.newWindow(data);
		}
		else {
			GvData data = new GvData(1<=args.length ? args[0] : "sample.gv");
			GvPanel.newWindow(data);
		}
	}
}
