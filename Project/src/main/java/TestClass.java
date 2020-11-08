import java.util.HashSet;
import java.util.Set;

// 用于记录测试类
public class TestClass {
    static Set<String> testClasses = new HashSet<>();

    // 添加测试类
    public static void add(String className) {
        testClasses.add("L" + className);
    }

    // 判断测试类
    public static boolean isTestClass(String className) {
        return testClasses.contains(className);
    }
}
