package com.zll.lib.link;

import java.io.IOException;

public class test extends Thread {
    public test() { 
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
        test test = new test() ; 
        test.setDaemon(true) ; 
        test.start() ; 
        System.out.println("isDaemon=" + test.isDaemon()); 
        try {
            System.in.read() ;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
    }
}