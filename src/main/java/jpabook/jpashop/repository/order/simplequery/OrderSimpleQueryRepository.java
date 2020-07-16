package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;

    /*
     *  Dto를 통해 조회는 재사용이 힘듬(API Spec 맞춰짐.. )
     *  장점으로 셀렉 쿼리 날릴때 원하는 컬럼만 선택해서 조회 가능.
     * (생각보다 미비..)
     * @return
     */
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery("select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)"
                + " from Order o"
                + " join o.member m"
                + " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
