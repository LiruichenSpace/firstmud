package Commands;

/**
 * 其他的没什么好说的,就地图.....我要是支持地图真是能累死我,这是真的精细活,告辞
 */
public enum FirstCommand {
    移动,攻击,查看,技能,退出,逃跑,地图,群发,私聊;

    static public boolean have(String s){
        boolean flag=false;
        for(FirstCommand f:FirstCommand.values()){
            if(f.name().equals(s))flag=true;
        }
        return flag;
    }
}
