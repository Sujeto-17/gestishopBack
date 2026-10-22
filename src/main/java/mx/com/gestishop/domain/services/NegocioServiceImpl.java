package mx.com.gestishop.domain.services;

import lombok.RequiredArgsConstructor;
import mx.com.gestishop.application.dto.request.NegocioRequestDTO;
import mx.com.gestishop.application.dto.response.ArchivoResponseDTO;
import mx.com.gestishop.application.dto.response.NegocioResponseDTO;
import mx.com.gestishop.application.mapper.NegocioMapper;
import mx.com.gestishop.core.enums.ApiCodeResponse;
import mx.com.gestishop.core.exception.ApiResponseException;
import mx.com.gestishop.core.exception.RepositoryExecutor;
import mx.com.gestishop.core.generic.ErrorFactory;
import mx.com.gestishop.domain.interfaces.ArchivoService;
import mx.com.gestishop.domain.interfaces.NegocioService;
import mx.com.gestishop.domain.interfaces.SuscripcionService;
import mx.com.gestishop.domain.model.CatGiroNegocio;
import mx.com.gestishop.domain.model.CatMunicipio;
import mx.com.gestishop.domain.model.Negocio;
import mx.com.gestishop.domain.model.Plan;
import mx.com.gestishop.domain.repository.*;
import mx.com.gestishop.infrastructure.utils.FechaNegocioUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación de la gestión de negocios. Es el último eslabón de la
 * cadena de dependencias: requiere que el giro y el plan ya existan en BD
 * (ambos son FK obligatorias), por eso este servicio se construye AL FINAL,
 * después de tener CatGiroNegocioService y PlanService funcionando.
 */
@Service
@RequiredArgsConstructor
public class NegocioServiceImpl implements NegocioService {

    private final NegocioRepository negocioRepository;
    private final CatGiroNegocioRepository catGiroNegocioRepository;
    private final PlanRepository planRepository;
    private final CatMunicipioRepository catMunicipioRepository;
    private final AdminNegocioRepository adminNegocioRepository;
    private final TrabajadorRepository trabajadorRepository;

    private final SuscripcionService suscripcionService;
    private final ArchivoService archivoService;

    private static final Set<String> TIPOS_LOGO_PERMITIDOS =
            Set.of("image/jpeg", "image/png", "image/webp");

    // Carga el municipio (obligatorio para un negocio) o falla con "no encontrado"
    private CatMunicipio cargarMunicipio(Long idMunicipio) {
        return RepositoryExecutor.execute(
                () -> catMunicipioRepository.findById(idMunicipio)
                        .orElseThrow(() -> ErrorFactory.notFound("CatMunicipio", idMunicipio)),
                "CatMunicipio", "consultar"
        );
    }

