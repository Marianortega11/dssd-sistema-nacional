package com.rescuesync.nacional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FlujoApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String login(String user, String pass) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode n = mapper.readTree(body);
        return "Bearer " + n.get("accessToken").asText();
    }

    private ResultActions enviar(String url, String token, String json) throws Exception {
        var rb = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) rb.header("Authorization", token);
        return mvc.perform(rb);
    }

    @Test
    void loginInvalidoYEndpointsProtegidos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/certificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/certificaciones").header("Authorization", "Bearer basura"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void registroDeOngYHabilitacion() throws Exception {
        enviar("/api/auth/register", null, "{\"nombre\":\"ONG Nueva\",\"cuit\":\"30-71234567-8\","
                + "\"email\":\"nueva@ong.org\",\"username\":\"nueva\",\"password\":\"clave1234\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.habilitada").value(false));
        enviar("/api/auth/register", null, "{\"nombre\":\"Otra\",\"cuit\":\"30712345678\","
                + "\"email\":\"o@ong.org\",\"username\":\"otra\",\"password\":\"clave1234\"}")
                .andExpect(status().isConflict());

        String ong = login("nueva", "clave1234");
        String admin = login("admin", "admin12345");
        mvc.perform(patch("/api/ongs/1/habilitacion").header("Authorization", ong)
                .contentType(MediaType.APPLICATION_JSON).content("{\"habilitada\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/ongs").param("habilitada", "false").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].nombre", hasItem("ONG Nueva")));
    }

    @Test
    void competenciaBloqueoYLiberacion() throws Exception {
        String bonita = login("bonita", "bonita12345");
        String ong = login("cruzroja", "demo12345");

        // Cruz Roja: Paramedico avanzado (riesgo hasta CRITICO) y sin certificacion de brigadistas.
        enviar("/api/validacion/competencias", bonita, "{\"ong\":\"april.sanchez\",\"gravedad\":\"Critica\","
                + "\"items\":[{\"tipoRecurso\":\"Paramédicos\",\"cantidad\":5},{\"tipoRecurso\":\"Brigadistas\",\"cantidad\":3}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("HABILITACION_PARCIAL"))
                .andExpect(jsonPath("$.capacidades[0].rolPermitido").value("PRINCIPAL"))
                .andExpect(jsonPath("$.capacidades[0].nivelNombre").value("AVANZADO"))
                .andExpect(jsonPath("$.capacidades[1].rolPermitido").value("APOYO"));

        String req = "{\"proyectoRef\":\"EMG-1\",\"ong\":\"april.sanchez\",\"items\":["
                + "{\"tipoRecurso\":\"Raciones de alimento\",\"cantidad\":4000}]}";

        // Una cuenta ONG no puede bloquear; la del sistema si.
        enviar("/api/recursos/bloqueos", ong, req).andExpect(status().isForbidden());
        enviar("/api/recursos/bloqueos", bonita, req).andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        enviar("/api/recursos/bloqueos", bonita, req).andExpect(status().isOk());

        // Solo quedan 1000 raciones: otro proyecto que pide 2000 recibe 409 con el detalle del faltante.
        enviar("/api/recursos/bloqueos", bonita, req.replace("EMG-1", "EMG-2").replace("4000", "2000"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detalles[0].faltante").value(1000));
        enviar("/api/recursos/validar-restricciones", bonita, req.replace("EMG-1", "EMG-3").replace("4000", "2000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.viable").value(false));

        mvc.perform(get("/api/recursos/disponibilidad").param("tipoRecurso", "raciones de alimento")
                        .header("Authorization", ong))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cantidadBloqueada").value(4000));

        enviar("/api/proyectos/EMG-1/finalizacion", bonita, "{\"ong\":\"april.sanchez\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("LIBERADO"));
        enviar("/api/proyectos/EMG-1/cierre", bonita, "{}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.compromisosLiberados").value(0))
                .andExpect(jsonPath("$.compromisosYaLiberados").value(1));
        mvc.perform(get("/api/recursos/disponibilidad").param("tipoRecurso", "raciones de alimento")
                        .header("Authorization", ong))
                .andExpect(jsonPath("$[0].cantidadBloqueada").value(0));
    }
}
