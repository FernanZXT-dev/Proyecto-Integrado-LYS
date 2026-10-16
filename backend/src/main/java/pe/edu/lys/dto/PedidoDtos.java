package pe.edu.lys.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.edu.lys.entity.EstadoPedido;
import pe.edu.lys.entity.Modalidad;

public final class PedidoDtos {
  private PedidoDtos() {}

  public record PedidoItemRequest(
      @Positive Long productoId,
      @Positive Long comboId,
      @NotNull @Min(1) @Max(50) Integer cantidad,
      @Size(max = 20) List<@NotBlank @Size(max = 120) String> personalizaciones,
      @Size(max = 300) String indicaciones) {}

  public record PedidoRequest(
      @NotNull Modalidad modalidad,
      @NotBlank @Size(max = 100) String nombreContacto,
      @Pattern(regexp = "[0-9]{7,15}") String telefono,
      @Size(max = 20) String mesa,
      @Size(max = 300) String direccion,
      @Size(max = 200) String referencia,
      @NotEmpty @Size(max = 100) List<@NotNull @Valid PedidoItemRequest> items) {}

  public record PedidoItemResponse(Long id, Long productoId, Long comboId, Long promocionId,
      String nombre, String contenidoCombo, int cantidad, BigDecimal precioBase,
      BigDecimal porcentajeDescuento, BigDecimal precioUnitario, BigDecimal total,
      List<String> personalizaciones, String indicaciones) {}

  public record PedidoResponse(Long id, Long clienteId, Modalidad modalidad, EstadoPedido estado,
      String nombreContacto, String telefono, String mesa, String direccion, String referencia,
      BigDecimal subtotal, BigDecimal costoDelivery, BigDecimal total,
      Instant creadoEn, Instant actualizadoEn, List<PedidoItemResponse> items) {}
}
