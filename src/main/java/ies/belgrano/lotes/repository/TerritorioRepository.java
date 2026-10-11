package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

@Repository
public class TerritorioRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public TerritorioRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<Fuente> fuentes() {
        return jdbc.query("""
            SELECT f.*, (SELECT COUNT(*) FROM territorio_sin_geometria s WHERE s.capa=f.capa) sin_geometria
            FROM territorio_fuentes f ORDER BY f.capa
            """, (rs, row) -> new Fuente(rs.getString("capa"), rs.getString("fuente"),
            rs.getString("url"), rs.getInt("registros"), rs.getInt("invalidos"),
            rs.getInt("sin_geometria"), rs.getString("importado_en")));
    }

    public List<Feature> ventana(String capa, String bounds, int limite) {
        return features("""
            SELECT origen_id, ST_AsGeoJSON(geometria) geometria_json, propiedades
            FROM territorio_elementos FORCE INDEX (territorio_geometria)
            WHERE capa=? AND valida=TRUE AND MBRIntersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat'))
            ORDER BY origen_id LIMIT ?
            """, capa, bounds, limite);
    }

    public List<Feature> parcelas(String punto, int limite) {
        return features("""
            SELECT origen_id, ST_AsGeoJSON(geometria) geometria_json, propiedades
            FROM territorio_elementos FORCE INDEX (territorio_geometria)
            WHERE capa='catastro' AND valida=TRUE
              AND MBRIntersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat'))
              AND ST_Intersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat'))
            ORDER BY origen_id LIMIT ?
            """, punto, punto, limite);
    }

    public boolean dentroAgua(String punto) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM territorio_elementos WHERE capa='aysam' AND valida=TRUE
              AND ST_Intersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat')))
            """, Boolean.class, punto));
    }

    public Electricidad electricidad(String punto, String bounds, int radio) {
        List<Electricidad> rows = jdbc.query("""
            SELECT JSON_UNQUOTE(JSON_EXTRACT(propiedades,'$.tipo')) tipo,
              ST_Distance(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat')) distancia
            FROM territorio_elementos FORCE INDEX (territorio_geometria)
            WHERE capa='electricidad' AND valida=TRUE
              AND MBRIntersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat'))
            HAVING distancia <= ? ORDER BY distancia,origen_id LIMIT 1
            """, (rs, row) -> new Electricidad(Math.round(rs.getDouble("distancia") * 10.0) / 10.0,
                rs.getString("tipo"), radio, "Proximidad a un tramo publicado; no confirma factibilidad ni conexión."),
            punto, bounds, radio);
        return rows.isEmpty() ? new Electricidad(null, null, radio,
            "No se encontró un tramo publicado dentro del radio; no implica ausencia de servicio.") : rows.getFirst();
    }

    private List<Feature> features(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new Feature("Feature", rs.getString("origen_id"),
            mapper.readTree(rs.getString("geometria_json")),
            mapper.readTree(rs.getString("propiedades"))), args);
    }
}
