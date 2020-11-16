import java.util.HashSet;
import java.util.Set;

// 用于记录测试方法
public class TestMethod {
    static public Set<String> testMethods = new HashSet<>();

    // 添加测方法
    public static void add(String name) {
        testMethods.add(name);
    }

    // 判断测方法
    public static boolean isTestMethod(String name) {
        return testMethods.contains(name);
    }
}
