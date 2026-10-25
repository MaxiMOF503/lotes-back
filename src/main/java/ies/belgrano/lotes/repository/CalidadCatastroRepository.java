package ies.belgrano.lotes.repository;

import ies.belgrano.lotes.dto.response.CalidadCatastroResponse.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;

@Repository
public class CalidadCatastroRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public CalidadCatastroRepository(JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}

    public Resumen resumen(){
        return jdbc.queryForObject("""
            SELECT COUNT(*) grupos,
              COALESCE(SUM(conflictos=0 AND pendientes=0),0) identicos,
              COALESCE(SUM(conflictos>0),0) conflictivos,
              COALESCE(SUM(identicos),0) copias_identicas,
              COALESCE(SUM(conflictos),0) copias_conflictivas,
              COALESCE(SUM(copias+1),0) involucrados,
              COALESCE(SUM(IF(conflictos>0,copias+1,0)),0) involucrados_conflicto,
              COALESCE(SUM(pendientes),0) pendientes
            FROM (
              SELECT duplicado_de, COUNT(*) copias,
                SUM(tipo_duplicado='DUPLICADO_IDENTICO') identicos,
                SUM(tipo_duplicado='CONFLICTO_ATRIBUTOS') conflictos,
                SUM(tipo_duplicado IS NULL OR campos_conflicto IS NULL
                    OR tipo_duplicado NOT IN ('DUPLICADO_IDENTICO','CONFLICTO_ATRIBUTOS')) pendientes
              FROM territorio_elementos WHERE capa='catastro' AND duplicado_de IS NOT NULL
              GROUP BY duplicado_de
            ) grupos
            """,(rs,fila)->new Resumen(rs.getLong("grupos"),rs.getLong("identicos"),rs.getLong("conflictivos"),
                rs.getLong("copias_identicas"),rs.getLong("copias_conflictivas"),rs.getLong("involucrados"),
                rs.getLong("involucrados_conflicto"),rs.getLong("pendientes"),rs.getLong("pendientes")==0));
    }

    public Optional<String> representante(String id){
        return jdbc.query("SELECT COALESCE(duplicado_de,origen_id) FROM territorio_elementos WHERE capa='catastro' AND origen_id=?",
            (rs,fila)->rs.getString(1),id).stream().findFirst();
    }

    public List<GrupoResumen> grupos(boolean soloConflictos,int limite,int offset){
        return jdbc.query("""
            SELECT duplicado_de representante,COUNT(*) copias,
              COALESCE(SUM(tipo_duplicado='CONFLICTO_ATRIBUTOS'),0) conflictos,
              SUM(tipo_duplicado IS NULL OR campos_conflicto IS NULL
                  OR tipo_duplicado NOT IN ('DUPLICADO_IDENTICO','CONFLICTO_ATRIBUTOS')) pendientes
            FROM territorio_elementos WHERE capa='catastro' AND duplicado_de IS NOT NULL
            GROUP BY duplicado_de HAVING (?=FALSE OR conflictos>0)
            ORDER BY duplicado_de LIMIT ? OFFSET ?
            """,(rs,fila)->new GrupoResumen(rs.getString("representante"),rs.getLong("copias"),
                rs.getLong("conflictos"),rs.getLong("pendientes")),soloConflictos,limite,offset);
    }

    public List<Registro> grupo(String representante){
        return jdbc.query("""
            SELECT origen_id,duplicado_de,tipo_duplicado,campos_conflicto,propiedades
            FROM territorio_elementos WHERE capa='catastro' AND (origen_id=? OR duplicado_de=?)
            ORDER BY (origen_id=?) DESC,origen_id
            """,(rs,fila)->new Registro(rs.getString("origen_id"),rs.getString("duplicado_de"),
                rs.getString("tipo_duplicado"),rs.getString("campos_conflicto")==null?List.of():
                    mapper.readValue(rs.getString("campos_conflicto"),mapper.getTypeFactory().constructCollectionType(List.class,String.class)),
                mapper.readTree(rs.getString("propiedades"))),representante,representante,representante);
    }
}
