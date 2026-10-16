package pe.edu.lys.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promociones")
@Getter
@Setter
public class Promocion {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Version private Long version;
  @Column(nullable = false, length = 100) private String nombre;
  @Column(length = 500) private String descripcion;
  @Column(nullable = false, precision = 5, scale = 2) private BigDecimal porcentajeDescuento;
  @Column(nullable = false) private Instant inicio;
  @Column(nullable = false) private Instant fin;
  @Column(nullable = false) private boolean activo = true;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producto_id") private Producto producto;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "combo_id") private Combo combo;
}
