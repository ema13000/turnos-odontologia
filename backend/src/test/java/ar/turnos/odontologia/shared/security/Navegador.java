package ar.turnos.odontologia.shared.security;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


/**
 * Simula un navegador sobre MockMvc: recuerda la sesión y la cookie XSRF-TOKEN, y reenvía el token en el
 * encabezado X-XSRF-TOKEN como lo hace el frontend (sin el atajo csrf() de spring-security-test).
 */
public final class Navegador {

    public static final String COOKIE_CSRF = "XSRF-TOKEN";
    public static final String ENCABEZADO_CSRF = "X-XSRF-TOKEN";

    private final MockMvc mvc;
    private MockHttpSession sesion;
    private Cookie csrf;

    public Navegador(MockMvc mvc) {
        this.mvc = mvc;
    }

    public MockHttpSession sesion() {
        return sesion;
    }

    public void usarSesion(MockHttpSession sesion) {
        this.sesion = sesion;
    }

    public String tokenCsrf() {
        return csrf == null ? null : csrf.getValue();
    }

    public void usarTokenCsrf(String valor) {
        this.csrf = valor == null ? null : new Cookie(COOKIE_CSRF, valor);
    }

    public ResultActions get(String url) throws Exception {
        return enviar(MockMvcRequestBuilders.get(url));
    }

    /** POST con la cookie y el encabezado CSRF iguales (el caso normal del frontend). */
    public ResultActions post(String url, String json) throws Exception {
        return enviar(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json)
                .header(ENCABEZADO_CSRF, tokenCsrf() == null ? "" : tokenCsrf()));
    }

    /** POST sin el encabezado X-XSRF-TOKEN (aunque haya cookie). */
    public ResultActions postSinEncabezadoCsrf(String url, String json) throws Exception {
        return enviar(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json));
    }

    /** POST con un encabezado X-XSRF-TOKEN distinto del de la cookie. */
    public ResultActions postConEncabezadoCsrf(String url, String json, String encabezado) throws Exception {
        return enviar(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(json == null ? "" : json)
                .header(ENCABEZADO_CSRF, encabezado));
    }

    public ResultActions enviar(MockHttpServletRequestBuilder pedido) throws Exception {
        if (sesion != null) {
            pedido.session(sesion);
        }
        if (csrf != null) {
            pedido.cookie(csrf);
        }
        MvcResult resultado = mvc.perform(pedido).andReturn();
        recordar(resultado);
        return new ResultActions() {
            @Override
            public ResultActions andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
                matcher.match(resultado);
                return this;
            }

            @Override
            public ResultActions andDo(org.springframework.test.web.servlet.ResultHandler handler) throws Exception {
                handler.handle(resultado);
                return this;
            }

            @Override
            public MvcResult andReturn() {
                return resultado;
            }
        };
    }

    private void recordar(MvcResult resultado) {
        for (Cookie cookie : resultado.getResponse().getCookies()) {
            if (COOKIE_CSRF.equals(cookie.getName())) {
                csrf = cookie.getMaxAge() == 0 || cookie.getValue() == null || cookie.getValue().isEmpty() ? null : cookie;
            }
        }
        if (resultado.getRequest().getSession(false) instanceof MockHttpSession nueva) {
            sesion = nueva;
        }
    }
}
