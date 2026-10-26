package model;

/** Entitas akun pengguna (tabel users). */
public class User {
    private int id;
    private String username;
    private String passwordHash; // tidak pernah plaintext
    private String role;          // DOSEN | MAHASISWA
    private Integer mahasiswaId;  // ref ke mahasiswa bila role=MAHASISWA
    private Integer dosenId;      // ref ke dosen bila role=DOSEN
    private String namaLengkap;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getMahasiswaId() { return mahasiswaId; }
    public void setMahasiswaId(Integer mahasiswaId) { this.mahasiswaId = mahasiswaId; }
    public Integer getDosenId() { return dosenId; }
    public void setDosenId(Integer dosenId) { this.dosenId = dosenId; }
    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }
}
