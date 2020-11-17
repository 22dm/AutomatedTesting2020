import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

// 用于生成 dot 文件
public class MyDot {
    String name;
    Set<String> lines = new HashSet<>();

    public MyDot(String name) {
        this.name = name;
    }

    // 添加一条边
    public void append(String source, String target) {
        lines.add("\t\"" + source + "\" -> \"" + target + "\";");
    }

    // 保存
    public void save(String path) throws IOException {
        Utils.saveFile(path, "digraph " + name + " {\n" + String.join("\n", lines) + "\n}\n");
    }
}
