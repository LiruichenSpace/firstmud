package GameMaps;

/**
 * 方向，通过reverse方法可以输出相反的方向
 */
public enum Diraction {
    东,下,南,东南,西南,东北,西北,北,上,西;

    /**
     * 输出反方向
     * @param diraction 输入一个方向
     * @return 输出相反的方向
     */
    static public Diraction reverse(Diraction diraction){
        return Diraction.values()[Diraction.values().length-diraction.ordinal()-1];

    }
}
