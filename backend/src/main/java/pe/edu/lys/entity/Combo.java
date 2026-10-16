package pe.edu.lys.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "combos")
@Getter
@Setter
public class Combo {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Version private Long version;
  @Column(nullable = false, length = 100) private String nombre;
  @Column(length = 500) private String descripcion;
  @Column(nullable = false, precision = 10, scale = 2) private BigDecimal precio;
  @Column(nullable = false) private boolean activo = true;
  @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<ComboItem> items = new ArrayList<>();
}
