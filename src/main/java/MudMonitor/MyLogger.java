package MudMonitor;

public class MyLogger {
    static public void loge(String ClassName, String Method) {
        loge(ClassName, Method,"");
    }

    static public void loge(String ClassName, String Method, String Type){
        System.out.println("Log(Error): at"+ClassName+": at"+Method+": "+Type);
    }
    static public void logm(String ClassName, String Method) {
        loge(ClassName, Method,"");
    }

    static public void logm(String ClassName, String Method, String Type){
        System.out.println("Log(Message): at"+ClassName+": at"+Method+": "+Type);
    }
}
