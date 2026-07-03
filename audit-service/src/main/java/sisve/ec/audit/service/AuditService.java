package sisve.ec.audit.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import sisve.ec.audit.db.AuditoriaEntity;
import sisve.ec.audit.dto.EventoAuditoriaRequest;
import sisve.ec.audit.dto.EventoAuditoriaResponse;
import sisve.ec.audit.mapper.AuditoriaMapper;
import sisve.ec.audit.repository.AuditoriaRepository;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaMapper auditoriaMapper;
    private final Counter eventosRegistrados;
    private final Counter consultasPorServicio;
    private final Counter consultasPorTipo;

    public AuditService(AuditoriaRepository auditoriaRepository, AuditoriaMapper auditoriaMapper, MeterRegistry meterRegistry) {
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaMapper = auditoriaMapper;
        this.eventosRegistrados = meterRegistry.counter("audit_service_eventos_registrados_total");
        this.consultasPorServicio = meterRegistry.counter("audit_service_consultas_por_servicio_total");
        this.consultasPorTipo = meterRegistry.counter("audit_service_consultas_por_tipo_total");
    }

    @Transactional
    public EventoAuditoriaResponse registrarEvento(EventoAuditoriaRequest request) {
        AuditoriaEntity entity = auditoriaMapper.toEntity(request);
        auditoriaRepository.persist(entity);
        eventosRegistrados.increment();
        return auditoriaMapper.toResponse(entity);
    }

    public List<EventoAuditoriaResponse> listarPorServicio(String servicioOrigen, LocalDateTime desde) {
        consultasPorServicio.increment();
        List<AuditoriaEntity> resultado = desde == null
                ? auditoriaRepository.findByServicio(servicioOrigen)
                : auditoriaRepository.findByServicioYFecha(servicioOrigen, desde);
        return auditoriaMapper.toResponseList(resultado);
    }

    public List<EventoAuditoriaResponse> listarPorTipoEvento(String tipoEvento) {
        consultasPorTipo.increment();
        return auditoriaMapper.toResponseList(auditoriaRepository.findByTipoEvento(tipoEvento));
    }
}