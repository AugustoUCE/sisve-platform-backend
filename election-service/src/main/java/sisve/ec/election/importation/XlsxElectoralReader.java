package sisve.ec.election.importation;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sisve.ec.election.importation.dto.ValidationErrorDTO;
import sisve.ec.election.importation.model.CandidateImportRow;
import sisve.ec.election.importation.model.ElectionImportRow;
import sisve.ec.election.importation.model.ElectoralWorkbook;
import sisve.ec.election.importation.model.PollingMemberImportRow;
import sisve.ec.election.importation.model.PollingStationImportRow;
import sisve.ec.election.importation.model.PositionImportRow;
import sisve.ec.election.importation.model.VoterImportRow;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class XlsxElectoralReader {
    private static final Set<String> REQUIRED_SHEETS = Set.of(
            "ELECCION", "MESAS", "VOTANTES", "MIEMBROS_MESA", "CARGOS", "CANDIDATOS");
    private static final Set<String> IGNORED_SHEETS = Set.of("INSTRUCCIONES", "MAPEO_IMPORTACION", "RESUMEN");
    private static final DataFormatter FORMATTER = new DataFormatter();

    public ReadResult read(InputStream input) {
        List<ValidationErrorDTO> errors = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(input)) {
            for (String sheetName : REQUIRED_SHEETS) {
                if (workbook.getSheet(sheetName) == null) {
                    errors.add(error(sheetName, 1, "", "REQUIRED_SHEET_MISSING", "La hoja obligatoria no existe."));
                }
            }
            if (!errors.isEmpty()) return new ReadResult(null, errors);

            ElectoralWorkbook data = new ElectoralWorkbook(
                    readElections(workbook.getSheet("ELECCION"), errors),
                    readStations(workbook.getSheet("MESAS"), errors),
                    readVoters(workbook.getSheet("VOTANTES"), errors),
                    readMembers(workbook.getSheet("MIEMBROS_MESA"), errors),
                    readPositions(workbook.getSheet("CARGOS"), errors),
                    readCandidates(workbook.getSheet("CANDIDATOS"), errors));
            return new ReadResult(data, errors);
        } catch (IOException | RuntimeException exception) {
            errors.add(error("", 1, "", "INVALID_XLSX", "No se pudo leer el archivo XLSX: " + exception.getMessage()));
            return new ReadResult(null, errors);
        }
    }

    private List<ElectionImportRow> readElections(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_eleccion_ref", "nombre", "descripcion", "fecha_inicio", "fecha_fin", "estado"), errors);
        List<ElectionImportRow> result = new ArrayList<>();
        Set<Long> refs = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) {
            if (empty(row, header)) continue;
            Long ref = longValue(row, header, "id_eleccion_ref", sheet.getSheetName(), errors, true);
            String name = text(row, header, "nombre");
            String description = text(row, header, "descripcion");
            LocalDateTime start = date(row, header, "fecha_inicio", sheet.getSheetName(), errors);
            LocalDateTime end = date(row, header, "fecha_fin", sheet.getSheetName(), errors);
            String status = text(row, header, "estado");
            if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_eleccion_ref", "DUPLICATE_ELECTION_REF");
            required(errors, sheet, row, "nombre", name);
            required(errors, sheet, row, "fecha_inicio", start);
            required(errors, sheet, row, "fecha_fin", end);
            if (start != null && end != null && !start.isBefore(end)) add(errors, sheet, row.getRowNum() + 1, "fecha_inicio", "INVALID_DATE_RANGE", "fecha_inicio debe ser anterior a fecha_fin.");
            if (status != null && !Set.of("planificado", "activo").contains(status)) add(errors, sheet, row.getRowNum() + 1, "estado", "INVALID_ELECTION_STATUS", "Estado permitido: planificado o activo.");
            if (ref != null && name != null && start != null && end != null) result.add(new ElectionImportRow(row.getRowNum() + 1, ref, name, description, start, end, status));
        }
        return result;
    }

    private List<PollingStationImportRow> readStations(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_mesa_ref", "id_eleccion_ref", "code", "name", "location", "status"), errors);
        List<PollingStationImportRow> result = new ArrayList<>(); Set<Long> refs = new HashSet<>(); Set<String> codes = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) {
            if (empty(row, header)) continue;
            Long ref = longValue(row, header, "id_mesa_ref", sheet.getSheetName(), errors, true);
            Long election = longValue(row, header, "id_eleccion_ref", sheet.getSheetName(), errors, true);
            String code = text(row, header, "code"), name = text(row, header, "name"), location = text(row, header, "location"), status = text(row, header, "status");
            if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_mesa_ref", "DUPLICATE_STATION_REF");
            if (code != null && !codes.add(code)) duplicate(errors, sheet, row, "code", "DUPLICATE_STATION_CODE");
            required(errors, sheet, row, "code", code); required(errors, sheet, row, "name", name);
            length(errors, sheet, row, "code", code, 30); length(errors, sheet, row, "name", name, 150); length(errors, sheet, row, "location", location, 250);
            if (status != null && !Set.of("OPEN", "CLOSED", "SUSPENDED").contains(status)) add(errors, sheet, row.getRowNum() + 1, "status", "INVALID_STATION_STATUS", "Estado de mesa no permitido.");
            if (ref != null && election != null && code != null && name != null) result.add(new PollingStationImportRow(row.getRowNum() + 1, ref, election, code, name, location, status));
        }
        return result;
    }

    private List<VoterImportRow> readVoters(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_votante_ref", "cedula", "correo_institucional", "nombres", "apellidos", "estado", "voto", "id_eleccion_ref", "id_mesa_ref", "participation_status"), errors);
        List<VoterImportRow> result = new ArrayList<>(); Set<Long> refs = new HashSet<>(); Set<String> ids = new HashSet<>(); Set<String> emails = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) {
            if (empty(row, header)) continue;
            Long ref = longValue(row, header, "id_votante_ref", sheet.getSheetName(), errors, true), election = longValue(row, header, "id_eleccion_ref", sheet.getSheetName(), errors, true), station = longValue(row, header, "id_mesa_ref", sheet.getSheetName(), errors, true);
            String cedula = text(row, header, "cedula"), email = text(row, header, "correo_institucional"), names = text(row, header, "nombres"), surnames = text(row, header, "apellidos"), status = text(row, header, "participation_status");
            Boolean active = bool(row, header, "estado", sheet.getSheetName(), errors), voted = bool(row, header, "voto", sheet.getSheetName(), errors);
            if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_votante_ref", "DUPLICATE_VOTER_REF");
            if (cedula != null && !ids.add(cedula)) duplicate(errors, sheet, row, "cedula", "DUPLICATE_CEDULA");
            if (email != null && !emails.add(email)) duplicate(errors, sheet, row, "correo_institucional", "DUPLICATE_EMAIL");
            required(errors, sheet, row, "cedula", cedula); required(errors, sheet, row, "correo_institucional", email); required(errors, sheet, row, "nombres", names); required(errors, sheet, row, "apellidos", surnames);
            length(errors, sheet, row, "cedula", cedula, 10); length(errors, sheet, row, "correo_institucional", email, 100); length(errors, sheet, row, "nombres", names, 80); length(errors, sheet, row, "apellidos", surnames, 80);
            if (Boolean.TRUE.equals(voted)) add(errors, sheet, row.getRowNum() + 1, "voto", "VOTER_ALREADY_VOTED", "Una carga inicial debe tener voto=false.");
            if (status != null && !"PENDING".equals(status)) add(errors, sheet, row.getRowNum() + 1, "participation_status", "INVALID_INITIAL_STATUS", "Una carga inicial debe tener participation_status=PENDING.");
            if (ref != null && cedula != null && email != null && names != null && surnames != null && election != null && station != null && active != null && voted != null) result.add(new VoterImportRow(row.getRowNum() + 1, ref, cedula, email, names, surnames, active, voted, election, station, status));
        }
        return result;
    }

    private List<PollingMemberImportRow> readMembers(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_miembro_ref", "user_identifier", "full_name", "institutional_email", "id_mesa_ref", "role", "status", "es_votante", "id_votante_ref"), errors);
        List<PollingMemberImportRow> result = new ArrayList<>(); Set<Long> refs = new HashSet<>(); Set<String> users = new HashSet<>(); Set<String> emails = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) {
            if (empty(row, header)) continue;
            Long ref = longValue(row, header, "id_miembro_ref", sheet.getSheetName(), errors, true), station = longValue(row, header, "id_mesa_ref", sheet.getSheetName(), errors, true), voter = longValue(row, header, "id_votante_ref", sheet.getSheetName(), errors, false);
            String user = text(row, header, "user_identifier"), full = text(row, header, "full_name"), email = text(row, header, "institutional_email"), role = text(row, header, "role"), voterFlag = text(row, header, "es_votante");
            Boolean active = bool(row, header, "status", sheet.getSheetName(), errors);
            if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_miembro_ref", "DUPLICATE_MEMBER_REF");
            if (user != null && !users.add(user)) duplicate(errors, sheet, row, "user_identifier", "DUPLICATE_USER_IDENTIFIER");
            if (email != null && !emails.add(email)) duplicate(errors, sheet, row, "institutional_email", "DUPLICATE_MEMBER_EMAIL");
            required(errors, sheet, row, "user_identifier", user); required(errors, sheet, row, "full_name", full); required(errors, sheet, row, "institutional_email", email);
            length(errors, sheet, row, "user_identifier", user, 50); length(errors, sheet, row, "full_name", full, 200); length(errors, sheet, row, "institutional_email", email, 150);
            if (role != null && !Set.of("POLLING_STATION_PRESIDENT", "POLLING_STATION_MEMBER").contains(role)) add(errors, sheet, row.getRowNum() + 1, "role", "INVALID_MEMBER_ROLE", "Rol de miembro no permitido.");
            if (voterFlag != null && !Set.of("SI", "NO").contains(voterFlag)) add(errors, sheet, row.getRowNum() + 1, "es_votante", "INVALID_VOTER_FLAG", "Valores permitidos: SI o NO.");
            if ("SI".equals(voterFlag) && voter == null) add(errors, sheet, row.getRowNum() + 1, "id_votante_ref", "VOTER_REF_REQUIRED", "Es obligatorio para es_votante=SI.");
            if ("NO".equals(voterFlag) && voter != null) add(errors, sheet, row.getRowNum() + 1, "id_votante_ref", "VOTER_REF_FORBIDDEN", "Debe estar vacio para es_votante=NO.");
            if (ref != null && user != null && full != null && email != null && station != null && role != null && active != null && voterFlag != null) result.add(new PollingMemberImportRow(row.getRowNum() + 1, ref, user, full, email, station, role, active, voterFlag, voter));
        }
        return result;
    }

    private List<PositionImportRow> readPositions(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_cargo_ref", "id_eleccion_ref", "nombre"), errors); List<PositionImportRow> result = new ArrayList<>(); Set<Long> refs = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) { if (empty(row, header)) continue; Long ref = longValue(row, header, "id_cargo_ref", sheet.getSheetName(), errors, true), election = longValue(row, header, "id_eleccion_ref", sheet.getSheetName(), errors, true); String name = text(row, header, "nombre"); if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_cargo_ref", "DUPLICATE_POSITION_REF"); required(errors, sheet, row, "nombre", name); length(errors, sheet, row, "nombre", name, 80); if (ref != null && election != null && name != null) result.add(new PositionImportRow(row.getRowNum() + 1, ref, election, name)); }
        return result;
    }

    private List<CandidateImportRow> readCandidates(Sheet sheet, List<ValidationErrorDTO> errors) {
        Header header = header(sheet, List.of("id_candidato_ref", "id_cargo_ref", "nombres", "apellidos", "lista", "estado"), errors); List<CandidateImportRow> result = new ArrayList<>(); Set<Long> refs = new HashSet<>();
        if (!header.valid()) return result;
        for (Row row : dataRows(sheet)) { if (empty(row, header)) continue; Long ref = longValue(row, header, "id_candidato_ref", sheet.getSheetName(), errors, true), position = longValue(row, header, "id_cargo_ref", sheet.getSheetName(), errors, true); String names = text(row, header, "nombres"), surnames = text(row, header, "apellidos"), list = text(row, header, "lista"); Boolean active = bool(row, header, "estado", sheet.getSheetName(), errors); if (ref != null && !refs.add(ref)) duplicate(errors, sheet, row, "id_candidato_ref", "DUPLICATE_CANDIDATE_REF"); required(errors, sheet, row, "nombres", names); required(errors, sheet, row, "apellidos", surnames); length(errors, sheet, row, "nombres", names, 80); length(errors, sheet, row, "apellidos", surnames, 80); length(errors, sheet, row, "lista", list, 50); if (ref != null && position != null && names != null && surnames != null && active != null) result.add(new CandidateImportRow(row.getRowNum() + 1, ref, position, names, surnames, list, active)); }
        return result;
    }

    private Header header(Sheet sheet, List<String> required, List<ValidationErrorDTO> errors) {
        Row row = sheet.getRow(0); Map<String, Integer> columns = new HashMap<>();
        if (row != null) for (Cell cell : row) { String name = text(cell); if (!name.isBlank()) columns.put(name, cell.getColumnIndex()); }
        for (Cell cell : row == null ? List.<Cell>of() : row) {
            String name = text(cell);
            if (name.isBlank()) {
                add(errors, sheet, 1, "", "EMPTY_HEADER", "El encabezado no puede estar vacio.");
            } else if (!required.contains(name)) {
                add(errors, sheet, 1, name, "UNKNOWN_COLUMN", "La columna no pertenece al esquema exacto de la hoja.");
            }
        }
        for (String name : required) if (!columns.containsKey(name)) add(errors, sheet, 1, name, "REQUIRED_COLUMN_MISSING", "Falta la columna obligatoria.");
        return new Header(columns, columns.keySet().containsAll(required));
    }

    private List<Row> dataRows(Sheet sheet) { List<Row> rows = new ArrayList<>(); for (int i = 1; i <= sheet.getLastRowNum(); i++) { Row row = sheet.getRow(i); if (row != null) rows.add(row); } return rows; }
    private boolean empty(Row row, Header header) { for (int column : header.columns.values()) if (!text(row.getCell(column)).isBlank()) return false; return true; }
    private String text(Row row, Header h, String name) { return text(row.getCell(h.columns.getOrDefault(name, -1))); }
    private String text(Cell cell) { return cell == null ? "" : FORMATTER.formatCellValue(cell).trim(); }
    private Long longValue(Row row, Header h, String field, String sheet, List<ValidationErrorDTO> errors, boolean required) { String value = text(row, h, field); if (value.isBlank()) { if (required) add(errors, sheet, row.getRowNum() + 1, field, "REQUIRED_VALUE", "El valor es obligatorio."); return null; } try { long parsed = Long.parseLong(value); if (parsed <= 0) throw new NumberFormatException(); return parsed; } catch (NumberFormatException exception) { add(errors, sheet, row.getRowNum() + 1, field, "INVALID_POSITIVE_NUMBER", "Debe ser un numero entero positivo."); return null; } }
    private Boolean bool(Row row, Header h, String field, String sheet, List<ValidationErrorDTO> errors) { String value = text(row, h, field).toLowerCase(); if (value.equals("true") || value.equals("1") || value.equals("si")) return true; if (value.equals("false") || value.equals("0") || value.equals("no")) return false; add(errors, sheet, row.getRowNum() + 1, field, "INVALID_BOOLEAN", "Use true/false, 1/0 o SI/NO."); return null; }
    private LocalDateTime date(Row row, Header h, String field, String sheet, List<ValidationErrorDTO> errors) { Cell cell = row.getCell(h.columns.getOrDefault(field, -1)); if (cell == null || text(cell).isBlank()) return null; try { if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); return LocalDateTime.parse(text(cell)); } catch (Exception exception) { try { return LocalDate.parse(text(cell)).atStartOfDay(); } catch (Exception ignored) { add(errors, sheet, row.getRowNum() + 1, field, "INVALID_DATE", "Use ISO-8601: yyyy-MM-dd o yyyy-MM-ddTHH:mm:ss."); return null; } } }
    private void required(List<ValidationErrorDTO> errors, Sheet sheet, Row row, String field, Object value) { if (value == null || value.toString().isBlank()) add(errors, sheet, row.getRowNum() + 1, field, "REQUIRED_VALUE", "El valor es obligatorio."); }
    private void length(List<ValidationErrorDTO> errors, Sheet sheet, Row row, String field, String value, int max) { if (value != null && value.length() > max) add(errors, sheet, row.getRowNum() + 1, field, "MAX_LENGTH_EXCEEDED", "Longitud maxima: " + max + "."); }
    private void required(List<ValidationErrorDTO> errors, Sheet sheet, Row row, String field, Object value, boolean ignored) { required(errors, sheet, row, field, value); }
    private void duplicate(List<ValidationErrorDTO> errors, Sheet sheet, Row row, String field, String code) { add(errors, sheet, row.getRowNum() + 1, field, code, "El valor se encuentra repetido en el archivo."); }
    private void add(List<ValidationErrorDTO> errors, Sheet sheet, int row, String field, String code, String message) { errors.add(error(sheet.getSheetName(), row, field, code, message)); }
    private void add(List<ValidationErrorDTO> errors, String sheet, int row, String field, String code, String message) { errors.add(error(sheet, row, field, code, message)); }
    private ValidationErrorDTO error(String sheet, int row, String field, String code, String message) { return new ValidationErrorDTO(sheet, row, field, code, message); }

    public record ReadResult(ElectoralWorkbook workbook, List<ValidationErrorDTO> errors) { }
    private record Header(Map<String, Integer> columns, boolean valid) { }
}
