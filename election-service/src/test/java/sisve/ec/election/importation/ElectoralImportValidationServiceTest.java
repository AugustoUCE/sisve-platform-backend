package sisve.ec.election.importation;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import sisve.ec.election.importation.dto.ValidationResponseDTO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectoralImportValidationServiceTest {
    @Test
    void validaWorkbookCorrecto() throws Exception {
        ValidationResponseDTO response = validate(workbook(false, false, false));

        assertTrue(response.valid());
        assertEquals(1, response.summary().elections());
        assertEquals(1, response.summary().voters());
        assertEquals(1, response.summary().pollingStations());
        assertEquals(1, response.summary().members());
        assertEquals(1, response.summary().positions());
        assertEquals(1, response.summary().candidates());
        assertTrue(response.errors().isEmpty());
    }

    @Test
    void detectaHojaObligatoriaFaltante() throws Exception {
        ValidationResponseDTO response = validate(workbook(true, false, false));

        assertFalse(response.valid());
        assertTrue(response.errors().stream().anyMatch(error -> "REQUIRED_SHEET_MISSING".equals(error.code())));
    }

    @Test
    void detectaDuplicadoYReferenciaCruzada() throws Exception {
        ValidationResponseDTO response = validate(workbook(false, true, true));

        assertFalse(response.valid());
        assertTrue(response.errors().stream().anyMatch(error -> "DUPLICATE_CEDULA".equals(error.code())));
        assertTrue(response.errors().stream().anyMatch(error -> "UNKNOWN_POSITION_REF".equals(error.code())));
    }

    private ValidationResponseDTO validate(XSSFWorkbook workbook) throws IOException {
        try (workbook; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);
            return new ElectoralImportValidationService(new XlsxElectoralReader())
                    .validate(new ByteArrayInputStream(output.toByteArray()));
        }
    }

    private XSSFWorkbook workbook(boolean omitCandidates, boolean invalidReferences, boolean duplicateVoter) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        sheet(workbook, "ELECCION", List.of("id_eleccion_ref", "nombre", "descripcion", "fecha_inicio", "fecha_fin", "estado"),
            List.of("1", "Eleccion 2026", "Prueba", "2026-01-01T00:00:00", "2026-12-31T23:59:59", "planificado"), false);
        sheet(workbook, "MESAS", List.of("id_mesa_ref", "id_eleccion_ref", "code", "name", "location", "status"),
            List.of("10", "1", "MESA-001", "Mesa 1", "Campus", "CLOSED"), false);
        sheet(workbook, "VOTANTES", List.of("id_votante_ref", "cedula", "correo_institucional", "nombres", "apellidos", "estado", "voto", "id_eleccion_ref", "id_mesa_ref", "participation_status"),
            List.of("100", "1712345678", "juan@uce.edu.ec", "Juan", "Perez", "true", "false", "1", "10", "PENDING"), duplicateVoter);
        sheet(workbook, "MIEMBROS_MESA", List.of("id_miembro_ref", "user_identifier", "full_name", "institutional_email", "id_mesa_ref", "role", "status", "es_votante", "id_votante_ref"),
            List.of("1000", "mesa001", "Presidente", "mesa@uce.edu.ec", "10", "POLLING_STATION_PRESIDENT", "true", "SI", "100"), false);
        sheet(workbook, "CARGOS", List.of("id_cargo_ref", "id_eleccion_ref", "nombre"),
            List.of("20", "1", "Presidente"), false);
        if (!omitCandidates) {
            sheet(workbook, "CANDIDATOS", List.of("id_candidato_ref", "id_cargo_ref", "nombres", "apellidos", "lista", "estado"),
                List.of("30", invalidReferences ? "999" : "20", "Ana", "Perez", "Lista A", "true"), false);
        }
        return workbook;
    }

    private void sheet(XSSFWorkbook workbook, String name, List<String> headers, List<String> values, boolean duplicateVoter) {
        var sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
        Row data = sheet.createRow(1);
        for (int index = 0; index < values.size(); index++) data.createCell(index).setCellValue(values.get(index));
        if (duplicateVoter && "VOTANTES".equals(name)) {
            Row duplicate = sheet.createRow(2);
            for (int index = 0; index < values.size(); index++) duplicate.createCell(index).setCellValue(values.get(index));
        }
    }
}
