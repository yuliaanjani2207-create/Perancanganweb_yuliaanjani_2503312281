package model;

/** Pasangan key-value pengaturan aplikasi (tabel app_setting). */
public class AppSetting {
    private String key;
    private String value;

    public AppSetting() {}
    public AppSetting(String key, String value) { this.key = key; this.value = value; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
