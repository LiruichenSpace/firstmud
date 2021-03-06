package Commands;

/**
 * 用于switch查看功能的查看二级命令
 * 内容解释：1.我：查看自身详细信息;2.房间：查看房间信息（描述和角色，包含血量）
 * 3.角色:查看角色的详细信息;4.方向:查看该房间该方向上的房间
 */
public enum LookType {
    我,房间,角色,方向,在线玩家;
    static public boolean have(String s){
        boolean flag=false;
        for(LookType f:LookType.values()){
            if(f.name().equals(s))flag=true;
        }
        return flag;
    }
}
