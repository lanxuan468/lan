package com.itheima;

import lombok.extern.slf4j.Slf4j;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

@Slf4j
public class Test1 {
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            People people = new People();
            people.start();
        }
        try {
            People.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (Integer id : Mailboxes.getIds()) {
            new Postman(id,"内容"+id).start();
        }
    }
}
@Slf4j
class People extends Thread{
    @Override
    public void run() {
        GuardedObject guardedObject = Mailboxes.creatGuardedObject();
        log.debug("开始收信 id{}",guardedObject.getId());
        Object mail = guardedObject.get(5000);
        log.debug("收到信 id{},内容:{}",guardedObject.getId(),mail);
    }
}
@Slf4j
class Postman extends Thread{
    private int id;
    private String mail;

    public Postman(int id, String mail) {
        this.id = id;
        this.mail = mail;
    }

    @Override
    public void run() {
        GuardedObject guardObject = Mailboxes.getGuardObject(id);
        log.debug("送信信 id:{},内容:{}",id,mail);
        guardObject.complete(mail);
    }
}


class Mailboxes{
    private static Map<Integer,GuardedObject> boexs=new Hashtable<>();

    private static int id=1;
    private static synchronized int generateId(){
        return id++;
    }
    public static GuardedObject getGuardObject(int id){
        return boexs.remove(id);
    }
    static GuardedObject creatGuardedObject(){
        GuardedObject go = new GuardedObject();
        boexs.put(go.getId(),go);
        return go;
    }
    public static Set<Integer> getIds(){
        return boexs.keySet();
    }
}


class GuardedObject{
    private int id;

    public int getId() {
        return id;
    }

    public void GuardedObject(int id) {
        this.id = id;
    }

    //结果
    private Object response;

    //获取结果方法
    public Object get(long timeout){

        long begin = System.currentTimeMillis();
        long passedTime = 0;
        synchronized (this){
      while (response==null){
          //这一轮应该等待的时间
          long waitTime=timeout-passedTime;
          if(waitTime<=0)break;

          try {
              this.wait(waitTime);
          } catch (InterruptedException e) {
              throw new RuntimeException(e);
          }
      }
        }
        passedTime=System.currentTimeMillis()- begin;
        return response;
    }
    //产生结果

    public synchronized void complete(Object response){
        //给response赋值
      this.response=response;
        this.notifyAll();
    }
}
