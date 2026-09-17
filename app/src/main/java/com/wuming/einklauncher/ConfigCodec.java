package com.wuming.einklauncher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 持久化序列化工具：自定义顺序与自定义名称的编解码。
 * 纯静态、不依赖 Android 运行环境，便于单元测试覆盖。
 */
final class ConfigCodec {

  /** 自定义顺序的分隔符（包名不含换行符，可安全用作分隔符） */
  private static final String ORDER_SEPARATOR = "\n";

  /** 旧版自定义名称格式：条目按换行分隔，条目内以控制字符分隔包名与名称 */
  private static final String LEGACY_LABEL_ENTRY_SEPARATOR = "\n";
  private static final String LEGACY_LABEL_KV_SEPARATOR = "\u0001";

  private ConfigCodec() {
  }

  // ---- 自定义顺序 ----

  static List<String> decodeOrder(String raw) {
    List<String> order = new ArrayList<>();
    if (raw == null || raw.isEmpty()) {
      return order;
    }
    for (String pkg : raw.split(ORDER_SEPARATOR)) {
      if (!pkg.isEmpty()) {
        order.add(pkg);
      }
    }
    return order;
  }

  static String encodeOrder(List<String> order) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < order.size(); i++) {
      if (i > 0) {
        sb.append(ORDER_SEPARATOR);
      }
      sb.append(order.get(i));
    }
    return sb.toString();
  }

  // ---- 自定义名称 ----

  /**
   * 解码自定义名称映射（包名 → 名称）。
   * 当前格式为 JSON 对象；旧格式（换行分条、控制字符分键值）仅在读取时兼容迁移，
   * 写回时统一落为新格式，避免旧转义方案被特殊字符破坏。
   */
  static Map<String, String> decodeLabels(String raw) {
    Map<String, String> labels = new HashMap<>();
    if (raw == null || raw.isEmpty()) {
      return labels;
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("{")) {
      try {
        JSONObject json = new JSONObject(trimmed);
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
          String pkg = keys.next();
          String label = json.optString(pkg, "");
          if (!label.isEmpty()) {
            labels.put(pkg, label);
          }
        }
      } catch (JSONException ignored) {
        // 损坏数据按空处理，避免崩溃
      }
      return labels;
    }
    // 旧格式迁移（旧版写入时已将名称中的换行替换为空格，此处按行解析安全）
    for (String entry : trimmed.split(LEGACY_LABEL_ENTRY_SEPARATOR)) {
      int sep = entry.indexOf(LEGACY_LABEL_KV_SEPARATOR);
      if (sep > 0) {
        labels.put(entry.substring(0, sep), entry.substring(sep + 1));
      }
    }
    return labels;
  }

  static String encodeLabels(Map<String, String> labels) {
    JSONObject json = new JSONObject();
    for (Map.Entry<String, String> entry : labels.entrySet()) {
      try {
        json.put(entry.getKey(), entry.getValue());
      } catch (JSONException ignored) {
        // 键为 null 等非法情形直接跳过
      }
    }
    return json.toString();
  }
}
