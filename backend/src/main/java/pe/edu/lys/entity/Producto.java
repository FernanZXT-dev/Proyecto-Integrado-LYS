package pe.edu.lys.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter
@Setter
public class Producto {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String nombre;

  @Column(length = 500)
  private String descripcion;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precio;

  private boolean activo = true;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_id")
  private Categoria categoria;

}
