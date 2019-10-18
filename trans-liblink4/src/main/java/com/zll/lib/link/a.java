package com.zll.lib.link;
import java.io.IOException;

public class a extends Thread{
	    public a() { 
	    } 
	    
	    public void run() { 
	        for (int i = 0 ; i < 100 ; i ++) { 
	            try {
	                Thread.sleep(100) ;
	            } catch (InterruptedException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            } 
	            System.out.println(i);   
	        }
	    } 
	            
	    public static void main (String args[]) { 
	        a a = new a() ; 
	        //a.setDaemon(true) ; 
	        a.start() ; 
	        System.out.println("isDaemon=" + a.isDaemon()); 
	        /*try {
	            System.in.read() ;
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } */
	    }
	}
