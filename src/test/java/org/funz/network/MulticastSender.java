/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.funz.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

/**
 *
 * @author richet
 */
public class MulticastSender {

    //public final static String IP = "239.192.0.100";
    public final static int PORT = 8020;

    public static void main(String[] args) {
        String IP=args[0];
        try {
            MulticastSocket socket = new MulticastSocket();
            socket.setTimeToLive(16);
            System.out.println(socket);


            int i = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                System.out.println(""+i);

                StringBuffer sb = new StringBuffer();
                sb.append("" + (i++));
                sb.append('\n');
                byte data[] = sb.toString().getBytes();
                
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(IP), MulticastSender.PORT);
                packet.setData(data);

                socket.send(packet);
            }
        } catch (SocketException se) {
            System.err.println(se);
        } catch (IOException ie) {
            System.err.println(ie);
        }
    }
}
