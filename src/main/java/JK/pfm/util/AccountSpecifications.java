/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package JK.pfm.util;

import JK.pfm.model.Account;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


public class AccountSpecifications {

    public static Specification<Account> belongsToUser(Long userId) {
        return (Root<Account> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
}