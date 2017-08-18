/*
 * Copyright 2013-2018 Lilinfeng.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package me.coding.panda.learning.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * 时间服务器 (服务端)
 */
public class TimeServer {

    public static void main(String[] args) {

        int port = 8080;
        ServerSocket server = null;

        try {
            server = new ServerSocket(port);
            System.out.println("时间服务器启动成功 Port : " + port);

            Socket socket = null;
            while (true) {
                socket = server.accept();
                new Thread(new TimeServerHandler(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("时间服务器关闭");
                server = null;
            }
        }
    }
}
