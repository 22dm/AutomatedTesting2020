import com.ibm.wala.classLoader.ClassFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class TestSelection {
    public static void main(String[] args) {
        try {
            // 读取命令行参数
            // 模式 (-m 或 -c)
            // 添加了 -a 模式用于测试，会生成所有文件（包括 dot）
            String mode = args[0];
            // target 目录路径
            String path = args[1];
            // change_info.txt 文件路径
            String change = args[2];

            // exclusion.txt 文件的位置
            String exclusionPath = TestSelection.class.getResource("exclusion.txt").toString();
            // 生成分析域
            AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", new File(exclusionPath), TestSelection.class.getClassLoader());

            // 添加要分析的测试类文件
            addClass(scope, Paths.get(path, "test-classes").toString());
            // 记录所有测试类，后续的测试用例选择只会输出这些类的方法
            for (Module m : scope.getModules(ClassLoaderReference.Application)) {
                if (m instanceof ClassFileModule) {
                    TestClass.add(((ClassFileModule) m).getClassName());
                }
            }

            // 添加要分析的其他类文件
            addClass(scope, Paths.get(path, "classes").toString());

            // 生成类层次
            ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);

            // 构建调用图
            CHACallGraph graph = new CHACallGraph(cha);
            graph.init(new AllApplicationEntrypoints(scope, cha));

            // 类、方法级别的 Dot 文件
            MyDot classDot = new MyDot("class");
            MyDot methodDot = new MyDot("method");

            // 调用图
            MyCallGraph methodCallGraph = new MyCallGraph();

            // 遍历调用图
            for (CGNode node : graph) {
                // 过滤掉不感兴趣的节点
                if (notInterestingNode(node)) {
                    continue;
                }

                // 记录类名、方法签名
                String className = node.getMethod().getDeclaringClass().getName().toString();
                String methodSignature = node.getMethod().getSignature();
                String methodName = className + " " + methodSignature;

                // 遍历调用此方法的方法
                for (CGNode predNode : Iterator2Iterable.make(graph.getPredNodes(node))) {
                    // 过滤掉不感兴趣的节点
                    if (notInterestingNode(predNode)) {
                        continue;
                    }

                    // 记录类名、方法签名
                    String predClassName = predNode.getMethod().getDeclaringClass().getName().toString();
                    String predMethodSignature = predNode.getMethod().getSignature();
                    String predMethodName = predClassName + " " + predMethodSignature;

                    if (Objects.equals(mode, "-a")) {
                        // 记录 dot 文件
                        classDot.append(className, predClassName);
                        methodDot.append(methodSignature, predMethodSignature);
                    }

                    // 记录方法调用关系
                    methodCallGraph.ensureEdge(methodName, predMethodName);
                }
            }

            if (Objects.equals(mode, "-a")) {
                // 写入 dot 文件
                classDot.save("class-cfa.dot");
                methodDot.save("method-cfa.dot");
            }

            // 受影响的方法
            Set<String> changed = new HashSet<>();

            // 按行读取文件
            FileInputStream inputStream = new FileInputStream(change);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                // 记录受影响的方法
                changed.add(str);
            }
            bufferedReader.close();
            inputStream.close();

            // 判断输出模式
            if (Objects.equals(mode, "-m") || Objects.equals(mode, "-a")) {
                // 输出方法级测试选择
                Utils.saveFile("selection-method.txt", String.join("\n", methodCallGraph.getTest(changed, MyCallGraph.MethodMode)));
            }

            if (Objects.equals(mode, "-c") || Objects.equals(mode, "-a")) {
                // 输出类级测试选择
                Utils.saveFile("selection-class.txt", String.join("\n", methodCallGraph.getTest(changed, MyCallGraph.ClassMode)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 添加要分析的 class 文件
    static void addClass(AnalysisScope scope, String path) throws InvalidClassFileException {
        File[] classes = new File(path).listFiles();
        if (classes != null) {
            for (File file : classes) {
                String filePath = file.getAbsolutePath();

                // 递归遍历
                if (file.isDirectory()) {
                    addClass(scope, filePath);
                } else if (file.getName().endsWith(".class")) {
                    scope.addClassFileToScope(ClassLoaderReference.Application, new File(filePath));
                }
            }
        }
    }

    // 判断是否为感兴趣的节点
    static boolean notInterestingNode(CGNode node) {
        return !(node.getMethod() instanceof ShrikeBTMethod) || !node.getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }
}
