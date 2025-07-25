package cn.wolfcode.ws;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wolfcode-lanxw
 */
@ServerEndpoint("/{token}")
@Component
public class OrderWSServer {
    public static ConcurrentHashMap<String,Session> clients = new ConcurrentHashMap<>();
    //浏览器和服务器在建立连接的时候会调用此方法
    //建立关系
    @OnOpen
    public void onOpen(Session session, @PathParam( "token") String token){
        System.out.println("浏览器和服务器建立连接:"+token);
        //建立和浏览器的会话的映射关系
        clients.put(token,session);
    }
    //浏览器和服务器之间断开连接之后会调用此方法.
    //移除关系
    @OnClose
    public void onClose(@PathParam( "token") String token){
        System.out.println("浏览器和服务器断开连接:"+token);
        clients.remove(token);
    }
    //在服务器和浏览器通讯过程中出现异常会调用此方法
    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }
}
