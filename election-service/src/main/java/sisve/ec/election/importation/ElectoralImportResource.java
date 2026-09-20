package sisve.ec.election.importation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import sisve.ec.election.importation.dto.ValidationResponseDTO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Path("/admin/electoral-import")
@Produces(MediaType.APPLICATION_JSON)
public class ElectoralImportResource {
    private final ElectoralImportValidationService validationService;

    @Inject
    public ElectoralImportResource(ElectoralImportValidationService validationService) {
        this.validationService = validationService;
    }

    @POST
    @Path("/validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response validate(@RestForm("file") FileUpload file) {
        if (file == null || file.fileName() == null || file.fileName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationResponseDTO(false, new sisve.ec.election.importation.dto.ValidationSummaryDTO(0, 0, 0, 0, 0, 0),
                            java.util.List.of(new sisve.ec.election.importation.dto.ValidationErrorDTO("", 1, "file", "FILE_REQUIRED", "El archivo XLSX es obligatorio.")), java.util.List.of()))
                    .build();
        }
        if (!file.fileName().toLowerCase().endsWith(".xlsx")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationResponseDTO(false, new sisve.ec.election.importation.dto.ValidationSummaryDTO(0, 0, 0, 0, 0, 0),
                            java.util.List.of(new sisve.ec.election.importation.dto.ValidationErrorDTO("", 1, "file", "INVALID_EXTENSION", "Solo se aceptan archivos .xlsx.")), java.util.List.of()))
                    .build();
        }
        try {
            if (Files.size(file.uploadedFile()) == 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationResponseDTO(false, new sisve.ec.election.importation.dto.ValidationSummaryDTO(0, 0, 0, 0, 0, 0),
                    java.util.List.of(new sisve.ec.election.importation.dto.ValidationErrorDTO("", 1, "file", "EMPTY_FILE", "El archivo XLSX esta vacio.")), java.util.List.of()))
                .build();
            }
        } catch (IOException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationResponseDTO(false, new sisve.ec.election.importation.dto.ValidationSummaryDTO(0, 0, 0, 0, 0, 0),
                    java.util.List.of(new sisve.ec.election.importation.dto.ValidationErrorDTO("", 1, "file", "FILE_READ_ERROR", "No se pudo inspeccionar el archivo XLSX.")), java.util.List.of()))
                .build();
        }
        try (InputStream input = file.uploadedFile().toFile().toURI().toURL().openStream()) {
            ValidationResponseDTO result = validationService.validate(input);
            return Response.ok(result).build();
        } catch (IOException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationResponseDTO(false, new sisve.ec.election.importation.dto.ValidationSummaryDTO(0, 0, 0, 0, 0, 0),
                            java.util.List.of(new sisve.ec.election.importation.dto.ValidationErrorDTO("", 1, "file", "FILE_READ_ERROR", "No se pudo leer el archivo XLSX.")), java.util.List.of()))
                    .build();
        }
    }
}
