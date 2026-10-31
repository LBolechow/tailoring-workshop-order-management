package pl.lukbol.dyplom.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.lukbol.dyplom.classes.Order;
import pl.lukbol.dyplom.DTOs.order.BookingInterval;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /** Prevents simultaneous edits/deletion of the same order. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    /** An overlap exists when start < existingEnd and end > existingStart. */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM Order o WHERE o.employee.id = :employeeId " +
            "AND o.startDate < :slotEnd AND o.endDate > :slotStart")
    boolean existsOverlappingSlot(@Param("employeeId") Long employeeId,
                                  @Param("slotStart") Date slotStart,
                                  @Param("slotEnd") Date slotEnd);

    /** During an edit, do not consider the order's original slot a collision. */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM Order o WHERE o.employee.id = :employeeId " +
            "AND o.startDate < :slotEnd AND o.endDate > :slotStart " +
            "AND o.id <> :excludedOrderId")
    boolean existsOverlappingSlotExcludingOrder(@Param("employeeId") Long employeeId,
                                                 @Param("slotStart") Date slotStart,
                                                 @Param("slotEnd") Date slotEnd,
                                                 @Param("excludedOrderId") Long excludedOrderId);

    /** One compact query for all employees and all appointments in a bounded horizon.
     *  Interval semantics are half-open: [startDate, endDate).
     */
    @Query("SELECT new pl.lukbol.dyplom.DTOs.order.BookingInterval(" +
            "o.employee.id, o.startDate, o.endDate) " +
            "FROM Order o WHERE o.employee.id IN :employeeIds " +
            "AND o.endDate > :rangeStart AND o.startDate < :rangeEnd")
    List<BookingInterval> findBookedIntervalsForEmployees(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("rangeStart") Date rangeStart,
            @Param("rangeEnd") Date rangeEnd);

    List<Order> findAll();

    List<Order> findByEndDate(Date endDate);

    List<Order> findByStartDateBetweenOrEndDateBetween(Date startDate, Date endDate, Date startDate1, Date endDate1);

    @Query("SELECT o FROM Order o WHERE o.endDate BETWEEN :startDate AND :endDate")
    List<Order> findByEndDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.materials m " +
            "WHERE o.startDate BETWEEN :startDate AND :endDate")
    List<Order> findByStartDateBetweenWithMaterials(
            @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /** Overlap of half-open intervals [start, end), including identical intervals. */
    @Query("SELECT o FROM Order o WHERE o.employee.id = :employeeId " +
            "AND o.endDate > :slotStart AND o.startDate < :slotEnd")
    List<Order> findOverlappingOrdersForEmployee(
            @Param("employeeId") Long employeeId,
            @Param("slotStart") Date slotStart,
            @Param("slotEnd") Date slotEnd);

    @Query("SELECT o FROM Order o WHERE o.clientEmail = :userEmail")
    List<Order> findOrdersByUserEmail(@Param("userEmail") String userEmail);

    @Query("SELECT o FROM Order o WHERE o.employee.id = :employeeId " +
            "AND o.endDate BETWEEN :startDate AND :endDate")
    List<Order> findByEmployeeIdAndEndDateBetween(
            @Param("employeeId") Long employeeId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.materials " +
            "WHERE o.employee.id = :employeeId AND o.startDate BETWEEN :startDate AND :endDate")
    List<Order> findByEmployeeIdAndStartDateBetweenWithMaterials(
            @Param("employeeId") Long employeeId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    boolean existsByEmployee_Id(Long employeeId);

    boolean existsByidCode(String generateActivationCode);

    @EntityGraph(attributePaths = {"employee", "materials"})
    Order findByIdCode(String idCode);


}
