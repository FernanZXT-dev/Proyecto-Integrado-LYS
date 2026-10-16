package pe.edu.lys.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pedido_items")
@Getter
@Setter
public class PedidoItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pedido_id") private Pedido pedido;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producto_id") private Producto producto;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "combo_id") private Combo combo;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promocion_id") private Promocion promocion;
  // Copias históricas: editar el catálogo no altera pedidos registrados.
  @Column(nullable = false, length = 100) private String nombre;
  @Column(length = 3000) private String contenidoCombo;
  @Column(nullable = false) private int cantidad;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal precioBase;
  @Column(nullable = false, precision = 5, scale = 2) private BigDecimal porcentajeDescuento;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal precioUnitario;
  @Column(nullable = false, precision = 16, scale = 2) private BigDecimal total;
  @Column(length = 300) private String indicaciones;
  @ElementCollection
  @CollectionTable(name = "pedido_item_personalizaciones", joinColumns = @JoinColumn(name = "pedido_item_id"))
  @Column(name = "opcion", nullable = false, length = 120)
  private Set<String> personalizaciones = new LinkedHashSet<>();
}
