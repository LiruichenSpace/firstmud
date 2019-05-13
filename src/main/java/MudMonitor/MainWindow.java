package MudMonitor;

import Characters.BlankPerson;
import Characters.CharacterType;
import Characters.User;
import Commands.FirstCommand;
import Commands.LookType;
import GameMaps.BlankRoom;
import GameMaps.Diraction;
import GameMaps.GameMap;
import Items.Item;
import Skills.AbstractSkill;
import Skills.SkillTypes;
import Utils.DBCPManager;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * 评价：又臭又长，结构像是补丁摞补丁，一定要好好研究多线程单例模式问题，为什么总是会阻塞....
 * 地图支持文件扩展是一件开心的设计，至少有兴趣就能写小说玩，还能直接跑起来（笑）
 * 并发度堪忧，怎么说都没有脸面说这个系统支持高并发，人够多的话怕是得等到程序员找到女朋友
 */
public class MainWindow extends JFrame{
    private JTextArea logs;
    private JScrollPane scrollPane;
    //公共访问界面
    private ServerSocket ss;
    private ServerSocket broadcastServer;
    private ThreadPoolExecutor exec;
    private DBCPManager dbcpManager;
    //连接需求对象
    public HashMap<String,OutputStream> onlineDict;
    public HashMap<SkillTypes, AbstractSkill> skillDict;
    public GameMap mainMap;
    static private MyLock lock= new MyLock();
    //游戏系统支持表
    /**
     * 监控窗体构造，系统资源配置以及死循环监听
     */
    public MainWindow() {
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(512);
        RejectedExecutionHandler policy = new ThreadPoolExecutor.DiscardPolicy();
        exec = new ThreadPoolExecutor(poolSize, poolSize,
                0, TimeUnit.SECONDS,
                queue,
                policy);
        dbcpManager=new DBCPManager();
        //处理线程池
        setTitle("Mud服务端监控台");
        logs=new JTextArea();
        scrollPane=new JScrollPane();
        logs.setBorder(new BevelBorder(BevelBorder.RAISED));
        setLayout(new BorderLayout());
        getContentPane().add(scrollPane,BorderLayout.CENTER);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(200,100,400,600);
        scrollPane.setViewportView(logs);
        logs.append("Mud服务端启动中\n");
        //设置监控台窗体
        onlineDict=new HashMap<String, OutputStream>();
        skillDict= new HashMap<SkillTypes, AbstractSkill>();
        mainMap=new GameMap("mainMap.txt");
        //实例化系统数据;
        try {
            ss=new ServerSocket(8667);//用于收发交替的普通处理方式
            broadcastServer=new ServerSocket(8664);//用于主动广播，客户端开启线程监听，平时保持阻塞
        } catch (IOException e) {
            e.printStackTrace();
        }
        mainMap.saveMap();
        if(ss!=null){
            logs.append("服务器已启动\n");
            logs.setSelectionEnd(logs.getText().length());
            while(true){
                try {
                    Socket temp = ss.accept();
                    Socket broadcast = broadcastServer.accept();
                    exec.execute(new ServerRun(temp, broadcast));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //循环监听开启线程服务新用户
    }

    /**
     *获得物品（暂且没有需求）
     * @param user 用户
     * @param item 物品
     */
    void get(BlankPerson user, Item item){}

    /**
     * 普通攻击（大概应该通知整个房间的玩家？）
     * @param shooter 攻击者
     * @param target 目标
     * @return 返回攻击事件的描述
     */
    String  attack(BlankPerson shooter, BlankPerson target){
        synchronized (mainMap){
            BlankRoom room=mainMap.getRoom(shooter.room);
        }
        boolean flag=shooter.shoot(target);
        StringBuilder info=new StringBuilder();
        info.append(shooter.name);
        info.append("攻击了");
        info.append(target.name);
        if(target.currArmor==0){
            info.append("  ");info.append(target.name);info.append("的护甲碎裂");
        }
        if(flag){info.append("  ");info.append(target.name);info.append("死亡");}
        info.append("\n");
        return info.toString();
    }

    /**
     * 移动行为（内部处理了通知行为）
     * 备注:再同步块中放大量的代码绝对不是个好主意......幸好改了
     * @param user 用户
     * @param diraction 方向
     * @return 返回行为的描述
     */
    String move(User user, Diraction diraction){
        StringBuilder sb=new StringBuilder();
        BlankRoom room;
        boolean type=false;
        synchronized (mainMap) {
            room = mainMap.getRoom(user.room);
        }
            BlankRoom to=null;
            if(room.surround.get(diraction).equalsIgnoreCase("null")){
                sb.append("此方向没有房间");
            }
            else{
                synchronized (mainMap) {
                    to=mainMap.getRoom(room.surround.get(diraction));
                }
                type= to instanceof GameMap;
                if(type){
                    sb.append("你进入了" + to.name);
                    informUserOffline(user);
                    to=(GameMap)to;
                    ((GameMap) to).enter(user);
                    informUserOnline(user);
                }
                else {
                    sb.append("你进入了" + to.name);
                    informUserOut(user, diraction);
                    user.room = to.name;
                    informUserIn(user, Diraction.reverse(diraction));
                }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * 用于为玩家开启新线程，内部处理玩家逻辑
     * 通过线程池构造并执行
     */
    class ServerRun implements Runnable {
        Socket userS;
        Socket broadcastSocket;
        BufferedInputStream buffIn;
        BufferedOutputStream buffOut;
        String username;//须在内部获取用户名用来查找流
        private PrintWriter pw;
        private BufferedReader br;
        byte[] buffer;

        /**
         * 初始化部分，利用socket初始化以绑定唯一用户
         * @param communicateSocket 发来的socket连接
         */
        ServerRun(Socket communicateSocket, Socket broadcastSocket){
            userS=communicateSocket;
            this.broadcastSocket=broadcastSocket;
            buffer=new byte[1024];
            try {
                buffIn= new BufferedInputStream(communicateSocket.getInputStream());
                buffOut= new BufferedOutputStream(communicateSocket.getOutputStream());//获得输入输出流
                pw=new PrintWriter(buffOut);
                br=new BufferedReader(new InputStreamReader(buffIn));
            } catch (IOException e) {
                MyLogger.loge("Serverrun","build");
                e.printStackTrace();
            }
        }

        /**
         * 思路：信息收发成对，通过先发送长度固定长度读来避免阻塞
         * 所有交互性作业都在此内完成 ，需要的方法写在ServerRun类内
         * 以此为主，其他类为该类开放接口，该类处理时加锁
         * 概述：用户线程代码，包含所有用户处理逻辑
         * 抱怨：为什么这么恶臭冗长，我实在是菜啊........
         */
        public void run() {
            boolean flag=false;//判断是否需要插入新项，true插入，否则更新
            int count=1;
            String message=null;
            StringBuilder response=new StringBuilder();
            try {
                message=readMessage();
                username=message;
                Connection conn=null;
                synchronized (dbcpManager){
                    conn=dbcpManager.getConn();
                }
                String sql="select * from userinfo where username =?";
                ResultSet rs=null;
                User u=new User(username);
                try {
                    PreparedStatement pr=conn.prepareStatement(sql);
                    pr.setString(1, username);
                    rs=pr.executeQuery();
                    if(rs.next()){
                        u.initiate(rs);
                        doBroadcast(broadcastSocket.getOutputStream(),"数据读取完毕\n");
                    }
                    else{
                        flag=true;
                        doBroadcast(broadcastSocket.getOutputStream(),"未知用户名，已创建新用户\n");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                mainMap.enter(u);
                onlineDict.put(username, broadcastSocket.getOutputStream());
                informUserOnline(u);
                sendMessage("您现在位于："+u.room+"\n请输入命令：\n");
                message=readMessage();
                /////////////////////////////////////完成登入,循环处理
                while(!message.equalsIgnoreCase("quit")){
                    String[] command=message.split(" +");
                    //////////////////////////////////这可真是不忍直视的恶臭,强行解释命令,难受啊.....
                    switch(FirstCommand.valueOf(command[0])){
                        case 移动:{
                            response.append(move(u,Diraction.valueOf(command[1])));
                        }break;
                        case 技能:{

                        }break;
                        case 攻击:{

                        }break;
                        case 查看:{
                            if(command.length>1) {
                                if(command[1].equals("我")||command[1].equals("房间")
                                        ||command[1].equals("方向")||command[1].equals("角色")) {
                                    switch (LookType.valueOf(command[1])) {//switch二级目录
                                        case 我: {
                                            response.append(u.showStatus());
                                        }break;
                                        case 房间: {
                                            BlankRoom room = null;
                                            synchronized (mainMap) {
                                                room = mainMap.getRoom(u.room);
                                            }
                                            response.append(room.name);
                                            response.append("\n");
                                            response.append(room.description);
                                            response.append("房间内角色有:\n");
                                            int num = 0;
                                            for (String s : room.personList.keySet()) {
                                                response.append(s + "  ");
                                                num++;
                                                if (num == 5) {
                                                    response.append("\n");
                                                    num = 0;
                                                }
                                            }
                                        }break;
                                        case 方向: {
                                            BlankRoom room = null;
                                            synchronized (mainMap) {
                                                room = mainMap.getRoom(u.room);
                                            }
                                            if (command.length > 2) {
                                                String to = room.surround.get(command[2]);
                                                if (to == null) {
                                                    response.append("方向参数错误");
                                                } else {
                                                    response.append("该房间");
                                                    response.append(command[2]);
                                                    response.append("方向");
                                                    if (to.equals("null")) {
                                                        response.append("没有其他房间");
                                                    } else {
                                                        response.append("为");
                                                        response.append(room.surround.get(command[2]));
                                                    }
                                                }
                                            } else {
                                                int num = 0;
                                                for (Diraction d : room.surround.keySet()) {
                                                    response.append(d.name());
                                                    response.append(":");
                                                    response.append(room.surround.get(d));
                                                    response.append("  ");
                                                    num++;
                                                    if (num == 3) {
                                                        response.append("\n");
                                                        num = 0;
                                                    }
                                                }
                                            }
                                        }break;
                                        case 角色: {
                                            if (command.length > 2) {
                                                BlankRoom room = null;
                                                synchronized (mainMap) {
                                                    room = mainMap.getRoom(u.room);
                                                }
                                                BlankPerson temp=room.personList.get(command[2]);
                                                if(temp!=null) response.append(temp.showStatus());
                                                else response.append("该房间无此角色");
                                            } else {
                                                response.append("指令长度错误,请输入欲查看用户");
                                            }
                                        }break;
                                    }
                                }else{
                                    response.append("指令错误,可用指令选项:我,房间,方向,角色");
                                }
                            }else{
                                response.append("指令长度错误,可用指令选项:我,房间,方向,角色");
                            }
                        }break;
                        case 逃跑:{
                            BlankRoom room=null;
                            synchronized (mainMap){
                                room=mainMap.getRoom(u.room);
                            }
                            if(room instanceof GameMap){//说明是副本
                                GoBack(u);
                                response.append("您已回到行动基地");
                            }
                            else{
                                response.append("此处无法逃跑");
                            }
                        }break;
                    }

                    response.append("\n");
                    response.append("请输入指令:\n\n");
                    sendMessage(response.toString());
                    response.delete(0,response.length());
                    message=readMessage();
                }
                //////////////////////////////////循环结束,处理断连逻辑
                int num=saveUserData(u,flag);
                if(num>0)doBroadcast(broadcastSocket.getOutputStream(),"数据保存完成");
                else doBroadcast(broadcastSocket.getOutputStream(),"数据保存失败");
                buffOut.write(toByteArray("华盛顿等你回来，特工！".length()));
                pw.print("华盛顿等你回来，特工！");
                pw.flush();
                doBroadcast(broadcastSocket.getOutputStream(),"disconnect");
                logs.append("用户退出\n");
                logs.setSelectionEnd(logs.getText().length());
                informUserOffline(u);
            } catch (IOException e) {
                MyLogger.loge("Runserver","run");
                e.printStackTrace();
            }
            try {
                broadcastSocket.close();//关闭广播由线程进行，同时删除在线用户字典自己这一项
                pw.close();
                br.close();
                userS.close();
            } catch (IOException e) {
                MyLogger.loge("ServerRun"," close");
                e.printStackTrace();
            }
        }

        /**
         * 从Socket读取信息
         * @return 读到的信息
         */
        String readMessage(){
            int len=0;
            String message;
            byte[] num=new byte[4];
            char[] buff=new char[512];
            try {
                buffIn.read(num,0,4);
                len= toInt(num);
                br.read(buff,0,len);
            } catch (IOException e) {
                e.printStackTrace();
            }
            message=String.valueOf(buff).trim();
            return message;
        }

        /**
         * 回归行动基地方法
         */
        void GoBack(User user){
            informUserOffline(user);
            mainMap.enter(user);
            informUserOnline(user);
        }
        /**
         * 向客户端发送信息
         * @param message 信息字符串
         */
        void sendMessage(String message){
            try {
                buffOut.write(toByteArray(message.length()));
                pw.print(message);
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**
         * 储存玩家信息
         * @param u 用户对象
         * @return 返回更新的列数，判断是否成功
         */
        int  saveUserData(User u,boolean flag){
            Connection conn=null;
            int num=0;
            synchronized (dbcpManager) {
                conn=dbcpManager.getConn();
            }
            String sql;
            if(flag)
            sql="insert into userinfo (levelnum,experience,upgradeEX,blockRate,skills,username) values (?,?,?,?,?,?)";
            else sql="update userinfo set levelnum=?,experience=?,upgradeEX=?,blockRate=?,skills=? where username=?";
            try {

                PreparedStatement ps=conn.prepareStatement(sql);
                ps.setInt(1,u.level);
                ps.setInt(2,u.experience);
                ps.setInt(3,u.upgradeEx);
                ps.setFloat(4,u.blockRate);
                StringBuilder sb=new StringBuilder();
                Iterator<String> it=u.skillMap.keySet().iterator();
                if(it.hasNext()){
                    sb.append(it.next());
                    while(it.hasNext()){
                        sb.append(":");sb.append(it.next());
                    }
                }
                ps.setString(5,sb.toString());
                ps.setString(6,u.name);
                num=ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return num;
        }
    }
    /////////////////////////////////////////////////////////////////////////////工具函数组
    /**
     * 用户上线
     * @param user 用户
     */
    public void informUserOnline(BlankPerson user) {
        String ss=user.name+"进入区域\n";
        BlankRoom r=mainMap.getRoom(user.room);
        for(String s:r.personList.keySet()) {
            if(r.personList.get(s).type== CharacterType.特工)
                doBroadcast(s, ss);
        }
        r.personList.put(user.name,user);
    }
    /**
     * 用户下线
     * @param user 用户
     */
    public void informUserOffline(BlankPerson user) {
        String ss=user.name+"离开了区域\n";
        BlankRoom r=mainMap.getRoom(user.room);
        r.personList.remove(user.name);
        for(String s:r.personList.keySet()) {
            if(r.personList.get(s).type== CharacterType.特工)
                doBroadcast(s, ss);
        }
    }
    /**
     * 用户进入房间
     * @param user 用户
     */
    public void informUserIn(BlankPerson user, Diraction diraction) {
        BlankRoom r=mainMap.getRoom(user.room);
        String ss=user.name+"从"+diraction.name()+"方来到了"+r.name+"\n";
        for(String s:r.personList.keySet()) {
            if(r.personList.get(s).type== CharacterType.特工)
                doBroadcast(s, ss);
        }
        r.personList.put(user.name,user);
    }
    /**
     * 用户离开房间
     * @param user 用户
     */
    public void informUserOut(BlankPerson user, Diraction diraction) {
        BlankRoom r=mainMap.getRoom(user.room);
        String ss=user.name+"向"+diraction.name()+"方离开了"+r.name+"\n";
        r.personList.remove(user.name);
        for(String s:r.personList.keySet()) {
            if(r.personList.get(s).type== CharacterType.特工)
                doBroadcast(s, ss);
        }
    }
    /**
     * 广播方法，用于向用户发送广播，一个用户退出或进入时独占在线用户字典，获取在房间用户名
     * 获取输出流后发送广播
     * @param os 目标用户socket的广播输出流
     * @param message 要发送的消息
     */
    private void doBroadcast(OutputStream os,String message){
        try {
            os.write(toByteArray(message.length()));
            PrintWriter broadcastWriter=new PrintWriter(os);
            broadcastWriter.write(message);
            broadcastWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过字符串发送消息，用于通过单例操作从外部调用
     * @param username 要发送对象的用户名
     * @param message 要发送的消息
     * @return 返回发送状态，若查询不到该用户则失败
     */
    public boolean doBroadcast(String username,String message){
            OutputStream os = onlineDict.get(username);
            if(os!=null)
            {doBroadcast(os,message);return true;}
            else {System.out.println("dobroadcast  用户名查询失败，用户字典错误");return false;}
    }


    /**
     * 用于将4字节转为int
      * @param num 字节数组
     * @return int
     */
    private int toInt(byte[] num){
        int result=0;
        result=num[0] & 0xff|(num[1]&0xff)<<8|(num[2]&0xff)<<16|(num[3]&0xff)<<24;
        return result;
    }

    /**
     * 将int转为目标字节数组
     * @param num int型
     * @return 字节数组
     */
    private byte[] toByteArray(int num){
        byte[] n=new byte[4];
        for(int i=0;i<4;i++){
            n[i]=(byte)(num>>8*i&0xff);
        }
        return n;
    }
    ///////////////////////////////////////////////////main函数
    public static void main(String[] args){
        MainWindow mainWindow= new MainWindow();
    }
}
class MyLock extends Object{

}
