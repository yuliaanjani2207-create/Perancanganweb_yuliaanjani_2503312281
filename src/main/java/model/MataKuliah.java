package model;

/** Entitas mata kuliah (tabel mata_kuliah). */
public class MataKuliah {
    private int id;
    private String kode;
    private String nama;
    private int sks;
    private int dosenId;
    private String namaDosen; // hasil JOIN untuk tampilan tabel

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public int getSks() { return sks; }
    public void setSks(int sks) { this.sks = sks; }
    public int getDosenId() { return dosenId; }
    public void setDosenId(int dosenId) { this.dosenId = dosenId; }
    public String getNamaDosen() { return namaDosen; }
    public void setNamaDosen(String namaDosen) { this.namaDosen = namaDosen; }
}
