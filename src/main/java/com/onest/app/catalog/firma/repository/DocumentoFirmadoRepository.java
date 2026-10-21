package com.onest.app.catalog.firma.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Autor de cada documento (SERV_MED_DOC_AUTOR) y folios de los impresos con firma
 * (SERV_MED_FIRMA_IMPRESO). Ver docs/portal-firmas-digitalizadas.sql.
 */
@Repository
public class DocumentoFirmadoRepository {

    public record Autor(String registro, String elaboroUsuario, String elaboroNombre, String medicoUsuario,
                        String medicoNombre, String medicoCedula, String origen) {
    }

    public record Impreso(long folio, String codigo, String formato, String registro, String nss,
                          String elaboroNombre, String medicoNombre, String medicoCedula, String impresoPor,
                          LocalDateTime impresoAt, Long firmaMedicoId, Long firmaElaboroId) {
    }

    private final JdbcTemplate jdbc;

    public DocumentoFirmadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Autor> autor(String registro) {
        return jdbc.query("SELECT REGISTRO, ELABORO_USUARIO, ELABORO_NOMBRE, MEDICO_USUARIO, MEDICO_NOMBRE, MEDICO_CEDULA, "
                        + "ORIGEN FROM SERV_MED_DOC_AUTOR WHERE REGISTRO = ?",
                (rs, i) -> new Autor(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)),
                registro).stream().findFirst();
    }

    /**
     * Fija el autor del documento solo si aun no tiene (el primero gana: un documento no cambia de autor).
     * Si dos peticiones chocan, la segunda se ignora y se devuelve el que quedo.
     */
    public Autor fijarAutor(Autor a, String nss) {
        Optional<Autor> ya = autor(a.registro());
        if (ya.isPresent()) {
            return ya.get();
        }
        try {
            jdbc.update("INSERT INTO SERV_MED_DOC_AUTOR (REGISTRO, NSS, ELABORO_USUARIO, ELABORO_NOMBRE, MEDICO_USUARIO, "
                            + "MEDICO_NOMBRE, MEDICO_CEDULA, ORIGEN) VALUES (?,?,?,?,?,?,?,?)",
                    a.registro(), nss, a.elaboroUsuario(), a.elaboroNombre(), a.medicoUsuario(), a.medicoNombre(),
                    a.medicoCedula(), a.origen());
            return a;
        } catch (DuplicateKeyException ex) {
            return autor(a.registro()).orElse(a);
        }
    }

    public long registrarImpreso(String codigo, String formato, String registro, String nss, Long firmaMedicoId,
                                 Long firmaElaboroId, String elaboroNombre, String medicoNombre, String medicoCedula,
                                 String impresoPor) {
        var kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement("INSERT INTO SERV_MED_FIRMA_IMPRESO (CODIGO, FORMATO, REGISTRO, NSS, FIRMA_MEDICO_ID, "
                    + "FIRMA_ELABORO_ID, ELABORO_NOMBRE, MEDICO_NOMBRE, MEDICO_CEDULA, IMPRESO_POR) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    new String[]{"FOLIO"});
            ps.setString(1, codigo);
            ps.setString(2, formato);
            ps.setString(3, registro);
            ps.setString(4, nss);
            ps.setObject(5, firmaMedicoId);
            ps.setObject(6, firmaElaboroId);
            ps.setString(7, elaboroNombre);
            ps.setString(8, medicoNombre);
            ps.setString(9, medicoCedula);
            ps.setString(10, impresoPor);
            return ps;
        }, kh);
        Number k = kh.getKey();
        return k == null ? -1L : k.longValue();
    }

    public Optional<Impreso> impreso(long folio) {
        return jdbc.query("SELECT FOLIO, CODIGO, FORMATO, REGISTRO, NSS, ELABORO_NOMBRE, MEDICO_NOMBRE, MEDICO_CEDULA, "
                        + "IMPRESO_POR, IMPRESO_AT, FIRMA_MEDICO_ID, FIRMA_ELABORO_ID FROM SERV_MED_FIRMA_IMPRESO WHERE FOLIO = ?",
                (rs, i) -> new Impreso(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getTimestamp(10) == null ? null : rs.getTimestamp(10).toLocalDateTime(),
                        rs.getObject(11) == null ? null : rs.getLong(11), rs.getObject(12) == null ? null : rs.getLong(12)),
                folio).stream().findFirst();
    }
}
