import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// 自己实现的调用图，既可以储存方法，也可以储存类
// 过滤了原本 CallGraph 的顶点，极大地加快了找边的速度
public class MyCallGraph {
    // key 为方法/类名
    // value 为调用了此方法/类的方法/类
    public Map<String, Set<String>> graph = new HashMap<>();

    // 两种模式，储存类调用图或者方法调用图
    public int type;
    static final int ClassGraph = 0;
    static final int MethodGraph = 1;

    MyCallGraph(int type) {
        this.type = type;
    }

    // 确保节点在图中，不存在则创建
    public void ensureNode(String name) {
        if (!graph.containsKey(name)) {
            graph.put(name, new HashSet<>());
        }
    }

    // 确保边（调用关系）在图中，不存在则创建
    // nameB 调用了 nameA
    public void ensureEdge(String nameA, String nameB) {
        ensureNode(nameA);
        ensureNode(nameB);
        // 因为是 Set，重复 add 也没关系
        graph.get(nameA).add(nameB);
    }

    // 选择测试用例
    // 输入修改的方法/类，输出被选择的测试方法
    public Set<String> getTest(Set<String> changed) {
        // 储存所有直接或间接调用了修改方法/类的方法/类，使用迭代算法
        // 被修改的方法默认添加到集合中
        Set<String> callers = new HashSet<>(changed);

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
        Set<String> selected = new HashSet<>();

        if (type == ClassGraph) {
            for (String testMethod : TestMethod.testMethods) {
                // 如果测试方法所在的类被选中
                if (callers.contains(testMethod.split(" ")[0])) {
                    selected.add(testMethod);
                }
            }
        } else if (type == MethodGraph) {
            for (String testMethod : TestMethod.testMethods) {
                // 如果测试方法被选中
                if (callers.contains(testMethod)) {
                    selected.add(testMethod);
                }
            }
        }

        return selected;
    }
}
