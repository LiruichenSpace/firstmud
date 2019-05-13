package Skills;

public class SkillFactory {
    public static Skill getSkillInstance(String name){
        Skill s=new Skill();
        return s;
    }
}
