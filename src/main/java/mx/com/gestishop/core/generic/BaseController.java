package mx.com.gestishop.core.generic;

import mx.com.gestishop.core.dto.ApiDataResponseDTO;
import mx.com.gestishop.core.dto.ApiPageResponseDTO;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

/**
 * Clase base para controladores REST que proporciona métodos de utilidad para
 * construir respuestas API estándar.
 */
public abstract class BaseController {

    /**
     * Construye una respuesta API con el código de respuesta personalizado y los
     * datos proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param code El código de respuesta personalizado a utilizar.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * respuesta personalizado y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> build(ApiCodeResponse code, T data) {
        return ApiResponseBuilder.build(code, data);
    }

    /**
     * Construye una respuesta API con el código de éxito y los datos
     * proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * éxito y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> ok(T data) {
        return ApiResponseBuilder.build(ApiCodeResponse.SUCCESS, data);
    }

    /**
     * Construye una respuesta API con el código de éxito sin incluir datos.
     *
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * éxito y sin datos.
     */
    protected ResponseEntity<ApiDataResponseDTO<Void>> ok() {
        return ApiResponseBuilder.build(ApiCodeResponse.SUCCESS, null);
    }

    /**
     * Construye una respuesta API con el código de recurso encontrado y los datos
     * proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso encontrado y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okEncontrado(T data) {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_FOUND, data);
    }

    /**
     * Construye una respuesta API con el código de recurso encontrado sin incluir
     * datos.
     *
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso encontrado y sin datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okEncontrado() {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_FOUND, null);
    }

    /**
     * Construye una respuesta API con el código de recurso creado y los datos
     * proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso creado y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okCreado(T data) {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_CREATED, data);
    }

    /**
     * Construye una respuesta API con el código de recurso creado sin incluir
     * datos.
     *
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso creado y sin datos.
     */
    protected ResponseEntity<ApiDataResponseDTO<Void>> okCreado() {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_CREATED, null);
    }

    /**
     * Construye una respuesta API con el código de recurso actualizado y los
     * datos proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso actualizado y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okActualizado(T data) {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_UPDATED, data);
    }

    /**
     * Construye una respuesta API con el código de recurso actualizado sin
     * incluir datos.
     *
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso actualizado y sin datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okActualizado() {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_UPDATED, null);
    }

    /**
     * Construye una respuesta API con el código de recurso eliminado y los datos
     * proporcionados.
     *
     * @param <T>  El tipo de datos a incluir en la respuesta.
     * @param data Los datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso eliminado y los datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okEliminado(T data) {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_DELETED, data);
    }

    /**
     * Construye una respuesta API con el código de recurso eliminado sin incluir
     * datos.
     *
     * @return Una ResponseEntity que contiene un ApiResponseDTO con el código de
     * recurso eliminado y sin datos.
     */
    protected <T> ResponseEntity<ApiDataResponseDTO<T>> okEliminado() {
        return ApiResponseBuilder.build(ApiCodeResponse.RESOURCE_DELETED, null);
    }

    /**
     * Construye una respuesta API con el código de éxito y la página de datos
     * proporcionada.
     *
     * @param <T>  El tipo de datos en la página.
     * @param page La página de datos a incluir en la respuesta.
     * @return Una ResponseEntity que contiene un ApiPageResponseDTO con el código
     * de éxito y la página de datos.
     */
    protected <T> ResponseEntity<ApiPageResponseDTO<T>> pkPaginado(Page<T> page) {
        return ApiResponseBuilder.buildPage(ApiCodeResponse.SUCCESS, page);
    }

    /**
     * Lanza una excepción de respuesta API estándar con el código de respuesta
     * personalizado y los datos proporcionados.
     *
     * @param apiCodeResponse El código de respuesta personalizado a utilizar en la
     *                        excepción.
     * @param data            Los datos a incluir en la excepción.
     */
    protected void error(ApiCodeResponse apiCodeResponse, Object data) {
        throw new ApiResponseException(apiCodeResponse, data);
    }

    /**
     * Lanza una excepción de respuesta API para datos requeridos con el código de
     * respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exDatoRequerido(Object data) {
        error(ApiCodeResponse.REQUIRED_DATA, data);
    }

    // Lanza una excepción de respuesta API para datos requeridos sin incluir datos
    protected void exDatoRequerido() {
        error(ApiCodeResponse.REQUIRED_DATA, null);
    }

    /**
     * Lanza una excepción de respuesta API para recurso no encontrado con el código
     * de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exRecursoNoEncontrado(Object data) {
        error(ApiCodeResponse.RESOURCE_NOT_FOUND, data);
    }

    /**
     * Lanza una excepción de respuesta API para recurso no encontrado con el código
     * de respuesta sin incluir datos.
     */
    protected void exRecursoNoEncontrado() {
        error(ApiCodeResponse.RESOURCE_NOT_FOUND, null);
    }

    /**
     * Lanza una excepción de respuesta API para conflictos generales de la petición
     * con el código de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exConflicto(Object data) {
        error(ApiCodeResponse.CONFLICT, data);
    }

    /**
     * Lanza una excepción de respuesta API para conflictos generales de la petición
     * con el código de respuesta sin incluir datos.
     */
    protected void exConflicto() {
        error(ApiCodeResponse.CONFLICT, null);
    }

    /**
     * Lanza una excepción de respuesta API para errores internos del servidor con el
     * código de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exErrorInterno(Object data) {
        error(ApiCodeResponse.INTERNAL_ERROR, data);
    }

    /**
     * Lanza una excepción de respuesta API para errores internos del servidor con el
     * código de respuesta sin incluir datos.
     */
    protected void exErrorInterno() {
        error(ApiCodeResponse.INTERNAL_ERROR, null);
    }

    /**
     * Lanza una excepción de respuesta API para errores en la base de datos con el
     * código de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exErrorBaseDeDatos(Object data) {
        error(ApiCodeResponse.DATABASE_ERROR, data);
    }

    /**
     * Lanza una excepción de respuesta API para errores en la base de datos con el
     * código de respuesta sin incluir datos.
     */
    protected void exErrorBaseDeDatos() {
        error(ApiCodeResponse.DATABASE_ERROR, null);
    }

    /**
     * Lanza una excepción de respuesta API para recursos no disponibles con el código
     * de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exRecursoNoDisponible(Object data) {
        error(ApiCodeResponse.RESOURCE_UNAVAILABLE, data);
    }

    /**
     * Lanza una excepción de respuesta API para recursos no disponibles con el código
     * de respuesta sin incluir datos.
     */
    protected void esRecursoNoDisponible() {
        error(ApiCodeResponse.RESOURCE_UNAVAILABLE, null);
    }

    /**
     * Lanza una excepción de respuesta API para tiempos de espera agotados con el
     * código de respuesta y los datos.
     *
     * @param data Los datos a incluir en la respuesta.
     */
    protected void exTimeout(Object data) {
        error(ApiCodeResponse.TIMEOUT, data);
    }

    /**
     * Lanza una excepción de respuesta API para tiempos de espera agotados con el
     * código de respuesta sin incluir datos.
     */
    protected void exTimeout() {
        error(ApiCodeResponse.TIMEOUT, null);
    }
}
