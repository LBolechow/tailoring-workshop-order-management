package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import pl.lukbol.dyplom.common.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;


    private String clientName;

    private String clientEmail;

    private String phoneNumber;

    /** Stable identity: employee names are display data and can change. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private User employee;

    /** Keep the existing REST DTO field without storing a duplicate name. */
    @Transient
    public String getEmployeeName() {
        return employee == null ? null : employee.getName();
    }

    private Date startDate;

    private Date endDate;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status")
    private OrderStatus orderStatus;

    /** String facade retained for the REST API and existing callers. */
    @Transient
    public String getStatus() {
        return orderStatus == null ? null : orderStatus.label();
    }

    public void setStatus(String status) {
        this.orderStatus = OrderStatus.fromLabel(status);
    }

    private int price;

    /** Numeric persistence retains the old column and API for existing data. */
    private double duration;

    /** Strongly typed duration for domain calculations, never for storage. */
    @Transient
    public java.time.Duration getDurationValue() {
        return java.time.Duration.ofMinutes(Math.round(duration * 60));
    }

    private String idCode;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Material> materials;

    public Order(String description, String clientName, String clientEmail, String phoneNumber, User employee, Date startDate, Date endDate, String status, int price, double duration, List<Material> materials, String idCode) {
        this.description = description;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.phoneNumber = phoneNumber;
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        setStatus(status);
        this.price = price;
        this.duration = duration;
        this.materials = materials;
        this.idCode = idCode;
    }

}
