package com.wuming.einklauncher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ConfigCodec} 序列化编解码的单元测试。
 */
public class ConfigCodecTest {

  // ---- 自定义顺序 ----

  @Test
  public void order_roundTrip() {
    List<String> order = Arrays.asList("com.a.b", "com.c.d", "com.e.f");
    assertEquals(order, ConfigCodec.decodeOrder(ConfigCodec.encodeOrder(order)));
  }

  @Test
  public void order_decodeEmptyAndNull() {
    assertTrue(ConfigCodec.decodeOrder(null).isEmpty());
    assertTrue(ConfigCodec.decodeOrder("").isEmpty());
    assertTrue(ConfigCodec.decodeOrder("\n\n").isEmpty());
  }

  @Test
  public void order_encodeEmpty() {
    assertEquals("", ConfigCodec.encodeOrder(new ArrayList<String>()));
  }

  // ---- 自定义名称：当前 JSON 格式 ----

  @Test
  public void labels_roundTrip() {
    Map<String, String> labels = new HashMap<>();
    labels.put("com.a.b", "阅读器");
    labels.put("com.c.d", "Line1\nLine2 \u0001 special");
    labels.put("com.e.f", "  spaced  ");

    Map<String, String> decoded = ConfigCodec.decodeLabels(ConfigCodec.encodeLabels(labels));
    assertEquals(3, decoded.size());
    assertEquals("阅读器", decoded.get("com.a.b"));
    // JSON 格式下换行、控制字符等特殊字符应原样保留
    assertEquals("Line1\nLine2 \u0001 special", decoded.get("com.c.d"));
    assertEquals("  spaced  ", decoded.get("com.e.f"));
  }

  @Test
  public void labels_decodeEmptyAndNull() {
    assertTrue(ConfigCodec.decodeLabels(null).isEmpty());
    assertTrue(ConfigCodec.decodeLabels("").isEmpty());
  }

  @Test
  public void labels_decodeCorruptJson() {
    // 以 "{" 开头但非法的串：按损坏数据处理，不抛异常
    assertTrue(ConfigCodec.decodeLabels("{not json").isEmpty());
  }

  @Test
  public void labels_encodedIsValidJsonObject() throws JSONException {
    Map<String, String> labels = new HashMap<>();
    labels.put("com.a.b", "名称");
    JSONObject json = new JSONObject(ConfigCodec.encodeLabels(labels));
    assertEquals("名称", json.optString("com.a.b"));
  }

  // ---- 自定义名称：旧格式迁移 ----

  @Test
  public void labels_decodeLegacyFormat() {
    Map<String, String> decoded = ConfigCodec.decodeLabels("com.a.b\u0001便签\ncom.c.d\u0001Clock");
    assertEquals(2, decoded.size());
    assertEquals("便签", decoded.get("com.a.b"));
    assertEquals("Clock", decoded.get("com.c.d"));
  }

  @Test
  public void labels_legacyRoundTripThroughNewFormat() {
    // 旧格式读入 → 新格式写出 → 再读入，值不变
    Map<String, String> decoded = ConfigCodec.decodeLabels("com.a.b\u0001笔记\ncom.c.d\u0001Test");
    Map<String, String> recoded = ConfigCodec.decodeLabels(ConfigCodec.encodeLabels(decoded));
    assertEquals("笔记", recoded.get("com.a.b"));
    assertEquals("Test", recoded.get("com.c.d"));
  }

  @Test
  public void labels_decodeLegacySkipsMalformedEntries() {
    // 缺少键值分隔符的条目应被跳过，不影响其余条目
    Map<String, String> decoded = ConfigCodec.decodeLabels("com.a.b\u0001正常\nbroken");
    assertEquals(1, decoded.size());
    assertEquals("正常", decoded.get("com.a.b"));
  }
}
