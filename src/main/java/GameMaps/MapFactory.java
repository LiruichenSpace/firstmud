package GameMaps;

import Characters.BlankPerson;
import Characters.User;

/**
 * 当前预期功能：为每个玩家单独制造一个副本，只能单人
 */
public class MapFactory {
    BlankRoom getBossRoom(BlankRoom room,BlankPerson boss){
        return  null;
    }
    BlankRoom getRoomWithEnimy(BlankRoom room,int num){
        return  null;
    }

    /**
     * 为用户生成一个副本地图（突然想到文件读取可能会爆炸....大概还是复制比较好吧）
     * @param user
     * @return
     */
    static public GameMap creatPrivateMap(User user){
        GameMap map=new GameMap(user.room);
        BlankRoom temp=null;
        for(String s:map.getRoomNames()) {
            temp=map.getRoom(s);
            if(temp.type==RoomType.副本房间){

            }else if(temp.type==RoomType.BOSS){

            }
        }
        return map;
    }
}
