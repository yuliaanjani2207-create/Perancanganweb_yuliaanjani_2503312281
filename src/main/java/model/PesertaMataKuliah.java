package model;

/** Relasi mahasiswa - mata kuliah (tabel peserta_mata_kuliah). */
public class PesertaMataKuliah {
    private int id;
    private int mahasiswaId;
    private int mataKuliahId;
    private String nim;   // hasil JOIN
    private String namaMahasiswa; // hasil JOIN
    private String kodeMatkul;    // hasil JOIN
    private String namaMatkul;    // hasil JOIN

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMahasiswaId() { return mahasiswaId; }
    public void setMahasiswaId(int mahasiswaId) { this.mahasiswaId = mahasiswaId; }
    public int getMataKuliahId() { return mataKuliahId; }
    public void setMataKuliahId(int mataKuliahId) { this.mataKuliahId = mataKuliahId; }
    public String getNim() { return nim; }
    public void setNim(String nim) { this.nim = nim; }
    public String getNamaMahasiswa() { return namaMahasiswa; }
    public void setNamaMahasiswa(String namaMahasiswa) { this.namaMahasiswa = namaMahasiswa; }
    public String getKodeMatkul() { return kodeMatkul; }
    public void setKodeMatkul(String kodeMatkul) { this.kodeMatkul = kodeMatkul; }
    public String getNamaMatkul() { return namaMatkul; }
    public void setNamaMatkul(String namaMatkul) { this.namaMatkul = namaMatkul; }
}
