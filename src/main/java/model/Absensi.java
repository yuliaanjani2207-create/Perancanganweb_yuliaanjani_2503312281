package model;

/** Catatan kehadiran mahasiswa per sesi (tabel absensi). */
public class Absensi {
    private int id;
    private int mahasiswaId;
    private int sesiId;
    private String waktuAbsen; // timestamp ISO
    private String status = "HADIR"; // HADIR | TERLAMBAT
    private String metode = "QR";    // QR | MANUAL
    // hasil JOIN
    private String nim;
    private String namaMahasiswa;
    private String kodeMatkul;
    private String namaMatkul;
    private Integer pertemuanKe;
    private String tanggal;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMahasiswaId() { return mahasiswaId; }
    public void setMahasiswaId(int mahasiswaId) { this.mahasiswaId = mahasiswaId; }
    public int getSesiId() { return sesiId; }
    public void setSesiId(int sesiId) { this.sesiId = sesiId; }
    public String getWaktuAbsen() { return waktuAbsen; }
    public void setWaktuAbsen(String waktuAbsen) { this.waktuAbsen = waktuAbsen; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMetode() { return metode; }
    public void setMetode(String metode) { this.metode = metode; }
    public String getNim() { return nim; }
    public void setNim(String nim) { this.nim = nim; }
    public String getNamaMahasiswa() { return namaMahasiswa; }
    public void setNamaMahasiswa(String namaMahasiswa) { this.namaMahasiswa = namaMahasiswa; }
    public String getKodeMatkul() { return kodeMatkul; }
    public void setKodeMatkul(String kodeMatkul) { this.kodeMatkul = kodeMatkul; }
    public String getNamaMatkul() { return namaMatkul; }
    public void setNamaMatkul(String namaMatkul) { this.namaMatkul = namaMatkul; }
    public Integer getPertemuanKe() { return pertemuanKe; }
    public void setPertemuanKe(Integer pertemuanKe) { this.pertemuanKe = pertemuanKe; }
    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }
}
