/**
 * 
 */
package edu.bitspilani.mtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.bitspilani.mtech.model.Sale;

/**
 * @author Mohamed Noohu
 *
 */
public interface SalesRepository extends JpaRepository<Sale, Integer> {

}
