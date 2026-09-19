package all_class;

import java.util.ArrayList;
import java.util.List;

// 统一处理分隔符，避免密码或商品名中含有 | 时读错字段。
final class TextCodec {
    //这个类只提供工具方法，不需要创建对象
    private TextCodec() {}

    static String join(Object... fields) {
        StringBuilder out = new StringBuilder("@TXT2");
        for (Object field : fields) {
            // 先处理反斜杠，再处理其他字符，顺序不能反过来。
            String value = String.valueOf(field);
            value = value.replace("\\", "\\\\").replace("|", "\\|")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            out.append('|').append(value);
        }
        return out.toString();
    }

    static String[] split(String line) {
        // 没有新版前缀的旧数据，仍按原来的方法读取。
        if (!line.startsWith("@TXT2|")) return line.split("\\|", -1);
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean escape = false;
        for (char c : line.substring(6).toCharArray()) {
            if (escape) {
                // 前一个字符是反斜杠，把转义字符还原。
                if (c == 'n') field.append('\n');
                else if (c == 'r') field.append('\r');
                else if (c == 't') field.append('\t');
                else if (c == '\\' || c == '|') field.append(c);
                else throw new IllegalArgumentException("无效的 TXT 转义");
                escape = false;
            } else if (c == '\\') escape = true;
            else if (c == '|') {
                // 只有未被转义的 | 才是字段分隔符。
                fields.add(field.toString());
                field.setLength(0);
            } else field.append(c);
        }
        if (escape) throw new IllegalArgumentException("不完整的 TXT 转义");
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
