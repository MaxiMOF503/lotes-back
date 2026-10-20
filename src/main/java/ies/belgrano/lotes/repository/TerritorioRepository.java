package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.dto.response.TerritorioResponse.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

@Repository
public class TerritorioRepository {
    private static final String SIN_COPIAS = "/*sin_copias*/";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private volatile boolean columnaCopias;
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
              /*sin_copias*/
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
              /*sin_copias*/
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

    public List<Feature> buscarParcelas(String prefijo,int limite,int offset) {
        return features("""
            SELECT origen_id, ST_AsGeoJSON(geometria) geometria_json, propiedades
            FROM territorio_elementos
            WHERE capa='catastro' AND valida=TRUE
              AND JSON_UNQUOTE(JSON_EXTRACT(propiedades,'$.nomenclatu')) LIKE CONCAT(?,'%')
              /*sin_copias*/
            ORDER BY JSON_UNQUOTE(JSON_EXTRACT(propiedades,'$.nomenclatu')),origen_id LIMIT ? OFFSET ?
            """, prefijo,limite,offset);
    }
    public java.util.Optional<Feature> parcela(String id) {
        return features("""
            SELECT origen_id, ST_AsGeoJSON(geometria) geometria_json, propiedades
            FROM territorio_elementos WHERE capa='catastro' AND valida=TRUE AND origen_id=?
            """,id).stream().findFirst();
    }
    public boolean pertenece(String id,String punto) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM territorio_elementos
              WHERE capa='catastro' AND valida=TRUE AND origen_id=?
              AND ST_Intersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat')))
            """,Boolean.class,id,punto));
    }

    /**
     * Parcelas a menos de {@code radio} metros del punto, de la más cercana a la más lejana.
     * Un punto de Georef cae sobre el eje de la calle, no dentro de una parcela: por eso se busca alrededor.
     */
    public List<Cercana> cercanas(String punto, String bounds, double radio, int limite) {
        String sql = """
            SELECT origen_id, ST_AsGeoJSON(geometria) geometria_json, propiedades,
              ST_Distance(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat')) distancia
            FROM territorio_elementos FORCE INDEX (territorio_geometria)
            WHERE capa='catastro' AND valida=TRUE
              AND MBRIntersects(geometria,ST_GeomFromText(?,4326,'axis-order=long-lat'))
              /*sin_copias*/
            HAVING distancia <= ? ORDER BY distancia,origen_id LIMIT ?
            """.replace(SIN_COPIAS, filtroCopias());
        return jdbc.query(sql, (rs, row) -> new Cercana(new Feature("Feature", rs.getString("origen_id"),
            mapper.readTree(rs.getString("geometria_json")), mapper.readTree(rs.getString("propiedades"))),
            Math.round(rs.getDouble("distancia") * 10.0) / 10.0), punto, bounds, radio, limite);
    }

    /**
     * La fuente municipal repite algunas parcelas; el importador marca las copias en duplicado_de.
     * Las búsquedas las ocultan, pero parcela(id) las sigue resolviendo para no romper fichas,
     * consultas guardadas ni historial. Sin la columna (importación anterior) no se filtra.
     */
    private String filtroCopias() {
        if (!columnaCopias) columnaCopias = Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT COUNT(*)>0 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
              AND TABLE_NAME='territorio_elementos' AND COLUMN_NAME='duplicado_de'
            """, Boolean.class));
        return columnaCopias ? "AND duplicado_de IS NULL" : "";
    }

    private List<Feature> features(String sql, Object... args) {
        if (sql.contains(SIN_COPIAS)) sql = sql.replace(SIN_COPIAS, filtroCopias());
        return jdbc.query(sql, (rs, row) -> new Feature("Feature", rs.getString("origen_id"),
            mapper.readTree(rs.getString("geometria_json")),
            mapper.readTree(rs.getString("propiedades"))), args);
    }
}
