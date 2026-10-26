package model;

/** Sesi pertemuan perkuliahan (tabel sesi_perkuliahan). */
public class SesiPerkuliahan {
    private int id;
    private int mataKuliahId;
    private int pertemuanKe;
    private String tanggal;    // yyyy-MM-dd
    private String jamMulai;   // HH:mm
    private String jamSelesai; // HH:mm
    private String status = "TERJADWAL"; // TERJADWAL | AKTIF | SELESAI
    private String topik;
    // kolom hasil JOIN untuk kenyamanan tampilan
    private String kodeMatkul;
    private String namaMatkul;
    private String namaDosen;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMataKuliahId() { return mataKuliahId; }
    public void setMataKuliahId(int mataKuliahId) { this.mataKuliahId = mataKuliahId; }
    public int getPertemuanKe() { return pertemuanKe; }
    public void setPertemuanKe(int pertemuanKe) { this.pertemuanKe = pertemuanKe; }
    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }
    public String getJamMulai() { return jamMulai; }
    public void setJamMulai(String jamMulai) { this.jamMulai = jamMulai; }
    public String getJamSelesai() { return jamSelesai; }
    public void setJamSelesai(String jamSelesai) { this.jamSelesai = jamSelesai; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTopik() { return topik; }
    public void setTopik(String topik) { this.topik = topik; }
    public String getKodeMatkul() { return kodeMatkul; }
    public void setKodeMatkul(String kodeMatkul) { this.kodeMatkul = kodeMatkul; }
    public String getNamaMatkul() { return namaMatkul; }
    public void setNamaMatkul(String namaMatkul) { this.namaMatkul = namaMatkul; }
    public String getNamaDosen() { return namaDosen; }
    public void setNamaDosen(String namaDosen) { this.namaDosen = namaDosen; }

    public String getLabelSingkat() {
        return "P" + pertemuanKe + " - " + (namaMatkul == null ? "" : namaMatkul) + " (" + tanggal + ")";
    }
}