    // Un plan inactivo no se puede asignar a un negocio
    private void validarPlanActivo(Plan plan) {
        if (!"activo".equals(plan.getEstatus())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "El plan «" + plan.getNombre() + "» está inactivo y no se puede asignar."));
        }
    }

    /**
     * Datos de facturación: o se capturan los 4 (razón social, RFC, régimen fiscal y código postal
     * fiscal) o no se captura ninguno. Capturar solo algunos deja un negocio que no se puede facturar.
     */
    private void validarDatosFiscales(NegocioRequestDTO dto) {
        int capturados = 0;
        if (dto.getRazonSocial() != null) capturados++;
        if (dto.getRfc() != null) capturados++;
        if (dto.getRegimenFiscal() != null) capturados++;
        if (dto.getCodigoPostalFiscal() != null) capturados++;

        if (capturados > 0 && capturados < 4) {
            throw new ApiResponseException(ApiCodeResponse.REQUIRED_DATA,
                    Map.of("detalle", "Para facturar se necesitan los 4 datos fiscales: razón social, RFC, régimen fiscal y código postal fiscal."));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO crear(NegocioRequestDTO dto) {
        validarDatosFiscales(dto);

        // Validar correo único entre negocios activos
        boolean correoExiste = RepositoryExecutor.execute(
                () -> negocioRepository.existeCorreoActivo(dto.getCorreo()), "Negocio", "validarCorreo");

        if (correoExiste) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "Ya existe un negocio registrado con ese correo."));
        }

        // Cargar el giro y el plan (ambos deben existir, son FK obligatorias)
        CatGiroNegocio giro = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(dto.getIdGiro())
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", dto.getIdGiro())),
                "CatGiroNegocio", "consultar"
        );

        Plan plan = RepositoryExecutor.execute(
                () -> planRepository.findById(dto.getIdPlan())
                        .orElseThrow(() -> ErrorFactory.notFound("Plan", dto.getIdPlan())),
                "Plan", "consultar"
        );

        // Construir y guardar la entidad (fechaAlta/vencimiento se calculan en el mapper)
        validarPlanActivo(plan);

        Negocio entidad = NegocioMapper.toEntity(dto);
        entidad.setGiro(giro);
        entidad.setPlan(plan);
        entidad.setMunicipio(cargarMunicipio(dto.getIdMunicipio()));

        Negocio guardado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "crear");

        // Se crea automáticamente el primer periodo de suscripción (prueba, estatus
        // 'pendiente'), con las mismas fechas que ya se calcularon para el negocio.
        // Al estar dentro de la misma transacción @Transactional del metodo, si algo
        // falla aquí, TAMBIÉN se revierte la creación del negocio (todo o nada)
        suscripcionService.crearPeriodoInicial(guardado, plan);

        return respuesta(guardado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO actualizar(UUID uuidNegocio, NegocioRequestDTO dto) {
        validarDatosFiscales(dto);

        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "actualizar"
        );

        CatGiroNegocio giro = RepositoryExecutor.execute(
                () -> catGiroNegocioRepository.findById(dto.getIdGiro())
                        .orElseThrow(() -> ErrorFactory.notFound("CatGiroNegocio", dto.getIdGiro())),
                "CatGiroNegocio", "consultar"
        );

        entidad.setNombre(dto.getNombre());
        entidad.setLogoUrl(dto.getLogoUrl());
        entidad.setSistemaType(dto.getSistemaType());
        entidad.setGiro(giro);
        entidad.setCorreo(dto.getCorreo());
        entidad.setTelefono(dto.getTelefono());
        entidad.setDireccion(dto.getDireccion());
        entidad.setMunicipio(cargarMunicipio(dto.getIdMunicipio()));
        NegocioMapper.aplicarDatosComplementarios(entidad, dto);

        // Cambio de plan. El periodo en curso no cambia de precio ni de fechas: el plan nuevo se cobra desde
        // la siguiente renovación; sus MÓDULOS rigen en cuanto el usuario renueve su sesión (máximo 15 min).
        // Antes el plan enviado al editar se ignoraba en silencio.
        if (!entidad.getPlan().getIdPlan().equals(dto.getIdPlan())) {
            Plan nuevoPlan = RepositoryExecutor.execute(
                    () -> planRepository.findById(dto.getIdPlan())
                            .orElseThrow(() -> ErrorFactory.notFound("Plan", dto.getIdPlan())),
                    "Plan", "consultar"
            );
            validarPlanActivo(nuevoPlan);
            entidad.setPlan(nuevoPlan);
        }

        Negocio actualizado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "actualizar");
        return respuesta(actualizado);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO cambiarEstatus(UUID uuidNegocio) {
        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "cambiarEstatus"
        );

        // Suspender: cualquier negocio vigente (activo o en prueba) pasa a 'inactivo'.
        // Reactivar: un negocio 'inactivo' vuelve a 'activo'.
        // Antes la condición era "activo ? inactivo : activo", así que un negocio en 'prueba'
        // pasaba a 'activo' justo cuando el superadmin intentaba suspenderlo.
        boolean reactivando = "inactivo".equals(entidad.getEstatus());

        // Un negocio cuyo periodo ya terminó NO se reactiva a mano: volvería a quedar bloqueado en el login
        // por la fecha. Para darle acceso otra vez se registra su pago en el módulo Suscripciones.
        if (reactivando && entidad.getVencimiento().isBefore(FechaNegocioUtil.hoy())) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "El periodo de este negocio ya terminó. Para darle acceso otra vez, registra su pago en Suscripciones."));
        }

        entidad.setEstatus(reactivando ? "activo" : "inactivo");

        Negocio actualizado = RepositoryExecutor.execute(() -> negocioRepository.save(entidad), "Negocio", "cambiarEstatus");
        return respuesta(actualizado);
    }

    @Override
    public NegocioResponseDTO obtenerPorUuid(UUID uuidNegocio) {
        Negocio entidad = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "consultar"
        );
        return respuesta(entidad);
    }

    // Lista todos los negocios con sus conteos de administradores y trabajadores activos
    @Override
    public List<NegocioResponseDTO> listarTodos() {
        List<Negocio> negocios = RepositoryExecutor.execute(negocioRepository::listarTodos, "Negocio", "listarTodos");

        // Dos consultas agrupadas para TODOS los negocios (en vez de dos consultas por cada negocio)
        Map<Long, Long> administradores = contarPorNegocio(RepositoryExecutor.execute(
                adminNegocioRepository::contarAdminsActivosAgrupados, "AdminNegocio", "contarAdmins"));
        Map<Long, Long> trabajadores = contarPorNegocio(RepositoryExecutor.execute(
                trabajadorRepository::contarActivosAgrupados, "Trabajador", "contarTrabajadores"));

        return negocios.stream()
                .map(n -> NegocioMapper.toResponse(n,
                        administradores.getOrDefault(n.getIdNegocio(), 0L),
                        trabajadores.getOrDefault(n.getIdNegocio(), 0L)))
                .toList();
    }

    // Arma la respuesta de UN negocio, consultando sus conteos
    private NegocioResponseDTO respuesta(Negocio negocio) {
        long administradores = RepositoryExecutor.execute(
                () -> adminNegocioRepository.contarAdminsActivos(negocio.getIdNegocio()), "AdminNegocio", "contarAdmins");
        long trabajadores = RepositoryExecutor.execute(
                () -> trabajadorRepository.contarActivosPorNegocio(negocio.getIdNegocio()), "Trabajador", "contarTrabajadores");
        return NegocioMapper.toResponse(negocio, administradores, trabajadores);
    }

    // Filas [idNegocio, cantidad] -> mapa idNegocio -> cantidad
    private Map<Long, Long> contarPorNegocio(List<Object[]> filas) {
        return filas.stream().collect(Collectors.toMap(f -> (Long) f[0], f -> (Long) f[1]));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NegocioResponseDTO subirLogo(UUID uuidNegocio, MultipartFile archivo) {

        validarTipoLogo(archivo);

        Negocio negocio = RepositoryExecutor.execute(
                () -> negocioRepository.buscarPorUuid(uuidNegocio)
                        .orElseThrow(() -> ErrorFactory.notFound("Negocio", uuidNegocio)),
                "Negocio", "subirLogo"
        );

        ArchivoResponseDTO archivoGuardado = archivoService.subir(
                "negocio", negocio.getIdNegocio(), "logo", archivo, true);

        // Se actualiza el puntero rápido del negocio al nuevo logo vigente
        negocio.setLogoUrl(archivoGuardado.getRutaRelativa());
        RepositoryExecutor.executeVoid(() -> negocioRepository.save(negocio), "Negocio", "subirLogo");

        return respuesta(negocio);
    }

    private void validarTipoLogo(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if (contentType == null || !TIPOS_LOGO_PERMITIDOS.contains(contentType)) {
            throw new ApiResponseException(ApiCodeResponse.CONFLICT,
                    Map.of("detalle", "El logo debe ser una imagen JPG, PNG o WEBP."));
        }
    }
}