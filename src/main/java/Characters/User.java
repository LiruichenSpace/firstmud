package Characters;

import GameMaps.BlankRoom;
import GameMaps.GameMap;
import Items.Item;
import MudMonitor.MainWindow;
import Skills.SkillFactory;
import com.alibaba.fastjson.JSONObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User extends BlankPerson {
    /**
     * 当前经验
     */
    public int experience;
    /**
     * 升级所需
     */
    public int upgradeEx;
    public GameMap privateMap;
    public User(String name){
        this.name=name;
        type=CharacterType.特工;
        experience=0;
        upgradeEx=3;
        damageNum=30;
        privateMap=null;
        room="";
    }

    /**
     * 保存的数据：技能列表，等级，经验状态
     *
     * 设定各类数值，如果登陆时数据库有该人物则用数据库初始化，否则创建缺省并插入新项
     * @param rs 通过查询结果判断，如果有结果则用其初始化，内部不调用next
     */
    public void initiate(ResultSet rs){//尚未初始化护甲和手雷
        try {
            level=rs.getInt("levelnum");
            setNum();
            currArmor=fullArmor;
            experience=rs.getInt("experience");
            upgradeEx=rs.getInt("upgradeEX");
            blockRate=rs.getFloat("blockRate");
            String skills=rs.getString("skills");
            String[] skillList=skills.split(":");
            for(String s:skillList){
                if(!s.equals(""))
                    skillMap.put(s, SkillFactory.getSkillInstance(s));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重设数据
     */
    public void setNum(){
        fullHP=200+25*level;
        currHP=fullHP;
        blockRate=0.5f;
        fullArmor=80+20*level;
    }

    /**
     * 更新冷却信息
     */
    void coolDown(){

    }
    public String showStatus(){
        StringBuilder sb=new StringBuilder();
        sb.append(super.showStatus());
        sb.append("经验值状态:");sb.append(showPercentBar(experience,upgradeEx));
        sb.append("  ");sb.append(experience);sb.append("/");sb.append(upgradeEx);
        sb.append("\n");
        return sb.toString();
    }
    /**
     * 重置各类数据，副本完成后进行
     */
    void reset(){
        currHP=fullHP;
        currArmor=fullArmor;

    }
    /**
     * 检测升级，满足条件进行升级
     */
    private void testUpgrade(){
        if(experience>=upgradeEx){
            if(level<10) {
                level++;
                setNum();
                experience=experience-upgradeEx;
                upgradeEx*=2;
            }
            else {
                experience=upgradeEx;
            }
        }
    }

    /**
     * 射击敌人，击杀时判断升级，升级回复血量但不补满护甲
     * 最高等级10级
     * 获得的经验为等级数
     * @param target 攻击目标
     */
    public boolean shoot(BlankPerson target){
        boolean flag=super.shoot(target);
        if(flag) {
            testUpgrade();
            experience+=target.level;
        }
        return flag;
    }
}
