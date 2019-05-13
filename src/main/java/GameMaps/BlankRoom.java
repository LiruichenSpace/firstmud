package GameMaps;

import Characters.BlankPerson;
import Characters.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Room类的空白抽象类
 */
public class BlankRoom {
    public HashMap<String,BlankPerson> personList;//所有人物
    public Map<Diraction,String> surround;
    RoomType type;
    public String name;
    public String description;
    public BlankRoom(){
        personList= new HashMap<String, BlankPerson>();
        type=null;
        surround=null;
    }
    public BlankRoom(String name, RoomType type, Map<Diraction,String> surround){
        this.surround=surround;
        this.type=type;
        this.name=name;
        personList= new HashMap<String, BlankPerson>();
    }
    public void setSurround(Map<Diraction, String> surround){this.surround=surround;}
    public void setType(RoomType type){ this.type=type;}
    public void setName(String name){this.name=name;}
    public void setDescription(String description){this.description=description;}

    /**
     * 返回房间状态字符串，用于用户查看
     * @return 房间状态字符串
     */
    public String Look() {
        StringBuilder sb=new StringBuilder();
        sb.append(description+"\n");
        sb.append(name+"内人物：\n");
        for(String s:personList.keySet()){
            sb.append(personList.get(s).toString());
        }
        return sb.toString();
    }
}
