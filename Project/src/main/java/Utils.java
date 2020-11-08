import java.io.FileWriter;
import java.io.IOException;

public class Utils {
    // 保存文件
    public static void saveFile(String path, String data) throws IOException {
        FileWriter writer = new FileWriter(path);
        writer.write(data);
        writer.flush();
        writer.close();
    }
}
