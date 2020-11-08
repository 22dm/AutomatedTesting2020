import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// 自己实现的调用图
// 过滤了原本 CallGraph 的顶点，极大地加快了找边的速度
public class MyCallGraph {
    // key 为方法名
    // value 为调用了此方法的方法
    public Map<String, Set<String>> graph = new HashMap<>();

    // 两种选择测试方法的模式
    static final int ClassMode = 0;
    static final int MethodMode = 1;

    // 确保节点（方法）在图中，不存在则创建
    public void ensureNode(String methodName) {
        if (!graph.containsKey(methodName)) {
            graph.put(methodName, new HashSet<>());
        }
    }

    // 确保边（调用关系）在图中，不存在则创建
    // methodNameB 调用了 methodNameA
    public void ensureEdge(String methodNameA, String methodNameB) {
        ensureNode(methodNameA);
        ensureNode(methodNameB);
        // 因为是 Set，重复 add 也没关系
        graph.get(methodNameA).add(methodNameB);
    }

    // 选择测试用例
    // 输入修改的方法与选择模式，输出测试方法
    public Set<String> getTest(Set<String> changed, int mode) {
        // 储存所有直接或间接调用了修改方法的方法，使用迭代算法
        Set<String> callers = new HashSet<>();
        if (mode == MethodMode) {
            // 被修改的方法默认添加到集合中
            callers.addAll(changed);
        } else if (mode == ClassMode) {
            // 获得所有被修改的类
            Set<String> changedClass = new HashSet<>();
            for (String methodName : changed) {
                changedClass.add(methodName.split(" ")[0]);
            }

            // 被修改的类中所有方法默认添加到集合中
            for (String methodName : graph.keySet()) {
                if (changedClass.contains(methodName.split(" ")[0])) {
                    callers.add(methodName);
                }
            }
        }

        // 等待集合稳定
        while (true) {
            // 新的集合
            Set<String> newCallers = new HashSet<>(callers);
            // 遍历旧集合的节点，将调用了此方法的方法加入新集合
            for (String caller : callers) {
                newCallers.addAll(graph.get(caller));
            }

            // 如果新旧集合节点数量一致，则已经找全
            if (newCallers.size() == callers.size()) {
                break;
            }

            callers = newCallers;
        }

        // 只输出测试类的方法
        Set<String> testPreds = new HashSet<>();
        for (String caller : callers) {
            if (TestClass.isTestClass(caller.split(" ")[0]) && !caller.contains("<init>")) {
                testPreds.add(caller);
            }
        }

        return testPreds;
    }
}
