package ies.belgrano.lotes.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;
import java.sql.ResultSet;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Verifica el SQL de compatibilidad sin simular que fue ejecutado en MySQL. */
class TerritorioReferenciasRepositoryTest {
    static final String ID="Parcelas_visor0.55137";

    @Test void sinColumnaNoConsultaDuplicadoDeNiCteYConservaLaIdentidadOriginal() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);
        var rs=mock(ResultSet.class);
        when(rs.getString("origen_id")).thenReturn(ID);
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<RowMapper<TerritorioRepository.EnlaceParcela>>any(),eq(ID)))
            .thenAnswer(i->{
                String sql=i.getArgument(0);
                assertThat(sql).contains("NULL AS duplicado_de").doesNotContain("WITH RECURSIVE","SELECT origen_id, duplicado_de");
                RowMapper<TerritorioRepository.EnlaceParcela> filas=i.getArgument(1);
                return List.of(filas.mapRow(rs,0));
            });
        var repository=new TerritorioRepository(jdbc,JsonMapper.builder().build());
        assertThat(repository.enlaceParcela(ID)).contains(new TerritorioRepository.EnlaceParcela(ID,null));
        assertThat(repository.referenciasDelRepresentante(ID)).containsExactly(ID);
        verify(jdbc,never()).queryForList(anyString(),eq(String.class),anyString());
    }

    @Test void conColumnaUsaElEnlaceYRecorreSoloDescendientesValidosDelGrupo(){
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);
        when(jdbc.queryForList(anyString(),eq(String.class),eq(ID))).thenReturn(List.of(ID));
        var repository=new TerritorioRepository(jdbc,JsonMapper.builder().build());
        assertThat(repository.referenciasDelRepresentante(ID)).containsExactly(ID);
        verify(jdbc).queryForList(argThat(sql->sql.contains("JOIN referencias r ON e.duplicado_de=r.origen_id")
            && sql.contains("e.capa='catastro' AND e.valida=TRUE") && sql.contains("ORDER BY origen_id")),eq(String.class),eq(ID));
    }
}
