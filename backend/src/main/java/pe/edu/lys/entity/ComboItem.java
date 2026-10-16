package pe.edu.lys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "combo_items", uniqueConstraints = @UniqueConstraint(columnNames = {"combo_id", "producto_id"}))
@Getter
@Setter
public class ComboItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "combo_id") private Combo combo;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "producto_id") private Producto producto;
  @Column(nullable = false) private int cantidad;
}
