package GameMaps;

import Characters.BlankPerson;
import Characters.User;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.sql.DataSource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 游戏主要地图，通过JSON文件读写初始化，地图状态固定，是否进行roguelike待测
 */
public class GameMap {
    String entrance;
    String mapName;
    public GameMap(String mapname){
        rooms= new HashMap<String, BlankRoom>();
        mapName=mapname;
        loadGameMap(mapName+".txt");
    }

    /**
     * 链接数据库初始化整个游戏地图
     * 没想好怎么处理副本问题,和怎么处理玩家暴毙问题
     * 一方面玩家只可能被别人杀死,另一方面这边只能等待命令
     * 可能方向:死亡信息和输入提示通过广播通知,再输错跟系统无关OrZ
     */
    private void loadGameMap(String filename){
        File  f=new File("Map\\"+filename);
        JSONObject jobj=null;
        BufferedReader fr=null;
        BlankRoom br=new BlankRoom();
        Map<Diraction,String> s=new HashMap<Diraction, String>();
        try {
            fr=new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(fr!=null){
            try {
                String json = fr.readLine();
                do{
                    jobj= JSON.parseObject(json);
                    s.put(Diraction.南, jobj.getString("s"));
                    s.put(Diraction.下, jobj.getString("d"));
                    s.put(Diraction.上, jobj.getString("u"));
                    s.put(Diraction.北, jobj.getString("n"));
                    s.put(Diraction.东, jobj.getString("e"));
                    s.put(Diraction.西, jobj.getString("w"));
                    s.put(Diraction.西北, jobj.getString("nw"));
                    s.put(Diraction.西南, jobj.getString("sw"));
                    s.put(Diraction.东北, jobj.getString("ne"));
                    s.put(Diraction.东南, jobj.getString("se"));
                    br.setSurround(s);
                    s = new HashMap<Diraction, String>();
                    br.setName(jobj.getString("name").toString());
                    br.type = RoomType.valueOf(jobj.getString("type"));
                    br.description = jobj.getString("description");
                    if(br.type.equals(RoomType.入口))entrance=br.name;
                    rooms.put(br.name, br);
                    br=new BlankRoom();
                    json=fr.readLine();
                }while(json!=null);
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存所有的房间，通过房间名查找指定房间
     */
    private Map<String, BlankRoom> rooms;
    public void enter(User u){
        u.room=entrance;
    }
    /**
     * 一个只读的地图文件为什么还要支持保存呢？巧了，我也在奇怪这一点
     */
    public void saveMap(){
        JSONObject jobj=new JSONObject();
        File out=new File("Map\\"+mapName);
        PrintWriter pr=null;
        if(!out.exists()) {
            try {
                out.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            pr=new PrintWriter(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(BlankRoom r:rooms.values()){
            jobj.put("name",r.name);
            jobj.put("type",r.type.name());
            jobj.put("description",r.description);
            jobj.put("s",r.surround.get(Diraction.南));
            jobj.put("w",r.surround.get(Diraction.西));
            jobj.put("n",r.surround.get(Diraction.北));
            jobj.put("e",r.surround.get(Diraction.东));
            jobj.put("ne",r.surround.get(Diraction.东北));
            jobj.put("se",r.surround.get(Diraction.东南));
            jobj.put("sw",r.surround.get(Diraction.西南));
            jobj.put("nw",r.surround.get(Diraction.西北));
            jobj.put("u",r.surround.get(Diraction.上));
            jobj.put("d",r.surround.get(Diraction.下));
            if(pr!=null) {
                pr.println(jobj.toString());
                pr.flush();
                System.out.println("ok");
            }

            jobj.clear();
        }
    }
    public Set<String> getRoomNames(){
        return rooms.keySet();
    }
    public BlankRoom getRoom(String name){
        if(rooms.containsKey(name))
            return rooms.get(name);
        else return null;
    }
}
