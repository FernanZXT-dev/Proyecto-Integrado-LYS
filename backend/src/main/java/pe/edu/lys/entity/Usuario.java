package pe.edu.lys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nombre;

  @Column(nullable = false, unique = true)
  private String correo;

  @Column(nullable = false)
  private String clave;

  private boolean activo = true;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "rol_id")
  private Rol rol;
}
