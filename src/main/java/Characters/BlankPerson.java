package Characters;

import Skills.AbstractSkill;

import java.util.HashMap;
import java.util.Map;

public class BlankPerson  {
    public float fullHP;
    public float currHP;
    public float blockRate;
    public float fullArmor;
    public float currArmor;
    public int level;
    /**
     * 决定类型
     */
    public CharacterType type;
    public float damageNum;
    public String name;
    public String room;
    public Map<String, AbstractSkill> skillMap;
    /**
     * 缺省构造,默认armor为50，血量100，每升一级血量加20，护甲加10
     * 默认坏人,技能数不超过等级
     */
    public BlankPerson(){
        level=1;
        type=CharacterType.敌人;
        setNum();
        damageNum=10+2*level;
        currArmor=fullArmor;
        skillMap=new HashMap<String, AbstractSkill>();
    }
    /**
     * 含参构造,根据等级设置属性
     * 默认坏人,技能数不超过等级
     */
    public BlankPerson(int level){
        this.level=level;
        type=CharacterType.敌人;
        setNum();
        damageNum=10+2*level;
        currArmor=fullArmor;
        skillMap=new HashMap<String, AbstractSkill>();
    }

    /**
     * 用于基于等级重设数值
     */
    public void setNum(){
        fullHP=100+20*level;
        currHP=fullHP;
        blockRate=0.8f;
        fullArmor=50+10*level;
    }
    /**
     * 用户使用时先收集完成信息再调用
     * @param skillNum 技能编号
     * @param target 目标
     */
    public void useSkill(int skillNum,BlankPerson target) {

    }

    /**
     * 普通射击伤害解释：有护甲会阻挡攻击伤害
     * 普通射击会进行伤害阻挡计算后先对护甲造成伤害，再对血量造成伤害
     * 是否调用射击需根据房间类型判断，交由外部决定
     * @param target 射击目标
     * @return 返回目标是否死亡
     */
    public boolean shoot(BlankPerson target) {
        float damage=damageNum;
        if(target.currArmor!=0) {
            damage=target.damageBlock(damageNum);
            target.currArmor-=damage;
            if(target.currArmor<0){
                target.currHP=target.currHP+target.currArmor;
                target.currArmor=0;
            }
        }
        else target.currHP-=damageNum;
        if(target.currHP<=0){
            target.currHP=0;
            return true;
        }
        else return false;
    }
    public String showStatus(){
        StringBuilder sb=new StringBuilder();
        sb.append(name);sb.append("    等级:");sb.append(level);
        sb.append("    身份:");sb.append(type.name());
        sb.append("\n");
        sb.append("护甲状态:  ");sb.append(showPercentBar(currArmor,fullArmor));
        sb.append("  ");sb.append(currArmor);sb.append("/");sb.append(fullArmor);
        sb.append("\n");
        sb.append("生命值状态:");sb.append(showPercentBar(currHP,fullHP));
        sb.append("  ");sb.append(currHP);sb.append("/");sb.append(fullHP);
        sb.append("\n");
        return sb.toString();
    }
    public float getFullHP() {
        return fullHP;
    }
    public float getCurrHP() {
        return currHP;
    }
    public String showPercentBar(float curr,float full){
        StringBuilder sb=new StringBuilder(31);
        int count= (int) ((curr/full)*30);
        sb.append("|");
        for(int i=0;i<count;i++)sb.append("|");
        for (int i = 0; i < 30-count; i++) sb.append(" ");
        sb.append("|");
        return sb.toString();
    }
    public float damageBlock(float damage){
        return damage*blockRate;
    }
}
