package pe.edu.lys.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Version private Long version;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id") private Cliente cliente;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registrado_por_id") private Usuario registradoPor;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
  private Modalidad modalidad;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
  private EstadoPedido estado = EstadoPedido.RECIBIDO;
  @Column(nullable = false, length = 100) private String nombreContacto;
  @Column(length = 20) private String telefono;
  @Column(length = 20) private String mesa;
  @Column(length = 300) private String direccion;
  @Column(length = 200) private String referencia;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal subtotal;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal costoDelivery;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal total;
  @Column(nullable = false) private Instant creadoEn;
  @Column(nullable = false) private Instant actualizadoEn;
  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<PedidoItem> items = new ArrayList<>();
}
